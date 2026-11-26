package com.MovieMatic.dto;

/**
 * Response payload returned upon successful validation of a promotional coupon code.
 * Contains confirmation of validity, computed discount amount, and resulting final payable total.
 *
 * @author CodeFactoryAgent
 */
public class CouponValidationResponse {

    /**
     * Whether the coupon is valid and eligible for application.
     */
    private boolean valid;

    /**
     * Calculated discount savings in rupees.
     */
    private double discountAmount;

    /**
     * Final payable amount after deducting the discount from the pre-discount total.
     */
    private double finalAmount;

    /**
     * Optional informative message describing the applied discount.
     */
    private String message;

    /**
     * Default constructor for serialization frameworks.
     */
    public CouponValidationResponse() {
    }

    /**
     * Convenience constructor matching endpoint response specification.
     *
     * @param valid          validation flag
     * @param discountAmount calculated discount amount
     * @param finalAmount    final post-discount payable total
     */
    public CouponValidationResponse(boolean valid, double discountAmount, double finalAmount) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }

    /**
     * Complete constructor including status message.
     *
     * @param valid          validation flag
     * @param discountAmount calculated discount amount
     * @param finalAmount    final post-discount payable total
     * @param message        descriptive message
     */
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
