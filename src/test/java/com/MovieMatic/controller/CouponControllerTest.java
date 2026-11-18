package com.MovieMatic.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.service.CouponService;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
    }

    @Test
    @DisplayName("POST /user/coupons/validate - 200 OK with valid coupon")
    void testValidateCouponSuccess() throws Exception {
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0, null, 100, 0, true);
        CouponValidationResponse mockResponse = CouponValidationResponse.success(
                100.0, 400.0, "Coupon 'SAVE20' applied! You saved ₹100.00", coupon);

        when(couponService.validateAndCalculate(eq("SAVE20"), eq(500.0)))
                .thenReturn(mockResponse);

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
    @DisplayName("POST /user/coupons/validate - 400 Bad Request with invalid or expired code")
    void testValidateCouponExpiredOrInvalid() throws Exception {
        CouponValidationResponse errorResponse = CouponValidationResponse.error("Invalid or expired coupon code");

        when(couponService.validateAndCalculate(eq("INVALID"), eq(500.0)))
                .thenReturn(errorResponse);

        String requestJson = "{\"code\": \"INVALID\", \"totalAmount\": 500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    @DisplayName("POST /user/coupons/validate - 400 Bad Request with unmet minimum order amount")
    void testValidateCouponBelowThreshold() throws Exception {
        CouponValidationResponse errorResponse = CouponValidationResponse.error("Minimum booking amount of ₹300.00 required.");

        when(couponService.validateAndCalculate(eq("SAVE20"), eq(200.0)))
                .thenReturn(errorResponse);

        String requestJson = "{\"code\": \"SAVE20\", \"totalAmount\": 200.0}";

        mockMvc.perform(post("/user/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum booking amount of ₹300.00 required."));
    }
}
