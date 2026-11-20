package com.MovieMatic.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MovieMatic.dto.CouponValidateRequest;
import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.service.CouponService;

@RestController
@RequestMapping("/user/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestBody CouponValidateRequest request) {
        if (request == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", "Invalid or expired coupon code");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        CouponValidationResult result = couponService.validateCoupon(request.getCode(), request.getTotalAmount());

        if (!result.isValid()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", result.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", true);
        response.put("discountAmount", result.getDiscountAmount());
        response.put("finalAmount", result.getFinalAmount());
        if (result.getCoupon() != null) {
            response.put("couponCode", result.getCoupon().getCode());
        }

        return ResponseEntity.ok(response);
    }
}
