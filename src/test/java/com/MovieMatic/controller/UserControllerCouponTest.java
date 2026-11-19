package com.MovieMatic.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.MovieMatic.dto.CouponValidationResponse;
import com.MovieMatic.exception.CouponValidationException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserControllerCouponTest {

    private MockMvc mockMvc;

    @Mock
    private MovieService movieService;

    @Mock
    private ShowService showService;

    @Mock
    private UserService userService;

    @Mock
    private BookingService bookingService;

    @Mock
    private CouponService couponService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserController userController;

    private User dummyUser;
    private Show dummyShow;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.getParameterType().equals(UserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                                  org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                  org.springframework.web.context.request.NativeWebRequest webRequest,
                                                  org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return userDetails;
                    }
                })
                .build();

        dummyUser = new User();
        dummyUser.setId(10L);
        dummyUser.setEmail("user@example.com");
        dummyUser.setName("Test User");

        Movie dummyMovie = new Movie();
        dummyMovie.setId(1L);
        dummyMovie.setTitle("Avengers");

        dummyShow = new Show();
        dummyShow.setId(1L);
        dummyShow.setMovie(dummyMovie);
        dummyShow.setPrice(250.0);
        dummyShow.setScreen("Screen 1");
        dummyShow.setShowDate(LocalDate.now().plusDays(1));
        dummyShow.setShowTime(LocalTime.of(18, 30));
    }

    @Test
    void testValidateCouponValid() throws Exception {
        when(couponService.validateAndCalculate("SAVE20", 500.0))
                .thenReturn(new CouponValidationResponse(true, 100.0, 400.0));

        String requestBody = "{\"code\":\"SAVE20\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountAmount").value(100.0))
                .andExpect(jsonPath("$.finalAmount").value(400.0));
    }

    @Test
    void testValidateCouponInvalidOrExpired() throws Exception {
        when(couponService.validateAndCalculate("INVALID", 500.0))
                .thenThrow(new CouponValidationException("Invalid or expired coupon code"));

        String requestBody = "{\"code\":\"INVALID\",\"totalAmount\":500.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));
    }

    @Test
    void testValidateCouponBelowThreshold() throws Exception {
        when(couponService.validateAndCalculate("SAVE20", 200.0))
                .thenThrow(new CouponValidationException("Minimum booking amount of ₹300.00 required."));

        String requestBody = "{\"code\":\"SAVE20\",\"totalAmount\":200.0}";

        mockMvc.perform(post("/user/coupons/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Minimum booking amount of ₹300.00 required."));
    }

    @Test
    void testCreateBookingWithCoupon() throws Exception {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userService.findByEmail("user@example.com")).thenReturn(dummyUser);
        when(showService.getShowById(1L)).thenReturn(dummyShow);

        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 300.0,
                LocalDateTime.now().plusDays(2), 10, 0, true);
        coupon.setId(100L);

        when(couponService.validateCoupon("SAVE20", 500.0)).thenReturn(coupon);
        when(couponService.calculateDiscount(coupon, 500.0)).thenReturn(100.0);

        String requestBody = "{\"showId\":1,\"seatNumbers\":\"A1,A2\",\"couponCode\":\"SAVE20\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(400.0))
                .andExpect(jsonPath("$.discountAmount").value(100.0));

        verify(couponService).recordCouponUsage(coupon);
        verify(bookingService).saveBooking(any(Booking.class));
    }

    @Test
    void testCreateBookingWithoutCoupon() throws Exception {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userService.findByEmail("user@example.com")).thenReturn(dummyUser);
        when(showService.getShowById(1L)).thenReturn(dummyShow);

        String requestBody = "{\"showId\":1,\"seatNumbers\":\"A1,A2\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.discountAmount").value(0.0));

        verify(bookingService).saveBooking(any(Booking.class));
    }
}
