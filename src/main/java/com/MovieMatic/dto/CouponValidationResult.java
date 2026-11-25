package com.MovieMatic.dto;

import com.MovieMatic.model.Coupon;

public class CouponValidationResult {
    private Coupon coupon;
    private double discountAmount;
    private double finalAmount;

    public CouponValidationResult(Coupon coupon, double discountAmount, double finalAmount) {
        this.coupon = coupon;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }
}
