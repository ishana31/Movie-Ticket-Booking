package com.MovieMatic.model;

/**
 * Enumeration representing the supported discount calculation types for promotional coupons
 * within the MovieMatic application.
 *
 * @author CodeFactoryAgent
 */
public enum DiscountType {
    /**
     * Percentage-based discount calculated proportionally from the total booking amount.
     */
    PERCENTAGE,

    /**
     * Flat-rate discount deducted directly from the total booking amount.
     */
    FLAT
}
