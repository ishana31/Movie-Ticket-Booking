package com.MovieMatic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.MovieMatic.dto.CouponValidationRequest;
import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.service.CouponService;

/**
 * Controller unit tests verifying HTTP responses for promotional coupon validation endpoint
 * at POST /user/coupons/validate.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
public class CouponControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CouponControllerTest.class);

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    @Test
    @DisplayName("POST /user/coupons/validate returns 200 OK with valid discount for active coupon")
    void testValidateCoupon_Valid_Returns200OK() {
        log.info("Testing POST /user/coupons/validate valid coupon response");
        CouponValidationRequest request = new CouponValidationRequest("SAVE20", 500.0);
        CouponValidationResponse expectedResponse = new CouponValidationResponse(
                true, 100.0, 400.0, "Coupon 'SAVE20' applied! You saved ₹100.00");

        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(expectedResponse);

        ResponseEntity<?> responseEntity = couponController.validateCoupon(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody() instanceof CouponValidationResponse);
        CouponValidationResponse body = (CouponValidationResponse) responseEntity.getBody();
        assertTrue(body.isValid());
        assertEquals(100.0, body.getDiscountAmount(), 0.001);
        assertEquals(400.0, body.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("POST /user/coupons/validate returns 400 Bad Request with message when coupon is invalid or expired")
    void testValidateCoupon_Invalid_Returns400BadRequest() {
        log.info("Testing POST /user/coupons/validate invalid coupon response");
        CouponValidationRequest request = new CouponValidationRequest("INVALID99", 500.0);

        when(couponService.validateCoupon("INVALID99", 500.0))
                .thenThrow(new CouponException("Invalid or expired coupon code"));

        ResponseEntity<?> responseEntity = couponController.validateCoupon(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
        assertEquals("Invalid or expired coupon code", body.get("message"));
        assertFalse((Boolean) body.get("valid"));
    }

    @Test
    @DisplayName("POST /user/coupons/validate returns 400 Bad Request when booking amount is below spend threshold")
    void testValidateCoupon_BelowSpendThreshold_Returns400BadRequest() {
        log.info("Testing POST /user/coupons/validate below minimum spend response");
        CouponValidationRequest request = new CouponValidationRequest("SAVE20", 200.0);

        when(couponService.validateCoupon("SAVE20", 200.0))
                .thenThrow(new CouponException("Minimum booking amount of ₹300.00 required."));

        ResponseEntity<?> responseEntity = couponController.validateCoupon(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
        assertEquals("Minimum booking amount of ₹300.00 required.", body.get("message"));
        assertFalse((Boolean) body.get("valid"));
    }

    @Test
    @DisplayName("POST /user/coupons/validate returns 400 Bad Request when request body code is null or empty")
    void testValidateCoupon_EmptyCode_Returns400BadRequest() {
        log.info("Testing POST /user/coupons/validate with empty code");
        CouponValidationRequest request = new CouponValidationRequest("", 500.0);

        ResponseEntity<?> responseEntity = couponController.validateCoupon(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
        assertEquals("Invalid or expired coupon code", body.get("message"));
    }
}
