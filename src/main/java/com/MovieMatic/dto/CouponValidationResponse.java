package com.MovieMatic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponValidationResponse {

    private boolean valid;
    private Double discountAmount;
    private Double finalAmount;
    private String message;

    public CouponValidationResponse() {
    }

    public static CouponValidationResponse valid(double discountAmount, double finalAmount) {
        CouponValidationResponse res = new CouponValidationResponse();
        res.setValid(true);
        res.setDiscountAmount(discountAmount);
        res.setFinalAmount(finalAmount);
        return res;
    }

    public static CouponValidationResponse invalid(String message) {
        CouponValidationResponse res = new CouponValidationResponse();
        res.setValid(false);
        res.setMessage(message);
        return res;
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
}
