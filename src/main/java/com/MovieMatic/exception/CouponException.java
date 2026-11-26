package com.MovieMatic.exception;

/**
 * Custom runtime exception indicating coupon validation failures, such as invalid code,
 * expired promotional dates, exceeded usage limits, or unmet minimum order spend amounts.
 *
 * @author CodeFactoryAgent
 */
public class CouponException extends RuntimeException {

    /**
     * Constructs a new CouponException with the specified failure message.
     *
     * @param message the detail error message explaining the rejection reason
     */
    public CouponException(String message) {
        super(message);
    }
}
