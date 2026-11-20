package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.MovieMatic.dto.CouponValidationResult;
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
    private Coupon expiredCoupon;
    private Coupon maxedOutCoupon;
    private Coupon minSpendCoupon;

    @BeforeEach
    void setUp() {
        percentageCoupon = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                0.0,
                LocalDateTime.now().plusDays(10),
                100,
                true
        );
        percentageCoupon.setId(1L);

        flatCoupon = new Coupon(
                "FLAT50",
                DiscountType.FLAT,
                50.0,
                0.0,
                LocalDateTime.now().plusDays(10),
                50,
                true
        );
        flatCoupon.setId(2L);

        expiredCoupon = new Coupon(
                "EXPIRED10",
                DiscountType.PERCENTAGE,
                10.0,
                0.0,
                LocalDateTime.now().minusDays(1),
                100,
                true
        );
        expiredCoupon.setId(3L);

        maxedOutCoupon = new Coupon(
                "MAXED",
                DiscountType.PERCENTAGE,
                15.0,
                0.0,
                LocalDateTime.now().plusDays(5),
                5,
                true
        );
        maxedOutCoupon.setId(4L);
        maxedOutCoupon.setUsedCount(5);

        minSpendCoupon = new Coupon(
                "BIGSPEND",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(5),
                100,
                true
        );
        minSpendCoupon.setId(5L);
    }

    @Test
    void testValidateCoupon_PercentageDiscount_Success() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(percentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("SAVE20", 500.0);

        assertTrue(result.isValid());
        assertEquals(100.0, result.getDiscountAmount());
        assertEquals(400.0, result.getFinalAmount());
        assertNotNull(result.getCoupon());
        assertEquals("SAVE20", result.getCoupon().getCode());
    }

    @Test
    void testValidateCoupon_FlatDiscount_Success() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
                .thenReturn(Optional.of(flatCoupon));

        CouponValidationResult result = couponService.validateCoupon("FLAT50", 500.0);

        assertTrue(result.isValid());
        assertEquals(50.0, result.getDiscountAmount());
        assertEquals(450.0, result.getFinalAmount());
    }

    @Test
    void testValidateCoupon_CaseInsensitiveMatching() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(percentageCoupon));

        CouponValidationResult result = couponService.validateCoupon("save20", 500.0);

        assertTrue(result.isValid());
        assertEquals(100.0, result.getDiscountAmount());
        assertEquals(400.0, result.getFinalAmount());
    }

    @Test
    void testValidateCoupon_ExpiredRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED10"))
                .thenReturn(Optional.of(expiredCoupon));

        CouponValidationResult result = couponService.validateCoupon("EXPIRED10", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testValidateCoupon_UsageLimitReachedRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("MAXED"))
                .thenReturn(Optional.of(maxedOutCoupon));

        CouponValidationResult result = couponService.validateCoupon("MAXED", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testValidateCoupon_MinSpendUnmetRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIGSPEND"))
                .thenReturn(Optional.of(minSpendCoupon));

        CouponValidationResult result = couponService.validateCoupon("BIGSPEND", 200.0);

        assertFalse(result.isValid());
        assertEquals("Minimum booking amount of ₹300.00 required.", result.getMessage());
    }

    @Test
    void testValidateCoupon_MinSpendMet_Success() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIGSPEND"))
                .thenReturn(Optional.of(minSpendCoupon));

        CouponValidationResult result = couponService.validateCoupon("BIGSPEND", 500.0);

        assertTrue(result.isValid());
        assertEquals(100.0, result.getDiscountAmount());
        assertEquals(400.0, result.getFinalAmount());
    }

    @Test
    void testValidateCoupon_NonExistentCouponRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("NOTREAL"))
                .thenReturn(Optional.empty());

        CouponValidationResult result = couponService.validateCoupon("NOTREAL", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testValidateCoupon_BlankCodeRejection() {
        CouponValidationResult result = couponService.validateCoupon("   ", 500.0);

        assertFalse(result.isValid());
        assertEquals("Invalid or expired coupon code", result.getMessage());
    }

    @Test
    void testValidateCoupon_DiscountCappedAtTotalAmount() {
        Coupon hugeFlatCoupon = new Coupon(
                "HUGE1000",
                DiscountType.FLAT,
                1000.0,
                0.0,
                LocalDateTime.now().plusDays(1),
                10,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("HUGE1000"))
                .thenReturn(Optional.of(hugeFlatCoupon));

        CouponValidationResult result = couponService.validateCoupon("HUGE1000", 250.0);

        assertTrue(result.isValid());
        assertEquals(250.0, result.getDiscountAmount());
        assertEquals(0.0, result.getFinalAmount());
    }

    @Test
    void testIncrementUsedCount() {
        assertEquals(0, percentageCoupon.getUsedCount());

        couponService.incrementUsedCount(percentageCoupon);

        assertEquals(1, percentageCoupon.getUsedCount());
        verify(couponRepository).save(percentageCoupon);
    }
}
