package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

/**
 * Unit tests verifying promotional discount calculations, minimum spend enforcement,
 * case-insensitivity, and expiration handling in CouponService.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
public class CouponServiceTest {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceTest.class);

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon validPercentageCoupon;
    private Coupon validFlatCoupon;
    private Coupon expiredCoupon;
    private Coupon exhaustedCoupon;

    @BeforeEach
    void setUp() {
        validPercentageCoupon = new Coupon(
                "SAVE20",
                DiscountType.PERCENTAGE,
                20.0,
                300.0,
                LocalDateTime.now().plusDays(30),
                100,
                true
        );

        validFlatCoupon = new Coupon(
                "FLAT50",
                DiscountType.FLAT,
                50.0,
                200.0,
                LocalDateTime.now().plusDays(30),
                100,
                true
        );

        expiredCoupon = new Coupon(
                "EXPIRED10",
                DiscountType.PERCENTAGE,
                10.0,
                100.0,
                LocalDateTime.now().minusDays(1),
                100,
                true
        );

        exhaustedCoupon = new Coupon(
                "LIMITMAX",
                DiscountType.FLAT,
                25.0,
                100.0,
                LocalDateTime.now().plusDays(10),
                5,
                true
        );
        exhaustedCoupon.setUsedCount(5);
    }

    @Test
    @DisplayName("Should successfully calculate percentage discount when order meets minimum spend")
    void testValidateCoupon_PercentageDiscount_Success() {
        log.info("Testing percentage discount calculation for SAVE20 on order total 500.0");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResponse response = couponService.validateCoupon("SAVE20", 500.0);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount(), 0.001);
        assertEquals(400.0, response.getFinalAmount(), 0.001);
        assertTrue(response.getMessage().contains("You saved ₹100.00"));
    }

    @Test
    @DisplayName("Should successfully match coupon code in a case-insensitive manner (e.g. 'save20')")
    void testValidateCoupon_CaseInsensitiveMatch_Success() {
        log.info("Testing case-insensitive matching for lowercase code 'save20'");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("save20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponValidationResponse response = couponService.validateCoupon("save20", 500.0);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(100.0, response.getDiscountAmount(), 0.001);
        assertEquals(400.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should successfully calculate flat rate discount")
    void testValidateCoupon_FlatDiscount_Success() {
        log.info("Testing flat discount calculation for FLAT50 on order total 250.0");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("FLAT50"))
                .thenReturn(Optional.of(validFlatCoupon));

        CouponValidationResponse response = couponService.validateCoupon("FLAT50", 250.0);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(50.0, response.getDiscountAmount(), 0.001);
        assertEquals(200.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should cap flat discount to total amount when discount exceeds order total")
    void testValidateCoupon_FlatDiscountExceedsTotal_CapsAtTotal() {
        log.info("Testing flat discount capping when discount exceeds total");
        Coupon generousCoupon = new Coupon("BIG500", DiscountType.FLAT, 500.0, 0.0,
                LocalDateTime.now().plusDays(10), 50, true);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("BIG500"))
                .thenReturn(Optional.of(generousCoupon));

        CouponValidationResponse response = couponService.validateCoupon("BIG500", 300.0);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(300.0, response.getDiscountAmount(), 0.001);
        assertEquals(0.0, response.getFinalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should reject coupon when booking amount is below minimum spend threshold")
    void testValidateCoupon_BelowMinimumSpend_ThrowsCouponException() {
        log.info("Testing minimum spend rejection for SAVE20 when order total is 200.0 (min required 300.0)");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        CouponException ex = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("SAVE20", 200.0));

        assertEquals("Minimum booking amount of ₹300.00 required.", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject expired coupon")
    void testValidateCoupon_ExpiredCoupon_ThrowsCouponException() {
        log.info("Testing rejection of expired coupon EXPIRED10");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("EXPIRED10"))
                .thenReturn(Optional.of(expiredCoupon));

        CouponException ex = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("EXPIRED10", 500.0));

        assertEquals("Invalid or expired coupon code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when usage limit has been reached")
    void testValidateCoupon_UsageLimitExhausted_ThrowsCouponException() {
        log.info("Testing rejection of exhausted coupon LIMITMAX");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("LIMITMAX"))
                .thenReturn(Optional.of(exhaustedCoupon));

        CouponException ex = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("LIMITMAX", 200.0));

        assertEquals("Invalid or expired coupon code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when code does not exist in repository")
    void testValidateCoupon_NonExistentCode_ThrowsCouponException() {
        log.info("Testing rejection of non-existent coupon NONEXISTENT");
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("NONEXISTENT"))
                .thenReturn(Optional.empty());

        CouponException ex = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("NONEXISTENT", 500.0));

        assertEquals("Invalid or expired coupon code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject coupon when input code is blank or null")
    void testValidateCoupon_BlankCode_ThrowsCouponException() {
        CouponException ex1 = assertThrows(CouponException.class, () ->
                couponService.validateCoupon(null, 500.0));
        assertEquals("Invalid or expired coupon code", ex1.getMessage());

        CouponException ex2 = assertThrows(CouponException.class, () ->
                couponService.validateCoupon("   ", 500.0));
        assertEquals("Invalid or expired coupon code", ex2.getMessage());
    }

    @Test
    @DisplayName("Should increment used_count and save coupon when recording usage")
    void testRecordCouponUsage_IncrementsUsageCount() {
        log.info("Testing recordCouponUsage increments used count");
        validPercentageCoupon.setUsedCount(3);
        when(couponRepository.findByCodeIgnoreCaseAndIsActiveTrue("SAVE20"))
                .thenReturn(Optional.of(validPercentageCoupon));

        couponService.recordCouponUsage("SAVE20");

        assertEquals(4, validPercentageCoupon.getUsedCount());
        verify(couponRepository).save(validPercentageCoupon);
    }
}
