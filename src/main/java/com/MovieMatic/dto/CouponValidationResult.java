package com.MovieMatic.dto;

import com.MovieMatic.model.Coupon;

public class CouponValidationResult {

    private boolean valid;
    private double discountAmount;
    private double finalAmount;
    private String message;
    private Coupon coupon;

    public CouponValidationResult() {
    }

    public static CouponValidationResult valid(Coupon coupon, double discountAmount, double finalAmount) {
        CouponValidationResult result = new CouponValidationResult();
        result.setValid(true);
        result.setCoupon(coupon);
        result.setDiscountAmount(discountAmount);
        result.setFinalAmount(finalAmount);
        return result;
    }

    public static CouponValidationResult invalid(String message) {
        CouponValidationResult result = new CouponValidationResult();
        result.setValid(false);
        result.setMessage(message);
        return result;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }
}
