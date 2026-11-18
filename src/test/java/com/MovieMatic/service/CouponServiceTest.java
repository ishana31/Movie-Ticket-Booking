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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validPercentageCoupon;
    private Coupon validFlatCoupon;
    private Coupon expiredCoupon;
    private Coupon limitReachedCoupon;

    @BeforeEach
    void setUp() {
        validPercentageCoupon = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(10),
                100,
                0,
                true
        );
        validPercentageCoupon.setId(1L);

        validFlatCoupon = new Coupon(
                "FLAT50",
                DiscountType.FLAT,
                50.0,
                200.0,
                LocalDateTime.now().plusDays(10),
                50,
                0,
                true
        );
        validFlatCoupon.setId(2L);

        expiredCoupon = new Coupon(
                "EXPIRED",
                DiscountType.PERCENTAGE,
                15.0,
                100.0,
                LocalDateTime.now().minusDays(1),
                100,
                0,
                true
        );
        expiredCoupon.setId(3L);

        limitReachedCoupon = new Coupon(
                "MAXED",
                DiscountType.FLAT,
                25.0,
                100.0,
                LocalDateTime.now().plusDays(5),
                5,
                5,
                true
        );
        limitReachedCoupon.setId(4L);
    }

    @Test
    @DisplayName("Should calculate percentage discount correctly and return 200 payload structure")
    void testPercentageDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("SAVE20", 500.0);

        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount());
        assertEquals(400.0, response.getFinalAmount());
        assertTrue(response.getMessage().contains("SAVE20"));
        assertTrue(response.getMessage().contains("100.00"));
    }

    @Test
    @DisplayName("Should calculate flat discount correctly")
    void testFlatDiscountCalculation() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
                .thenReturn(Optional.of(validFlatCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("FLAT50", 250.0);

        assertTrue(response.isValid());
        assertEquals(50.0, response.getDiscountAmount());
        assertEquals(200.0, response.getFinalAmount());
    }

    @Test
    @DisplayName("Should cap flat discount if discount exceeds total order amount")
    void testFlatDiscountCappedAtTotalAmount() {
        Coupon bigFlat = new Coupon("BIG100", DiscountType.FLAT, 100.0, 50.0, LocalDateTime.now().plusDays(2), 10, 0, true);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIG100"))
                .thenReturn(Optional.of(bigFlat));

        CouponValidationResponse response = couponService.validateAndCalculate("BIG100", 80.0);

        assertTrue(response.isValid());
        assertEquals(80.0, response.getDiscountAmount());
        assertEquals(0.0, response.getFinalAmount());
    }

    @Test
    @DisplayName("Should match coupon codes case-insensitively (e.g. save20 matches SAVE20)")
    void testCaseInsensitiveCouponCodeMatching() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("save20", 500.0);

        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount());
        assertEquals(400.0, response.getFinalAmount());
    }

    @Test
    @DisplayName("Should reject coupon when booking amount is below minimum order amount threshold")
    void testMinimumBookingAmountRequirement() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("SAVE20", 250.0);

        assertFalse(response.isValid());
        assertEquals("Minimum booking amount of ₹300.00 required.", response.getMessage());
    }

    @Test
    @DisplayName("Should reject expired coupon")
    void testExpiredCouponRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED"))
                .thenReturn(Optional.of(expiredCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("EXPIRED", 500.0);

        assertFalse(response.isValid());
        assertEquals("Invalid or expired coupon code", response.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when usage limit is reached (used_count >= usage_limit)")
    void testUsageLimitReachedRejection() {
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("MAXED"))
                .thenReturn(Optional.of(limitReachedCoupon));

        CouponValidationResponse response = couponService.validateAndCalculate("MAXED", 500.0);

        assertFalse(response.isValid());
        assertEquals("Invalid or expired coupon code", response.getMessage());
    }

    @Test
    @DisplayName("Should reject non-existent or null/empty coupon code")
    void testNonExistentAndBlankCodeRejection() {
        CouponValidationResponse nullResp = couponService.validateAndCalculate(null, 500.0);
        assertFalse(nullResp.isValid());
        assertEquals("Invalid or expired coupon code", nullResp.getMessage());

        CouponValidationResponse emptyResp = couponService.validateAndCalculate("   ", 500.0);
        assertFalse(emptyResp.isValid());
        assertEquals("Invalid or expired coupon code", emptyResp.getMessage());

        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("NOTREAL"))
                .thenReturn(Optional.empty());
        CouponValidationResponse notFoundResp = couponService.validateAndCalculate("NOTREAL", 500.0);
        assertFalse(notFoundResp.isValid());
        assertEquals("Invalid or expired coupon code", notFoundResp.getMessage());
    }

    @Test
    @DisplayName("Should increment coupon used_count on booking confirmation")
    void testIncrementUsedCount() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(validPercentageCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(validPercentageCoupon);

        couponService.incrementUsedCount(validPercentageCoupon);

        assertEquals(1, validPercentageCoupon.getUsedCount());
        verify(couponRepository).save(validPercentageCoupon);
    }
}
