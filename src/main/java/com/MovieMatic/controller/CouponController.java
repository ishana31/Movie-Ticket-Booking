package com.MovieMatic.controller;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MovieMatic.dto.CouponValidationRequest;
import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.service.CouponService;

/**
 * REST controller exposing endpoints for coupon validation and discount computation
 * during the customer checkout journey.
 *
 * @author CodeFactoryAgent
 */
@RestController
@RequestMapping("/user/coupons")
public class CouponController {

    private static final Logger log = LoggerFactory.getLogger(CouponController.class);

    private final CouponService couponService;

    @Autowired
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Validates a promotional coupon code for a given order amount.
     *
     * @param request the {@link CouponValidationRequest} containing the promo code and order subtotal
     * @return 200 OK with {@link CouponValidationResponse} if valid, or 400 Bad Request with error message
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestBody CouponValidationRequest request) {
        log.info("Received coupon validation request for code: '{}', totalAmount: ₹{}",
                request != null ? request.getCode() : "null",
                request != null ? request.getTotalAmount() : "null");

        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid or expired coupon code"));
        }

        try {
            CouponValidationResponse response = couponService.validateCoupon(
                    request.getCode(), request.getTotalAmount());
            return ResponseEntity.ok(response);
        } catch (CouponException ex) {
            log.warn("Coupon validation failed for '{}': {}", request.getCode(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error validating coupon '{}'", request.getCode(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid or expired coupon code"));
        }
    }
}
