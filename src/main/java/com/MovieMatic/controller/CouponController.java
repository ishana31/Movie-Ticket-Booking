package com.MovieMatic.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * REST Controller for promotional coupon endpoints.
 * Handles validation of coupon codes against cart totals and provides instant discount breakdowns.
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
     * Validates a promotional coupon code against an order total.
     *
     * @param request validation request payload containing coupon code and pre-discount amount
     * @return 200 OK with CouponValidationResponse if valid; 400 Bad Request with error message if invalid
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestBody CouponValidationRequest request) {
        log.info("Received request to validate coupon: {}", request != null ? request.getCode() : "null");

        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Invalid or expired coupon code");
            errorBody.put("valid", false);
            return ResponseEntity.badRequest().body(errorBody);
        }

        try {
            CouponValidationResponse response = couponService.validateCoupon(
                    request.getCode(), request.getTotalAmount());
            return ResponseEntity.ok(response);
        } catch (CouponException e) {
            log.warn("Coupon validation failed: {}", e.getMessage());
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", e.getMessage());
            errorBody.put("valid", false);
            return ResponseEntity.badRequest().body(errorBody);
        } catch (Exception e) {
            log.error("Unexpected error validating coupon: {}", e.getMessage(), e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "Invalid or expired coupon code");
            errorBody.put("valid", false);
            return ResponseEntity.badRequest().body(errorBody);
        }
    }
}
