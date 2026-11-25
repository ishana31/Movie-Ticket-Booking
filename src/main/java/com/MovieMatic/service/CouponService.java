package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public static class CouponValidationResult {
        private final boolean valid;
        private final double discountAmount;
        private final double finalAmount;
        private final String message;
        private final Coupon coupon;

        private CouponValidationResult(boolean valid, double discountAmount, double finalAmount, String message, Coupon coupon) {
            this.valid = valid;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
            this.message = message;
            this.coupon = coupon;
        }

        public static CouponValidationResult success(Coupon coupon, double discountAmount, double finalAmount) {
            return new CouponValidationResult(true, discountAmount, finalAmount, null, coupon);
        }

        public static CouponValidationResult failure(String message) {
            return new CouponValidationResult(false, 0.0, 0.0, message, null);
        }

        public boolean isValid() {
            return valid;
        }

        public double getDiscountAmount() {
            return discountAmount;
        }

        public double getFinalAmount() {
            return finalAmount;
        }

        public String getMessage() {
            return message;
        }

        public Coupon getCoupon() {
            return coupon;
        }
    }

    public CouponValidationResult validateCoupon(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResult.failure("Invalid or expired coupon code");
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (couponOpt.isEmpty()) {
            return CouponValidationResult.failure("Invalid or expired coupon code");
        }

        Coupon coupon = couponOpt.get();

        // Expiration check
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            return CouponValidationResult.failure("Invalid or expired coupon code");
        }

        // Usage limit check
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidationResult.failure("Invalid or expired coupon code");
        }

        // Minimum order amount check
        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            String msg = String.format(Locale.US, "Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount());
            return CouponValidationResult.failure(msg);
        }

        double discountAmount = calculateDiscount(coupon, totalAmount);
        double finalAmount = roundToTwoDecimals(Math.max(0.0, totalAmount - discountAmount));

        return CouponValidationResult.success(coupon, discountAmount, finalAmount);
    }

    public double calculateDiscount(Coupon coupon, double totalAmount) {
        if (coupon == null || totalAmount <= 0) {
            return 0.0;
        }

        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = totalAmount * (coupon.getDiscountValue() / 100.0);
        } else if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else {
            discount = 0.0;
        }

        // Discount cannot exceed totalAmount and cannot be negative
        discount = Math.min(discount, totalAmount);
        discount = Math.max(0.0, discount);

        return roundToTwoDecimals(discount);
    }

    public double calculateFinalAmount(Coupon coupon, double totalAmount) {
        double discount = calculateDiscount(coupon, totalAmount);
        return roundToTwoDecimals(Math.max(0.0, totalAmount - discount));
    }

    public void incrementUsedCount(Coupon coupon) {
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    public Optional<Coupon> findActiveCouponByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
    }

    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
