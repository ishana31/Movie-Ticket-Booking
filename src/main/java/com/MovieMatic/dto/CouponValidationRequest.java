package com.MovieMatic.dto;

/**
 * Data Transfer Object for validating promotional coupons during ticket checkout.
 *
 * @author CodeFactoryAgent
 */
public class CouponValidationRequest {

    /**
     * The promotional coupon code submitted by the user.
     */
    private String code;

    /**
     * The gross order subtotal amount prior to promotional discount.
     */
    private double totalAmount;

    public CouponValidationRequest() {
    }

    public CouponValidationRequest(String code, double totalAmount) {
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
