package com.MovieMatic.dto;

public class CouponValidationResponse {
    private boolean valid;
    private double discountAmount;
    private double finalAmount;

    public CouponValidationResponse() {
    }

    public CouponValidationResponse(boolean valid, double discountAmount, double finalAmount) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
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
}
