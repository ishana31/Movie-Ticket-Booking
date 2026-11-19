package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon percentageCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0,
                LocalDateTime.now().plusDays(5), 10, 0, true);
        percentageCoupon.setId(1L);

        flatCoupon = new Coupon("FLAT100", DiscountType.FLAT, 100.0, 300.0,
                LocalDateTime.now().plusDays(5), 10, 0, true);
        flatCoupon.setId(2L);
    }

    @Test
    void testPercentageDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("SAVE20", 500.0);

        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount());
        assertEquals(400.0, response.getFinalAmount());
    }

    @Test
    void testFlatDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT100"))
                .thenReturn(Optional.of(flatCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("FLAT100", 500.0);

        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount());
        assertEquals(400.0, response.getFinalAmount());
    }

    @Test
    void testFlatDiscountCappedAtTotalAmount() {
        Coupon bigCoupon = new Coupon("FLAT100", DiscountType.FLAT, 100.0, 0.0,
                LocalDateTime.now().plusDays(5), 10, 0, true);
        double discount = couponService.calculateDiscount(bigCoupon, 60.0);
        assertEquals(60.0, discount);
    }

    @Test
    void testCaseInsensitiveCouponCodeLookup() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(percentageCoupon));

        Coupon coupon = couponService.validateCoupon("save20", 500.0);
        assertNotNull(coupon);
        assertEquals("SAVE20", coupon.getCode());
    }

    @Test
    void testMinimumSpendRequirementUnmet() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("SAVE20", 250.0);
        });

        assertEquals("Minimum booking amount of ₹300.00 required.", exception.getMessage());
    }

    @Test
    void testExpiredCouponRejection() {
        Coupon expiredCoupon = new Coupon("EXPIRED", DiscountType.PERCENTAGE, 10.0, 100.0,
                LocalDateTime.now().minusDays(1), 10, 0, true);

        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED"))
                .thenReturn(Optional.of(expiredCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("EXPIRED", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testUsageLimitExceededRejection() {
        Coupon maxedCoupon = new Coupon("MAXED", DiscountType.PERCENTAGE, 10.0, 100.0,
                LocalDateTime.now().plusDays(1), 5, 5, true);

        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("MAXED"))
                .thenReturn(Optional.of(maxedCoupon));

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("MAXED", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testInactiveOrNonExistentCouponRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("UNKNOWN"))
                .thenReturn(Optional.empty());

        CouponValidationException exception = assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("UNKNOWN", 500.0);
        });

        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    void testBlankOrNullCouponCodeRejection() {
        assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon("", 500.0);
        });
        assertThrows(CouponValidationException.class, () -> {
            couponService.validateCoupon(null, 500.0);
        });
    }

    @Test
    void testRecordCouponUsage() {
        assertEquals(0, percentageCoupon.getUsedCount());
        couponService.recordCouponUsage(percentageCoupon);

        assertEquals(1, percentageCoupon.getUsedCount());
        verify(couponRepository).save(percentageCoupon);
    }
}
