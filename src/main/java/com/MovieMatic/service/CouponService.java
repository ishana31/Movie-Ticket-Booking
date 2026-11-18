package com.MovieMatic.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@Service
public class CouponService {

    public static final String INVALID_OR_EXPIRED_MESSAGE = "Invalid or expired coupon code";

    @Autowired
    private CouponRepository couponRepository;

    public CouponValidationResponse validateAndCalculate(String code, double totalAmount) {
        if (code == null || code.trim().isEmpty()) {
            return CouponValidationResponse.error(INVALID_OR_EXPIRED_MESSAGE);
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
        if (couponOpt.isEmpty()) {
            return CouponValidationResponse.error(INVALID_OR_EXPIRED_MESSAGE);
        }

        Coupon coupon = couponOpt.get();

        if (!coupon.isActive()) {
            return CouponValidationResponse.error(INVALID_OR_EXPIRED_MESSAGE);
        }

        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            return CouponValidationResponse.error(INVALID_OR_EXPIRED_MESSAGE);
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.error(INVALID_OR_EXPIRED_MESSAGE);
        }

        if (coupon.getMinOrderAmount() > 0 && totalAmount < coupon.getMinOrderAmount()) {
            String message = String.format(Locale.US, "Minimum booking amount of ₹%.2f required.", coupon.getMinOrderAmount());
            return CouponValidationResponse.error(message);
        }

        double discountAmount = calculateDiscount(coupon, totalAmount);
        double finalAmount = calculateFinalAmount(totalAmount, discountAmount);
        String successMessage = String.format(Locale.US, "Coupon '%s' applied! You saved ₹%.2f", coupon.getCode(), discountAmount);

        return CouponValidationResponse.success(discountAmount, finalAmount, successMessage, coupon);
    }

    public double calculateDiscount(Coupon coupon, double totalAmount) {
        if (coupon == null || totalAmount <= 0) {
            return 0.0;
        }

        double rawDiscount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            rawDiscount = (totalAmount * coupon.getDiscountValue()) / 100.0;
        } else {
            rawDiscount = coupon.getDiscountValue();
        }

        double cappedDiscount = Math.min(rawDiscount, totalAmount);
        return roundToTwoDecimals(Math.max(0.0, cappedDiscount));
    }

    public double calculateFinalAmount(double totalAmount, double discountAmount) {
        double finalAmount = Math.max(0.0, totalAmount - discountAmount);
        return roundToTwoDecimals(finalAmount);
    }

    @Transactional
    public void incrementUsedCount(Coupon coupon) {
        if (coupon != null && coupon.getId() != null) {
            couponRepository.findById(coupon.getId()).ifPresent(c -> {
                c.setUsedCount(c.getUsedCount() + 1);
                couponRepository.save(c);
            });
        }
    }

    public Optional<Coupon> findActiveByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return couponRepository.findByCodeIgnoreCaseAndIsActiveTrue(code.trim());
    }

    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    public static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
