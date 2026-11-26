package com.MovieMatic.dto;

/**
 * Request payload for validating promotional coupons against a cart or booking total amount.
 *
 * @author CodeFactoryAgent
 */
public class CouponValidationRequest {

    /**
     * Case-insensitive promotional coupon code (e.g. 'SAVE20').
     */
    private String code;

    /**
     * Total pre-discount booking amount in rupees.
     */
    private double totalAmount;

    /**
     * Default constructor for Jackson deserialization.
     */
    public CouponValidationRequest() {
    }

    /**
     * Parameterized constructor for coupon validation requests.
     *
     * @param code        coupon code
     * @param totalAmount booking total amount
     */
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
