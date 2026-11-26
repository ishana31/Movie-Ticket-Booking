package com.MovieMatic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object representing the result of a coupon validation calculation.
 *
 * @author CodeFactoryAgent
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponValidationResponse {

    private boolean valid;
    private double discountAmount;
    private double finalAmount;
    private String message;

    public CouponValidationResponse() {
    }

    public CouponValidationResponse(boolean valid, double discountAmount, double finalAmount) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    public CouponValidationResponse(boolean valid, double discountAmount, double finalAmount, String message) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.message = message;
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
}
