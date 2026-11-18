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
import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.service.CouponService;

@RestController
@RequestMapping("/user/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<?> validateCoupon(@RequestBody CouponValidationRequest request) {
        String code = request != null ? request.getCode() : null;
        double totalAmount = request != null ? request.getTotalAmount() : 0.0;

        CouponValidationResponse result = couponService.validateAndCalculate(code, totalAmount);

        if (result.isValid()) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("valid", true);
            response.put("discountAmount", result.getDiscountAmount());
            response.put("finalAmount", result.getFinalAmount());
            if (result.getMessage() != null) {
                response.put("message", result.getMessage());
            }
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("message", result.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
