package com.MovieMatic.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.MovieMatic.dto.BookingRequest;
import com.MovieMatic.dto.BookingResponse;
import com.MovieMatic.dto.CouponValidationRequest;
import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@ExtendWith(MockitoExtension.class)
public class CouponControllerTest {

    @Mock
    private CouponService couponService;

    @Mock
    private BookingService bookingService;

    @Mock
    private UserService userService;

    @Mock
    private ShowService showService;

    @Mock
    private MovieService movieService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private User mockUser;
    private Show mockShow;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("Test User");
        mockUser.setEmail("user@example.com");

        Movie mockMovie = new Movie();
        mockMovie.setId(1L);
        mockMovie.setTitle("Inception");

        mockShow = new Show();
        mockShow.setId(1L);
        mockShow.setMovie(mockMovie);
        mockShow.setPrice(250.0);
        mockShow.setScreen("Screen 1");
        mockShow.setShowDate(LocalDate.now());
        mockShow.setShowTime(LocalTime.of(18, 0));
    }

    @Test
    void testValidateCouponSuccess() {
        CouponValidationRequest request = new CouponValidationRequest("SAVE20", 500.0);
        com.MovieMatic.model.Coupon coupon = new com.MovieMatic.model.Coupon(
            "SAVE20", "PERCENTAGE", 20.0, 300.0, LocalDateTime.now().plusDays(10), 100, 0, true
        );
        CouponValidationResult result = new CouponValidationResult(coupon, 100.0, 400.0);

        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(result);

        ResponseEntity<?> response = userController.validateCoupon(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof CouponValidationResponse);
        CouponValidationResponse body = (CouponValidationResponse) response.getBody();
        assertTrue(body.isValid());
        assertEquals(100.0, body.getDiscountAmount());
        assertEquals(400.0, body.getFinalAmount());
    }

    @Test
    void testValidateCouponInvalidOrExpired() {
        CouponValidationRequest request = new CouponValidationRequest("INVALID", 500.0);
        when(couponService.validateCoupon("INVALID", 500.0))
            .thenThrow(new CouponValidationException("Invalid or expired coupon code"));

        ResponseEntity<?> response = userController.validateCoupon(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testValidateCouponBelowThreshold() {
        CouponValidationRequest request = new CouponValidationRequest("SAVE20", 200.0);
        when(couponService.validateCoupon("SAVE20", 200.0))
            .thenThrow(new CouponValidationException("Minimum booking amount of ₹300.00 required."));

        ResponseEntity<?> response = userController.validateCoupon(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testCreateBookingWithValidCoupon() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        // 2 seats @ 250.0 = 500.0 original amount
        BookingRequest request = new BookingRequest();
        request.setShowId(1L);
        request.setSeatNumbers("A1,A2");
        request.setCouponCode("SAVE20");

        com.MovieMatic.model.Coupon coupon = new com.MovieMatic.model.Coupon(
            "SAVE20", "PERCENTAGE", 20.0, 300.0, LocalDateTime.now().plusDays(10), 100, 0, true
        );
        CouponValidationResult result = new CouponValidationResult(coupon, 100.0, 400.0);

        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(result);
        doNothing().when(couponService).recordCouponUsage("SAVE20");
        doNothing().when(bookingService).saveBooking(any(Booking.class));

        ResponseEntity<?> response = userController.createBooking(request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof BookingResponse);
        BookingResponse bookingResponse = (BookingResponse) response.getBody();
        assertEquals(400.0, bookingResponse.getTotalAmount());
        assertEquals(100.0, bookingResponse.getDiscountAmount());
        verify(couponService).recordCouponUsage("SAVE20");
    }

    @Test
    void testCreateBookingWithoutCoupon() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        BookingRequest request = new BookingRequest();
        request.setShowId(1L);
        request.setSeatNumbers("A1,A2");
        request.setCouponCode(null);

        doNothing().when(bookingService).saveBooking(any(Booking.class));

        ResponseEntity<?> response = userController.createBooking(request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BookingResponse bookingResponse = (BookingResponse) response.getBody();
        assertEquals(500.0, bookingResponse.getTotalAmount());
        assertEquals(0.0, bookingResponse.getDiscountAmount());
        verify(couponService, never()).recordCouponUsage(anyString());
    }

    @Test
    void testCreateBookingWithInvalidCouponFails() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        BookingRequest request = new BookingRequest();
        request.setShowId(1L);
        request.setSeatNumbers("A1,A2");
        request.setCouponCode("BADCODE");

        when(couponService.validateCoupon("BADCODE", 500.0))
            .thenThrow(new CouponValidationException("Invalid or expired coupon code"));

        ResponseEntity<?> response = userController.createBooking(request, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(bookingService, never()).saveBooking(any(Booking.class));
    }
}
