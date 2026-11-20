package com.MovieMatic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponValidateResponse {

    private boolean valid;
    private Double discountAmount;
    private Double finalAmount;
    private String message;
    private String couponCode;

    public CouponValidateResponse() {
    }

    public static CouponValidateResponse success(String couponCode, double discountAmount, double finalAmount) {
        CouponValidateResponse response = new CouponValidateResponse();
        response.setValid(true);
        response.setCouponCode(couponCode);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(finalAmount);
        return response;
    }

    public static CouponValidateResponse error(String message) {
        CouponValidateResponse response = new CouponValidateResponse();
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

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
