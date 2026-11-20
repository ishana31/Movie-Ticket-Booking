package com.MovieMatic.dto;

public class CouponValidateRequest {

    private String code;
    private double totalAmount;

    public CouponValidateRequest() {
    }

    public CouponValidateRequest(String code, double totalAmount) {
        this.code = code;
        this.totalAmount = totalAmount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
