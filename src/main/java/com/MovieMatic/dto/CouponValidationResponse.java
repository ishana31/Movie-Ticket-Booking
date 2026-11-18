package com.MovieMatic.dto;

import com.MovieMatic.model.Coupon;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponValidationResponse {

    private boolean valid;
    private Double discountAmount;
    private Double finalAmount;
    private String message;

    @JsonIgnore
    private Coupon coupon;

    public CouponValidationResponse() {
    }

    public static CouponValidationResponse success(double discountAmount, double finalAmount, String message, Coupon coupon) {
        CouponValidationResponse response = new CouponValidationResponse();
        response.setValid(true);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(finalAmount);
        response.setMessage(message);
        response.setCoupon(coupon);
        return response;
    }

    public static CouponValidationResponse error(String message) {
        CouponValidationResponse response = new CouponValidationResponse();
        response.setValid(false);
        response.setMessage(message);
        return response;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
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
