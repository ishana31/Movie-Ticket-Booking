package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying domain discount calculations, expiration checks,
 * minimum order spend criteria, and usage limits in {@link CouponService}.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository);
    }

    @Test
    @DisplayName("Should successfully calculate percentage discount when conditions are met")
    void shouldCalculatePercentageDiscountSuccessfully() {
        // Given SAVE20: 20% discount on min ₹300 order
        Coupon save20 = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(10),
                100,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(save20));

        // When validating for ₹500 order
        CouponValidationResponse response = couponService.validateCoupon("SAVE20", 500.0);

        // Then 20% of 500 is 100, final payable is 400
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount(), 0.001);
        assertEquals(400.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should successfully calculate flat rupee discount")
    void shouldCalculateFlatDiscountSuccessfully() {
        // Given FLAT50: ₹50 discount on min ₹200 order
        Coupon flat50 = new Coupon(
                "FLAT50",
                DiscountType.FLAT,
                50.0,
                200.0,
                LocalDateTime.now().plusDays(10),
                500,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
                .thenReturn(Optional.of(flat50));

        // When validating for ₹350 order
        CouponValidationResponse response = couponService.validateCoupon("FLAT50", 350.0);

        // Then discount is 50, final payable is 300
        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(50.0, response.getDiscountAmount(), 0.001);
        assertEquals(300.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should reject coupon when expiration date is in the past")
    void shouldRejectExpiredCoupon() {
        // Given expired coupon
        Coupon expiredCoupon = new Coupon(
                "EXPIRED20",
                DiscountType.PERCENTAGE,
                20.0,
                100.0,
                LocalDateTime.now().minusDays(1),
                100,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED20"))
                .thenReturn(Optional.of(expiredCoupon));

        // Expect rejection with standard error message
        CouponException exception = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("EXPIRED20", 500.0));
        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when order subtotal is below minimum spend threshold")
    void shouldRejectWhenOrderAmountIsBelowMinimumSpend() {
        // Given SAVE20 with min ₹300
        Coupon minSpendCoupon = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(5),
                100,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(minSpendCoupon));

        // When order is only ₹250 (< ₹300)
        CouponException exception = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("SAVE20", 250.0));
        assertEquals("Minimum booking amount of ₹300.00 required.", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when global usage limit has been reached")
    void shouldRejectWhenUsageLimitIsReached() {
        // Given coupon with limit 5, used 5
        Coupon maxedOutCoupon = new Coupon(
                "MAXED5",
                DiscountType.FLAT,
                20.0,
                100.0,
                LocalDateTime.now().plusDays(5),
                5,
                true
        );
        maxedOutCoupon.setUsedCount(5);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("MAXED5"))
                .thenReturn(Optional.of(maxedOutCoupon));

        CouponException exception = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("MAXED5", 200.0));
        assertEquals("Invalid or expired coupon code", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle case-insensitive code matching seamlessly")
    void shouldHandleCaseInsensitiveCodeMatching() {
        Coupon save20 = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(10),
                100,
                true
        );
        // Repository handles case-insensitivity
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(save20));

        CouponValidationResponse response = couponService.validateCoupon("save20", 500.0);
        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount(), 0.001);
    }

    @Test
    @DisplayName("Should cap flat discount to not exceed the gross total amount")
    void shouldCapFlatDiscountAtGrossTotalAmount() {
        // Flat discount ₹100 on ₹60 order
        Coupon bigDiscount = new Coupon(
                "BIG100",
                DiscountType.FLAT,
                100.0,
                50.0,
                LocalDateTime.now().plusDays(10),
                100,
                true
        );
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIG100"))
                .thenReturn(Optional.of(bigDiscount));

        CouponValidationResponse response = couponService.validateCoupon("BIG100", 60.0);
        assertEquals(60.0, response.getDiscountAmount(), 0.001);
        assertEquals(0.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should increment used count upon booking redemption")
    void shouldIncrementUsedCountUponRedemption() {
        Coupon save20 = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(10),
                100,
                true
        );
        save20.setUsedCount(10);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(save20));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CouponService.CouponRedemptionResult result =
                couponService.applyCouponForBooking("SAVE20", 500.0);

        assertEquals(11, result.getCoupon().getUsedCount());
        assertEquals(100.0, result.getDiscountAmount(), 0.001);
        assertEquals(400.0, result.getFinalAmount(), 0.001);
        verify(couponRepository).save(save20);
    }
}
