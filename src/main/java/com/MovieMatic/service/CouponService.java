package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public CouponValidationResult validateCoupon(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResult.invalid("Invalid or expired coupon code");
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (couponOpt.isEmpty()) {
            return CouponValidationResult.invalid("Invalid or expired coupon code");
        }

        Coupon coupon = couponOpt.get();

        // Check expiration
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            return CouponValidationResult.invalid("Invalid or expired coupon code");
        }

        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0 
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidationResult.invalid("Invalid or expired coupon code");
        }

        // Check minimum order amount
        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            String msg = String.format(Locale.US, "Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount());
            return CouponValidationResult.invalid(msg);
        }

        // Calculate discount
        double discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else {
            return CouponValidationResult.invalid("Invalid discount type");
        }

        if (discount > totalAmount) {
            discount = totalAmount;
        }

        double discountAmount = Math.round(discount * 100.0) / 100.0;
        double finalAmount = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;

        return CouponValidationResult.valid(coupon, discountAmount, finalAmount);
    }

    @Transactional
    public void incrementUsedCount(Coupon coupon) {
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public Optional<Coupon> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return couponRepository.findByCodeIgnoreCase(code.trim());
    }
}
