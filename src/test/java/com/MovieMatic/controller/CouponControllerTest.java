package com.MovieMatic.controller;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.service.CouponService;

@ExtendWith(MockitoExtension.class)
public class CouponControllerTest {

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
    void testValidateCoupon_Valid_Returns200WithDiscount() throws Exception {
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 0.0, null, 10, true);
        CouponValidationResult validResult = CouponValidationResult.valid(coupon, 100.0, 400.0);

        when(couponService.validateCoupon(eq("SAVE20"), eq(500.0)))
                .thenReturn(validResult);

        String requestBody = "{\"code\":\"SAVE20\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(100.0))
                .andExpect(jsonPath("$.finalAmount").value(400.0));
    }

    @Test
    void testValidateCoupon_InvalidOrExpired_Returns400WithMessage() throws Exception {
        CouponValidationResult invalidResult = CouponValidationResult.invalid("Invalid or expired coupon code");

        when(couponService.validateCoupon(eq("EXPIRED"), anyDouble()))
                .thenReturn(invalidResult);

        String requestBody = "{\"code\":\"EXPIRED\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    void testValidateCoupon_BelowMinSpend_Returns400WithSpecificMessage() throws Exception {
        CouponValidationResult invalidResult = CouponValidationResult.invalid("Minimum booking amount of ₹300.00 required.");

        when(couponService.validateCoupon(eq("SAVE20"), eq(200.0)))
                .thenReturn(invalidResult);

        String requestBody = "{\"code\":\"SAVE20\",\"totalAmount\":200.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum booking amount of ₹300.00 required."));
    }
}
