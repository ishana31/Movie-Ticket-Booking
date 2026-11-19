package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    public Coupon validateCoupon(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim())
                .orElseThrow(() -> new CouponValidationException("Invalid or expired coupon code"));

        if (coupon.getValidUntil() != null && LocalDateTime.now().isAfter(coupon.getValidUntil())) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() > 0 && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponValidationException("Invalid or expired coupon code");
        }

        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            throw new CouponValidationException(String.format(Locale.US, "Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount()));
        }

        return coupon;
    }

    public double calculateDiscount(Coupon coupon, double totalAmount) {
        if (coupon == null || totalAmount <= 0) {
            return 0.0;
        }

        double discount = 0.0;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        }

        if (discount > totalAmount) {
            discount = totalAmount;
        }

        return Math.round(discount * 100.0) / 100.0;
    }

    public CouponValidationResponse validateAndCalculate(String code, double totalAmount) {
        Coupon coupon = validateCoupon(code, totalAmount);
        double discountAmount = calculateDiscount(coupon, totalAmount);
        double finalAmount = Math.round((totalAmount - discountAmount) * 100.0) / 100.0;
        return new CouponValidationResponse(true, discountAmount, finalAmount);
    }

    @Transactional
    public void recordCouponUsage(Coupon coupon) {
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }
}
