package com.MovieMatic.exception;

public class CouponValidationException extends RuntimeException {
    public CouponValidationException(String message) {
        super(message);
    }
}
