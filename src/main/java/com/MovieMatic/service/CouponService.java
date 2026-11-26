package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

/**
 * Service managing promotional coupon validation, discount computation, and usage tracking.
 * Ensures robust server-side recalculation of discounts to protect against client-side tampering,
 * enforces usage limits, minimum order values, and expiration rules.
 *
 * @author CodeFactoryAgent
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;

    @Autowired
    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /**
     * Validates a promotional coupon code against an order total and computes the discount amount.
     *
     * @param rawCode     coupon code submitted by the user
     * @param totalAmount booking order total before discount
     * @return validation response containing calculated discount and final amount
     * @throws CouponException if code is invalid, expired, exhausted, or below minimum spend
     */
    public CouponValidationResponse validateCoupon(String rawCode, double totalAmount) {
        log.info("Validating coupon code '{}' for order total ₹{}", rawCode, totalAmount);

        if (rawCode == null || rawCode.trim().isEmpty()) {
            log.warn("Coupon code is missing or empty");
            throw new CouponException("Invalid or expired coupon code");
        }

        String code = rawCode.trim();
        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code);

        if (couponOpt.isEmpty()) {
            log.warn("Coupon code '{}' not found or is marked inactive", code);
            throw new CouponException("Invalid or expired coupon code");
        }

        Coupon coupon = couponOpt.get();

        // Validate expiration
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            log.warn("Coupon '{}' expired on {}", coupon.getCode(), coupon.getValidUntil());
            throw new CouponException("Invalid or expired coupon code");
        }

        // Validate usage limits
        if (coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            log.warn("Coupon '{}' exceeded usage limit: {} / {}",
                    coupon.getCode(), coupon.getUsedCount(), coupon.getUsageLimit());
            throw new CouponException("Invalid or expired coupon code");
        }

        // Validate minimum spend threshold
        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            log.warn("Booking amount ₹{} is below minimum threshold ₹{} for coupon '{}'",
                    totalAmount, coupon.getMinOrderAmount(), coupon.getCode());
            throw new CouponException(String.format(Locale.US,
                    "Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount()));
        }

        // Calculate discount savings
        double discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else if (coupon.getDiscountType() == DiscountType.FLAT) {
            discountAmount = coupon.getDiscountValue();
        } else {
            discountAmount = 0.0;
        }

        // Guard against discount exceeding total amount
        if (discountAmount > totalAmount) {
            discountAmount = totalAmount;
        }

        // Round currency amounts to 2 decimal places
        discountAmount = Math.round(discountAmount * 100.0) / 100.0;
        double finalAmount = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;

        String formattedMessage = String.format(Locale.US,
                "Coupon '%s' applied! You saved ₹%.2f", coupon.getCode(), discountAmount);
        log.info("Successfully validated coupon '{}'. Discount: ₹{}, Final Total: ₹{}",
                coupon.getCode(), discountAmount, finalAmount);

        return new CouponValidationResponse(true, discountAmount, finalAmount, formattedMessage);
    }

    /**
     * Records the usage of a promotional coupon upon successful booking creation by incrementing
     * the coupon's used_count in the database.
     *
     * @param rawCode applied coupon code
     */
    @Transactional
    public void recordCouponUsage(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return;
        }
        String code = rawCode.trim();
        couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code).ifPresent(coupon -> {
            int newCount = coupon.getUsedCount() + 1;
            coupon.setUsedCount(newCount);
            couponRepository.save(coupon);
            log.info("Recorded redemption for coupon '{}'. Updated used_count={}", coupon.getCode(), newCount);
        });
    }

    /**
     * Retrieves an active coupon by promotional code.
     *
     * @param code promotional code
     * @return Optional containing the Coupon if active
     */
    public Optional<Coupon> findActiveByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
    }

    /**
     * Persists or updates a coupon record in the database.
     *
     * @param coupon coupon entity to save
     * @return saved coupon entity
     */
    @Transactional
    public Coupon saveCoupon(Coupon coupon) {
        log.info("Saving coupon '{}' with discount type {}", coupon.getCode(), coupon.getDiscountType());
        return couponRepository.save(coupon);
    }

    /**
     * Retrieves all coupon records.
     *
     * @return list of all coupons
     */
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }
}
