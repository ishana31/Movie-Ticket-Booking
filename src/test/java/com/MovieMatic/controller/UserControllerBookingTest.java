package com.MovieMatic.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.MovieMatic.dto.BookingResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Unit test verifying server-side price recalculation and booking confirmation
 * with promotional coupon codes in {@link UserController}.
 *
 * @author CodeFactoryAgent
 */
@ExtendWith(MockitoExtension.class)
class UserControllerBookingTest {

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

    private User testUser;
    private Show testShow;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");

        testShow = new Show();
        testShow.setId(1L);
        testShow.setMovie(movie);
        testShow.setPrice(250.0); // 2 seats * 250 = ₹500
        testShow.setScreen("Screen 1");
    }

    @Test
    @DisplayName("createBooking recalculates discount server-side and returns BookingResponse")
    void shouldCreateBookingWithRecalculatedCouponDiscount() {
        // Given user and show
        when(userDetails.getUsername()).thenReturn("john@example.com");
        when(userService.findByEmail("john@example.com")).thenReturn(testUser);
        when(showService.getShowById(1L)).thenReturn(testShow);

        // Coupon redemption result for SAVE20 (20% on 500 = 100 off, final 400)
        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0, null, 100, true);
        CouponService.CouponRedemptionResult redemptionResult =
                new CouponService.CouponRedemptionResult(coupon, 100.0, 400.0);
        when(couponService.applyCouponForBooking(eq("SAVE20"), eq(500.0)))
                .thenReturn(redemptionResult);

        doAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return null;
        }).when(bookingService).saveBooking(any(Booking.class));

        // When
        Map<String, Object> request = new HashMap<>();
        request.put("showId", 1);
        request.put("seatNumbers", "A1,A2");
        request.put("couponCode", "SAVE20");

        ResponseEntity<?> responseEntity = userController.createBooking(request, userDetails);

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        BookingResponse bookingResponse = (BookingResponse) responseEntity.getBody();
        assertEquals(1L, bookingResponse.getBookingId());
        assertEquals(400.0, bookingResponse.getTotalAmount(), 0.001);
        assertEquals(100.0, bookingResponse.getDiscountAmount(), 0.001);
    }
}
