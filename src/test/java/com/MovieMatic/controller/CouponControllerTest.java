package com.MovieMatic.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.CouponService.CouponValidationResult;

@ExtendWith(MockitoExtension.class)
public class CouponControllerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
    }

    @Test
    void testValidateCoupon_Success() throws Exception {
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0,
                LocalDateTime.now().plusDays(5), 100, 0, true);
        CouponValidationResult successResult = CouponValidationResult.success(coupon, 100.0, 400.0);

        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(successResult);

        String json = "{\"code\":\"SAVE20\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(100.0))
                .andExpect(jsonPath("$.finalAmount").value(400.0));
    }

    @Test
    void testValidateCoupon_InvalidCode() throws Exception {
        CouponValidationResult failureResult = CouponValidationResult.failure("Invalid or expired coupon code");

        when(couponService.validateCoupon("BADCODE", 500.0)).thenReturn(failureResult);

        String json = "{\"code\":\"BADCODE\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    void testValidateCoupon_BelowMinimumSpend() throws Exception {
        CouponValidationResult failureResult = CouponValidationResult.failure("Minimum booking amount of ₹300.00 required.");

        when(couponService.validateCoupon("SAVE20", 200.0)).thenReturn(failureResult);

        String json = "{\"code\":\"SAVE20\",\"totalAmount\":200.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Minimum booking amount of ₹300.00 required."));
    }
}
