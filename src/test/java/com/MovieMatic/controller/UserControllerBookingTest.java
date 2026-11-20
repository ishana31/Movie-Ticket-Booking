package com.MovieMatic.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.MovieMatic.dto.CouponValidationResult;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.CouponService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@ExtendWith(MockitoExtension.class)
public class UserControllerBookingTest {

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
    private EmailService emailService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private Show testShow;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("user@example.com");
        testUser.setName("Test User");

        testShow = new Show();
        testShow.setId(1L);
        testShow.setPrice(250.0);

        // Custom argument resolver for @AuthenticationPrincipal UserDetails
        HandlerMethodArgumentResolver userDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return org.springframework.security.core.userdetails.User
                        .withUsername("user@example.com")
                        .password("password")
                        .roles("USER")
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(userDetailsResolver)
                .build();
    }

    @Test
    void testCreateBooking_WithValidCoupon_Success() throws Exception {
        when(userService.findByEmail("user@example.com")).thenReturn(testUser);
        when(showService.getShowById(1L)).thenReturn(testShow);

        Coupon coupon = new Coupon("SAVE20", DiscountType.PERCENTAGE, 20.0, 0.0, null, 10, true);
        CouponValidationResult validResult = CouponValidationResult.valid(coupon, 100.0, 400.0);

        when(couponService.validateCoupon(eq("SAVE20"), eq(500.0)))
                .thenReturn(validResult);

        doAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(1L);
            return null;
        }).when(bookingService).saveBooking(any(Booking.class));

        String requestBody = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\", \"couponCode\": \"SAVE20\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(400.0))
                .andExpect(jsonPath("$.discountAmount").value(100.0));

        verify(couponService).incrementUsedCount(coupon);
        verify(bookingService).saveBooking(any(Booking.class));
    }

    @Test
    void testCreateBooking_WithoutCoupon_Success() throws Exception {
        when(userService.findByEmail("user@example.com")).thenReturn(testUser);
        when(showService.getShowById(1L)).thenReturn(testShow);

        doAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(2L);
            return null;
        }).when(bookingService).saveBooking(any(Booking.class));

        String requestBody = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(2))
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.discountAmount").value(0.0));

        verify(couponService, never()).validateCoupon(any(), anyDouble());
        verify(couponService, never()).incrementUsedCount(any());
        verify(bookingService).saveBooking(any(Booking.class));
    }

    @Test
    void testCreateBooking_WithInvalidCoupon_ReturnsBadRequest() throws Exception {
        when(userService.findByEmail("user@example.com")).thenReturn(testUser);
        when(showService.getShowById(1L)).thenReturn(testShow);

        CouponValidationResult invalidResult = CouponValidationResult.invalid("Invalid or expired coupon code");
        when(couponService.validateCoupon(eq("INVALID"), eq(500.0)))
                .thenReturn(invalidResult);

        String requestBody = "{\"showId\": 1, \"seatNumbers\": \"A1,A2\", \"couponCode\": \"INVALID\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired coupon code"));

        verify(bookingService, never()).saveBooking(any());
    }
}
