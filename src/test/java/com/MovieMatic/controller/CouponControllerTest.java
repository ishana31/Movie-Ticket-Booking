package com.MovieMatic.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.service.CouponService;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test suite verifying REST API contracts for the coupon validation endpoint.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

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
    @DisplayName("POST /user/coupons/validate returns 200 OK with discount calculation for valid coupon")
    void shouldReturn200WithDiscountDetailsForValidCoupon() throws Exception {
        when(couponService.validateCoupon("SAVE20", 500.0))
                .thenReturn(new CouponValidationResponse(true, 100.0, 400.0,
                        "Coupon 'SAVE20' applied! You saved ₹100.00"));

        String requestJson = "{\"code\": \"SAVE20\", \"totalAmount\": 500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(100.0))
                .andExpect(jsonPath("$.finalAmount").value(400.0));
    }

    @Test
    @DisplayName("POST /user/coupons/validate returns 400 Bad Request when coupon is expired or invalid")
    void shouldReturn400BadRequestForInvalidOrExpiredCoupon() throws Exception {
        when(couponService.validateCoupon("INVALID", 500.0))
                .thenThrow(new CouponException("Invalid or expired coupon code"));

        String requestJson = "{\"code\": \"INVALID\", \"totalAmount\": 500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    @DisplayName("POST /user/coupons/validate returns 400 Bad Request when spend threshold is not met")
    void shouldReturn400BadRequestWhenMinimumSpendNotMet() throws Exception {
        when(couponService.validateCoupon("SAVE20", 150.0))
                .thenThrow(new CouponException("Minimum booking amount of ₹300.00 required."));

        String requestJson = "{\"code\": \"SAVE20\", \"totalAmount\": 150.0}";

        mockMvc.perform(post("/user/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum booking amount of ₹300.00 required."));
    }
}
