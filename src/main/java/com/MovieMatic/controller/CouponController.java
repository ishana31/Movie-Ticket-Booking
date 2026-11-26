package com.MovieMatic.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MovieMatic.dto.CouponValidationRequest;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.CouponService.CouponValidationResult;

@RestController
@RequestMapping("/user/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestBody CouponValidationRequest request) {
        String code = request != null ? request.getCode() : null;
        double totalAmount = request != null ? request.getTotalAmount() : 0.0;

        CouponValidationResult result = couponService.validateCoupon(code, totalAmount);
        Map<String, Object> response = new LinkedHashMap<>();

        if (result.isValid()) {
            response.put("valid", true);
            response.put("discountAmount", result.getDiscountAmount());
            response.put("finalAmount", result.getFinalAmount());
            return ResponseEntity.ok(response);
        } else {
            response.put("valid", false);
            response.put("message", result.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
