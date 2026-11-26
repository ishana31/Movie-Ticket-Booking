package com.MovieMatic.exception;

/**
 * Domain exception thrown when coupon validation fails due to invalid code,
 * expiration, exhaustion of usage limits, or failure to meet the minimum spend threshold.
 *
 * @author CodeFactoryAgent
 */
public class CouponException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new CouponException with the specified business detail message.
     *
     * @param message descriptive failure reason
     */
    public CouponException(String message) {
        super(message);
    }
}
