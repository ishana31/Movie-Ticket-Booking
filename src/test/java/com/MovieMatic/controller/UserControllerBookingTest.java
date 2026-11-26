package com.MovieMatic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.userdetails.UserDetails;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

/**
 * Unit tests verifying booking creation with promotional coupon recalculation,
 * discount persistence, and security against client-side tampering at POST /user/bookings.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
public class UserControllerBookingTest {

    private static final Logger log = LoggerFactory.getLogger(UserControllerBookingTest.class);

    @Mock
    private MovieService movieService;

    @Mock
    private ShowService showService;

    @Mock
    private UserService userService;

    @Mock
    private BookingService bookingService;

    @Mock
    private EmailService emailService;

    @Mock
    private CouponService couponService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private User sampleUser;
    private Show sampleShow;
    private Movie sampleMovie;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("customer@moviematic.com");
        sampleUser.setName("Customer");

        sampleMovie = new Movie();
        sampleMovie.setId(10L);
        sampleMovie.setTitle("Inception");

        sampleShow = new Show();
        sampleShow.setId(1L);
        sampleShow.setPrice(250.0);
        sampleShow.setMovie(sampleMovie);
        sampleShow.setScreen("Screen 1");
    }

    @Test
    @DisplayName("POST /user/bookings with coupon creates booking with discounted total and coupon code")
    void testCreateBooking_WithValidCoupon_Returns200WithDiscount() {
        log.info("Testing booking creation with valid promotional coupon SAVE20");
        when(userDetails.getUsername()).thenReturn("customer@moviematic.com");
        when(userService.findByEmail("customer@moviematic.com")).thenReturn(sampleUser);
        when(showService.getShowById(1L)).thenReturn(sampleShow);

        // 2 seats @ 250.0 = 500.0 total
        CouponValidationResponse couponResponse = new CouponValidationResponse(
                true, 100.0, 400.0, "Coupon 'SAVE20' applied! You saved ₹100.00");
        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(couponResponse);

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("showId", 1L);
        bookingData.put("seatNumbers", "A1,A2");
        bookingData.put("couponCode", "SAVE20");

        ResponseEntity<?> response = userController.createBooking(bookingData, userDetails);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(400.0, (Double) body.get("totalAmount"), 0.001);
        assertEquals(100.0, (Double) body.get("discountAmount"), 0.001);

        verify(bookingService).saveBooking(any(Booking.class));
        verify(couponService).recordCouponUsage("SAVE20");
    }

    @Test
    @DisplayName("POST /user/bookings without coupon creates booking with standard full amount")
    void testCreateBooking_WithoutCoupon_ReturnsStandardTotal() {
        log.info("Testing booking creation without coupon");
        when(userDetails.getUsername()).thenReturn("customer@moviematic.com");
        when(userService.findByEmail("customer@moviematic.com")).thenReturn(sampleUser);
        when(showService.getShowById(1L)).thenReturn(sampleShow);

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("showId", 1L);
        bookingData.put("seatNumbers", "A1,A2");
        bookingData.put("couponCode", null);

        ResponseEntity<?> response = userController.createBooking(bookingData, userDetails);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(500.0, (Double) body.get("totalAmount"), 0.001);
        assertEquals(0.0, (Double) body.get("discountAmount"), 0.001);

        verify(bookingService).saveBooking(any(Booking.class));
    }

    @Test
    @DisplayName("POST /user/bookings with invalid coupon returns 400 Bad Request")
    void testCreateBooking_WithInvalidCoupon_Returns400BadRequest() {
        log.info("Testing booking creation with invalid coupon fails with 400");
        when(userDetails.getUsername()).thenReturn("customer@moviematic.com");
        when(userService.findByEmail("customer@moviematic.com")).thenReturn(sampleUser);
        when(showService.getShowById(1L)).thenReturn(sampleShow);

        when(couponService.validateCoupon("EXPIRED", 500.0))
                .thenThrow(new CouponException("Invalid or expired coupon code"));

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("showId", 1L);
        bookingData.put("seatNumbers", "A1,A2");
        bookingData.put("couponCode", "EXPIRED");

        ResponseEntity<?> response = userController.createBooking(bookingData, userDetails);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Invalid or expired coupon code", body.get("message"));
    }
}
