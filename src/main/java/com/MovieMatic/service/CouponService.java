package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public CouponValidationResult validateCoupon(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (couponOpt.isEmpty()) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        Coupon coupon = couponOpt.get();

        // Check expiration date
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        // Check minimum booking amount
        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            throw new CouponValidationException(
                String.format("Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount())
            );
        }

        double discountAmount = calculateDiscount(coupon, totalAmount);
        double finalAmount = Math.max(0.0, totalAmount - discountAmount);
        finalAmount = Math.round(finalAmount * 100.0) / 100.0;

        return new CouponValidationResult(coupon, discountAmount, finalAmount);
    }

    public double calculateDiscount(Coupon coupon, double totalAmount) {
        if (coupon == null || totalAmount <= 0) {
            return 0.0;
        }

        double discount = 0.0;
        String type = coupon.getDiscountType() != null ? coupon.getDiscountType().trim().toUpperCase() : "";

        if ("PERCENTAGE".equals(type)) {
            discount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else if ("FLAT".equals(type)) {
            discount = coupon.getDiscountValue();
        }

        // Cap discount at totalAmount so discount cannot exceed total
        discount = Math.min(discount, totalAmount);
        return Math.round(discount * 100.0) / 100.0;
    }

    public void recordCouponUsage(String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public Optional<Coupon> findByCode(String code) {
        if (code == null) return Optional.empty();
        return couponRepository.findByCodeIgnoreCase(code.trim());
    }
}
