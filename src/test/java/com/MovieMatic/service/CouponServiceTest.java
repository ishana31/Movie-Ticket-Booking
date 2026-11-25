package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentageCoupon;
    private Coupon flatCoupon;
    private Coupon expiredCoupon;
    private Coupon limitReachedCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = new Coupon(
            "SAVE20",
            "PERCENTAGE",
            20.0,
            300.0,
            LocalDateTime.now().plusDays(30),
            100,
            0,
            true
        );

        flatCoupon = new Coupon(
            "FLAT50",
            "FLAT",
            50.0,
            200.0,
            LocalDateTime.now().plusDays(30),
            100,
            0,
            true
        );

        expiredCoupon = new Coupon(
            "EXPIRED",
            "PERCENTAGE",
            10.0,
            100.0,
            LocalDateTime.now().minusDays(1),
            100,
            0,
            true
        );

        limitReachedCoupon = new Coupon(
            "MAXED",
            "FLAT",
            50.0,
            100.0,
            LocalDateTime.now().plusDays(30),
            10,
            10,
            true
        );
    }

    @Test
    void testPercentageDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
            .thenReturn(Optional.of(percentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("SAVE20", 500.0);

        assertNotNull(result);
        assertEquals(100.0, result.getDiscountAmount(), 0.001);
        assertEquals(400.0, result.getFinalAmount(), 0.001);
        assertEquals("SAVE20", result.getCoupon().getCode());
    }

    @Test
    void testFlatDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
            .thenReturn(Optional.of(flatCoupon));

        CouponValidationResult result = couponService.validateCoupon("FLAT50", 500.0);

        assertNotNull(result);
        assertEquals(50.0, result.getDiscountAmount(), 0.001);
        assertEquals(450.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testFlatDiscountCappedAtTotalAmount() {
        Coupon bigFlat = new Coupon("FLAT500", "FLAT", 500.0, 50.0, LocalDateTime.now().plusDays(10), 10, 0, true);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT500"))
            .thenReturn(Optional.of(bigFlat));

        CouponValidationResult result = couponService.validateCoupon("FLAT500", 200.0);

        assertNotNull(result);
        assertEquals(200.0, result.getDiscountAmount(), 0.001);
        assertEquals(0.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testCaseInsensitiveCouponCode() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
            .thenReturn(Optional.of(percentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("save20", 500.0);

        assertNotNull(result);
        assertEquals(100.0, result.getDiscountAmount(), 0.001);
        assertEquals(400.0, result.getFinalAmount(), 0.001);
    }

    @Test
    void testExpiredCouponRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED"))
            .thenReturn(Optional.of(expiredCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("EXPIRED", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testUsageLimitReachedRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("MAXED"))
            .thenReturn(Optional.of(limitReachedCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("MAXED", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testNonExistentCouponRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("NONEXISTENT"))
            .thenReturn(Optional.empty());

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("NONEXISTENT", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testMinimumBookingAmountRequirement() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
            .thenReturn(Optional.of(percentageCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("SAVE20", 250.0);
        });

        assertEquals("Minimum booking amount of ₹300.00 required.", exception.getMessage());
    }

    @Test
    void testRecordCouponUsageIncrementsCount() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
            .thenReturn(Optional.of(percentageCoupon));

        couponService.recordCouponUsage("SAVE20");

        assertEquals(1, percentageCoupon.getUsedCount());
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }
}
