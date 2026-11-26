package com.MovieMatic.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Domain entity representing a promotional coupon code and discount rule in MovieMatic.
 * <p>
 * Controls discount eligibility, percentage or flat discount calculations, minimum order thresholds,
 * expiration dates, and global usage limits.
 * </p>
 *
 * @author CodeFactoryAgent
 */
@Entity
@Table(name = "coupons")
public class Coupon {

    /**
     * Unique identifier for the coupon entity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique promotional discount code entered by users during checkout (e.g., 'SAVE20').
     * Evaluated case-insensitively.
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * Discount strategy type: PERCENTAGE or FLAT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    /**
     * Magnitude of the discount (e.g., 20.0 for 20% or 50.0 for ₹50).
     */
    @Column(name = "discount_value", nullable = false)
    private double discountValue;

    /**
     * Minimum booking subtotal required to qualify for this coupon (nullable if no minimum).
     */
    @Column(name = "min_order_amount")
    private Double minOrderAmount;

    /**
     * Timestamp deadline after which the coupon is considered expired and invalid.
     */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /**
     * Maximum number of times this promotional coupon may be redeemed across all bookings.
     */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /**
     * Current total redemption count for this coupon.
     */
    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    /**
     * Administrative flag indicating whether this coupon is currently active.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Default no-arg constructor required by JPA.
     */
    public Coupon() {
    }

    /**
     * Convenience constructor initializing all mandatory promotional attributes.
     *
     * @param code           the unique promotional code string
     * @param discountType   the discount calculation strategy (PERCENTAGE or FLAT)
     * @param discountValue  the numeric discount rate or flat rupee value
     * @param minOrderAmount the minimum order subtotal spend requirement
     * @param validUntil     the coupon expiration date/time
     * @param usageLimit     the global maximum redemption limit
     * @param isActive       whether the coupon is active
     */
    public Coupon(String code, DiscountType discountType, double discountValue,
                  Double minOrderAmount, LocalDateTime validUntil, Integer usageLimit, boolean isActive) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.validUntil = validUntil;
        this.usageLimit = usageLimit;
        this.isActive = isActive;
        this.usedCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public Double getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(Double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
