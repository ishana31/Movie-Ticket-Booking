package com.MovieMatic.model;

/**
 * Enumeration representing the supported discount calculation strategies for promotional coupons
 * within the MovieMatic cinema ticket booking platform.
 *
 * @author CodeFactoryAgent
 */
public enum DiscountType {

    /**
     * Percentage-based discount deducted as a proportion of the gross booking order amount
     * (e.g., 20% off).
     */
    PERCENTAGE,

    /**
     * Fixed flat currency amount deducted from the gross booking order amount
     * (e.g., ₹50.00 off).
     */
    FLAT
}
