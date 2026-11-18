package com.MovieMatic.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserBookingCouponTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private ShowService showService;

    @Mock
    private BookingService bookingService;

    @Mock
    private CouponService couponService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserController userController;

    private User mockUser;
    private Show mockShow;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setEmail("user@example.com");
        mockUser.setName("Test User");

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");

        mockShow = new Show();
        mockShow.setId(1L);
        mockShow.setMovie(movie);
        mockShow.setPrice(250.0); // 2 seats = 500.0
    }

    @Test
    @DisplayName("POST /user/bookings - Success with valid coupon, server-side recalculation")
    void testCreateBookingWithCouponSuccess() throws Exception {
        Principal mockPrincipal = () -> "user@example.com";

        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0, null, 100, 0, true);
        coupon.setId(1L);

        // 2 seats * 250.0 = 500.0. 20% discount = 100.0, finalAmount = 400.0
        CouponValidationResponse validation = CouponValidationResponse.success(
                100.0, 400.0, "Coupon 'SAVE20' applied! You saved ₹100.00", coupon);
        when(couponService.validateAndCalculate(eq("SAVE20"), eq(500.0))).thenReturn(validation);

        doAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return null;
        }).when(bookingService).saveBooking(any(Booking.class));

        String requestJson = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\", \"couponCode\": \"SAVE20\"}";

        mockMvc.perform(post("/user/bookings")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(400.0))
                .andExpect(jsonPath("$.discountAmount").value(100.0));

        verify(couponService).incrementUsedCount(coupon);
        verify(bookingService).saveBooking(any(Booking.class));
    }

    @Test
    @DisplayName("POST /user/bookings - Rejection when coupon is invalid or expired")
    void testCreateBookingWithInvalidCoupon() throws Exception {
        Principal mockPrincipal = () -> "user@example.com";

        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        CouponValidationResponse invalidValidation = CouponValidationResponse.error("Invalid or expired coupon code");
        when(couponService.validateAndCalculate(eq("EXPIRED"), eq(500.0))).thenReturn(invalidValidation);

        String requestJson = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\", \"couponCode\": \"EXPIRED\"}";

        mockMvc.perform(post("/user/bookings")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    @DisplayName("POST /user/bookings - Booking without coupon applies 0.0 discount and full total")
    void testCreateBookingWithoutCoupon() throws Exception {
        Principal mockPrincipal = () -> "user@example.com";

        when(userService.findByEmail("user@example.com")).thenReturn(mockUser);
        when(showService.getShowById(1L)).thenReturn(mockShow);

        doAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(5L);
            return null;
        }).when(bookingService).saveBooking(any(Booking.class));

        String requestJson = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\"}";

        mockMvc.perform(post("/user/bookings")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(5))
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.discountAmount").value(0.0));
    }
}
