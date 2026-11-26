package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

/**
 * Service managing promotional coupon validation, discount computation, and redemption lifecycle.
 * <p>
 * Ensures server-side integrity of all pricing adjustments, preventing client tampering and enforcing
 * expiration dates, minimum spend thresholds, and maximum redemption limits.
 * </p>
 *
 * @author CodeFactoryAgent
 */
@Service
public class CouponService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;

    @Autowired
    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Seeds initial promotional coupons into the database if not already present.
     *
     * @param args incoming command-line execution arguments
     */
    @Override
    public void run(String... args) {
        initializeDefaultCoupons();
    }

    /**
     * Seeds canonical coupons like 'SAVE20' for immediate demonstration and production readiness.
     */
    @Transactional
    public void initializeDefaultCoupons() {
        if (couponRepository.findByCodeIgnoreCase("SAVE20").isEmpty()) {
            Coupon save20 = new Coupon(
                    "SAVE20",
                    DiscountType.PERCENTAGE,
                    20.0,
                    300.0,
                    LocalDateTime.now().plusYears(2),
                    1000,
                    true
            );
            couponRepository.save(save20);
            log.info("Initialized default promotional coupon: SAVE20 (20% off, min order ₹300.00)");
        }

        if (couponRepository.findByCodeIgnoreCase("FLAT50").isEmpty()) {
            Coupon flat50 = new Coupon(
                    "FLAT50",
                    DiscountType.FLAT,
                    50.0,
                    200.0,
                    LocalDateTime.now().plusYears(2),
                    500,
                    true
            );
            couponRepository.save(flat50);
            log.info("Initialized default promotional coupon: FLAT50 (₹50 off, min order ₹200.00)");
        }
    }

    /**
     * Validates a coupon code against a booking subtotal and computes the resulting discount without
     * incrementing the redemption count (preview validation for UI checkout).
     *
     * @param code        the user-submitted promotional code
     * @param totalAmount the gross booking subtotal amount prior to discount
     * @return a {@link CouponValidationResponse} detailing validity, discount amount, and final amount
     * @throws CouponException if the coupon is not found, inactive, expired, limit exceeded, or spend below threshold
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, double totalAmount) {
        log.info("Validating coupon code '{}' for booking order amount: ₹{}", code, totalAmount);

        Coupon coupon = getValidCouponOrThrow(code, totalAmount);
        double discountAmount = calculateDiscountAmount(coupon, totalAmount);
        double finalAmount = Math.max(0.0, roundCurrency(totalAmount - discountAmount));

        String successMessage = String.format(Locale.US, "Coupon '%s' applied! You saved ₹%.2f",
                coupon.getCode(), discountAmount);

        log.info("Coupon '{}' successfully validated: discount=₹{}, finalAmount=₹{}",
                coupon.getCode(), discountAmount, finalAmount);

        return new CouponValidationResponse(true, discountAmount, finalAmount, successMessage);
    }

    /**
     * Validates and applies a coupon during confirmed booking creation, incrementing its usage count.
     *
     * @param code        the promotional coupon code
     * @param totalAmount the gross booking subtotal amount
     * @return a {@link CouponRedemptionResult} containing the validated coupon, discount amount, and final payable total
     * @throws CouponException if validation conditions fail
     */
    @Transactional
    public CouponRedemptionResult applyCouponForBooking(String code, double totalAmount) {
        log.info("Applying and redeeming coupon '{}' for confirmed booking amount: ₹{}", code, totalAmount);

        Coupon coupon = getValidCouponOrThrow(code, totalAmount);
        double discountAmount = calculateDiscountAmount(coupon, totalAmount);
        double finalAmount = Math.max(0.0, roundCurrency(totalAmount - discountAmount));

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        log.info("Successfully redeemed coupon '{}'. Updated usage count: {}",
                coupon.getCode(), coupon.getUsedCount());

        return new CouponRedemptionResult(coupon, discountAmount, finalAmount);
    }

    /**
     * Locates an active coupon by code (case-insensitively) and validates expiration, usage limit,
     * and minimum order amount.
     *
     * @param code        the promotional code
     * @param totalAmount the gross order subtotal
     * @return the valid {@link Coupon} entity
     * @throws CouponException if validation rules are violated
     */
    public Coupon getValidCouponOrThrow(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Coupon validation rejected: code is empty or null");
            throw new CouponException("Invalid or expired coupon code");
        }

        String normalizedCode = code.trim();
        Optional<Coupon> couponOptional = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(normalizedCode);

        if (couponOptional.isEmpty()) {
            log.warn("Coupon validation rejected: code '{}' not found or inactive", normalizedCode);
            throw new CouponException("Invalid or expired coupon code");
        }

        Coupon coupon = couponOptional.get();

        // 1. Expiration Check
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            log.warn("Coupon validation rejected: code '{}' expired on {}", coupon.getCode(), coupon.getValidUntil());
            throw new CouponException("Invalid or expired coupon code");
        }

        // 2. Global Usage Limit Check
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            log.warn("Coupon validation rejected: code '{}' reached usage limit ({}/{})",
                    coupon.getCode(), coupon.getUsedCount(), coupon.getUsageLimit());
            throw new CouponException("Invalid or expired coupon code");
        }

        // 3. Minimum Order Amount Check
        if (coupon.getMinOrderAmount() != null && totalAmount < coupon.getMinOrderAmount()) {
            log.warn("Coupon validation rejected: code '{}' requires minimum ₹{}, but order is ₹{}",
                    coupon.getCode(), coupon.getMinOrderAmount(), totalAmount);
            throw new CouponException(String.format(Locale.US, "Minimum booking amount of ₹%.2f required.",
                    coupon.getMinOrderAmount()));
        }

        return coupon;
    }

    /**
     * Calculates the raw discount amount based on the coupon discount type and value, capped at totalAmount.
     *
     * @param coupon      the promotional coupon
     * @param totalAmount the order gross subtotal
     * @return the calculated discount amount in rupees
     */
    public double calculateDiscountAmount(Coupon coupon, double totalAmount) {
        if (totalAmount <= 0.0) {
            return 0.0;
        }

        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else {
            discount = Math.min(coupon.getDiscountValue(), totalAmount);
        }

        discount = Math.min(discount, totalAmount);
        discount = Math.max(0.0, discount);
        return roundCurrency(discount);
    }

    /**
     * Rounds a double currency figure to two decimal places.
     *
     * @param value currency value to round
     * @return rounded value
     */
    private double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Value record holding results of a successfully redeemed promotional coupon.
     */
    public static class CouponRedemptionResult {
        private final Coupon coupon;
        private final double discountAmount;
        private final double finalAmount;

        public CouponRedemptionResult(Coupon coupon, double discountAmount, double finalAmount) {
            this.coupon = coupon;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }

        public Coupon getCoupon() {
            return coupon;
        }

        public double getDiscountAmount() {
            return discountAmount;
        }

        public double getFinalAmount() {
            return finalAmount;
        }
    }
}
