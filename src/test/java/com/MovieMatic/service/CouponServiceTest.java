package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;
import com.MovieMatic.service.CouponService.CouponValidationResult;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validPercentageCoupon;
    private Coupon validFlatCoupon;

    @BeforeEach
    void setUp() {
        validPercentageCoupon = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(7),
                100,
                0,
                true
        );

        validFlatCoupon = new Coupon(
                "FLAT50",
                DiscountType.FLAT,
                50.0,
                200.0,
                LocalDateTime.now().plusDays(7),
                50,
                5,
                true
        );
    }

    @Test
    void testPercentageDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("SAVE20", 500.0);

        assertTrue(result.isValid());
        assertEquals(100.0, result.getDiscountAmount(), 0.001);
        assertEquals(400.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testFlatDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
                .thenReturn(Optional.of(validFlatCoupon));

        CouponValidationResult result = couponService.validateCoupon("FLAT50", 500.0);

        assertTrue(result.isValid());
        assertEquals(50.0, result.getDiscountAmount(), 0.001);
        assertEquals(450.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testFlatDiscountExceedsTotalAmount_ClampedToZero() {
        Coupon bigFlat = new Coupon("BIG500", DiscountType.FLAT, 500.0, 100.0,
                LocalDateTime.now().plusDays(1), 10, 0, true);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIG500"))
                .thenReturn(Optional.of(bigFlat));

        CouponValidationResult result = couponService.validateCoupon("BIG500", 300.0);

        assertTrue(result.isValid());
        assertEquals(300.0, result.getDiscountAmount(), 0.001);
        assertEquals(0.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testMinimumSpendRequirement_Unmet() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("SAVE20", 250.0);

        assertFalse(result.isValid());
        assertEquals("Minimum booking amount of ₹300.00 required.", result.getMessage());
    }

    @Test
    void testMinimumSpendRequirement_Met() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("SAVE20", 300.0);

        assertTrue(result.isValid());
        assertEquals(60.0, result.getDiscountAmount(), 0.001);
        assertEquals(240.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testExpiredCouponRejection() {
        Coupon expiredCoupon = new Coupon(
                "EXPIRED10",
                DiscountType.PERCENTAGE,
                10.0,
                100.0,
                LocalDateTime.now().minusDays(1),
                100,
                0,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED10"))
                .thenReturn(Optional.of(expiredCoupon));

        CouponValidationResult result = couponService.validateCoupon("EXPIRED10", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testUsageLimitReached_Rejected() {
        Coupon limitReachedCoupon = new Coupon(
                "LIMITMAX",
                DiscountType.FLAT,
                50.0,
                100.0,
                LocalDateTime.now().plusDays(5),
                10,
                10,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("LIMITMAX"))
                .thenReturn(Optional.of(limitReachedCoupon));

        CouponValidationResult result = couponService.validateCoupon("LIMITMAX", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testInactiveOrNotFoundCoupon_Rejected() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("UNKNOWN"))
                .thenReturn(Optional.empty());

        CouponValidationResult result = couponService.validateCoupon("UNKNOWN", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testNullOrBlankCouponCode_Rejected() {
        CouponValidationResult nullResult = couponService.validateCoupon(null, 500.0);
        assertFalse(nullResult.isValid());
        assertEquals("Invalid or expired coupon code", nullResult.getMessage());

        CouponValidationResult emptyResult = couponService.validateCoupon("   ", 500.0);
        assertFalse(emptyResult.isValid());
        assertEquals("Invalid or expired coupon code", emptyResult.getMessage());
    }

    @Test
    void testCaseInsensitiveMatching() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("save20", 500.0);

        assertTrue(result.isValid());
        assertEquals(100.0, result.getDiscountAmount(), 0.001);
        assertEquals(400.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testIncrementUsedCount() {
        assertEquals(0, validPercentageCoupon.getUsedCount());

        couponService.incrementUsedCount(validPercentageCoupon);

        assertEquals(1, validPercentageCoupon.getUsedCount());
        verify(couponRepository).save(validPercentageCoupon);
    }
}
