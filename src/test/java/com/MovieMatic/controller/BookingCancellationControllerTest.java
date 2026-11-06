package com.MovieMatic.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.MovieMatic.dto.CancellationPreviewResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
public class BookingCancellationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private UserService userService;

    @MockBean
    private ShowService showService;

    @MockBean
    private MovieService movieService;

    @MockBean
    private EmailService emailService;

    private User ownerUser;
    private User otherUser;
    private Show showFuture;
    private Show showNear;
    private Booking validBookingFuture;
    private Booking validBookingNear;
    private Booking alreadyCancelledBooking;

    @BeforeEach
    void setUp() {
        ownerUser = new User("Owner User", "owner@example.com", "pass", "USER");
        ownerUser.setId(10L);

        otherUser = new User("Other User", "other@example.com", "pass", "USER");
        otherUser.setId(20L);

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");

        // Show 2 days in the future (> 4 hours)
        showFuture = new Show();
        showFuture.setId(100L);
        showFuture.setMovie(movie);
        showFuture.setShowDate(LocalDate.now().plusDays(2));
        showFuture.setShowTime(LocalTime.of(20, 0));
        showFuture.setPrice(150.0);
        showFuture.setScreen("Screen 1");

        // Show 30 minutes in the future (< 1 hour)
        LocalDateTime nearTime = LocalDateTime.now().plusMinutes(30);
        showNear = new Show();
        showNear.setId(200L);
        showNear.setMovie(movie);
        showNear.setShowDate(nearTime.toLocalDate());
        showNear.setShowTime(nearTime.toLocalTime());
        showNear.setPrice(150.0);
        showNear.setScreen("Screen 2");

        validBookingFuture = new Booking();
        validBookingFuture.setId(1L);
        validBookingFuture.setUser(ownerUser);
        validBookingFuture.setShow(showFuture);
        validBookingFuture.setSeatNumbers("C1,C2");
        validBookingFuture.setTotalAmount(300.0);
        validBookingFuture.setStatus("CONFIRMED");

        validBookingNear = new Booking();
        validBookingNear.setId(2L);
        validBookingNear.setUser(ownerUser);
        validBookingNear.setShow(showNear);
        validBookingNear.setSeatNumbers("D1,D2");
        validBookingNear.setTotalAmount(300.0);
        validBookingNear.setStatus("CONFIRMED");

        alreadyCancelledBooking = new Booking();
        alreadyCancelledBooking.setId(3L);
        alreadyCancelledBooking.setUser(ownerUser);
        alreadyCancelledBooking.setShow(showFuture);
        alreadyCancelledBooking.setSeatNumbers("E1");
        alreadyCancelledBooking.setTotalAmount(150.0);
        alreadyCancelledBooking.setStatus("CANCELLED");
        alreadyCancelledBooking.setCancelledAt(LocalDateTime.now().minusHours(1));

        when(userService.findByEmail("owner@example.com")).thenReturn(ownerUser);
        when(userService.findByEmail("other@example.com")).thenReturn(otherUser);
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testPreviewMoreThan4Hours_Returns200With90PercentRefund() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(validBookingFuture);
        when(bookingService.getCancellationPreview(eq(validBookingFuture), any()))
            .thenReturn(new CancellationPreviewResponse(300.0, 90, 270.0, 30.0));

        mockMvc.perform(get("/user/bookings/1/cancellation-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaid", is(300.0)))
                .andExpect(jsonPath("$.refundPercentage", is(90)))
                .andExpect(jsonPath("$.refundAmount", is(270.0)))
                .andExpect(jsonPath("$.cancellationFee", is(30.0)));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testPreviewBetween1And4Hours_Returns200With50PercentRefund() throws Exception {
        // Show 2 hours in the future
        LocalDateTime midTime = LocalDateTime.now().plusHours(2);
        Show showMid = new Show();
        showMid.setId(300L);
        showMid.setShowDate(midTime.toLocalDate());
        showMid.setShowTime(midTime.toLocalTime());
        showMid.setPrice(100.0);

        Booking bookingMid = new Booking();
        bookingMid.setId(4L);
        bookingMid.setUser(ownerUser);
        bookingMid.setShow(showMid);
        bookingMid.setSeatNumbers("A1");
        bookingMid.setTotalAmount(100.0);
        bookingMid.setStatus("CONFIRMED");

        when(bookingService.getBookingById(4L)).thenReturn(bookingMid);
        when(bookingService.getCancellationPreview(eq(bookingMid), any()))
            .thenReturn(new CancellationPreviewResponse(100.0, 50, 50.0, 50.0));

        mockMvc.perform(get("/user/bookings/4/cancellation-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaid", is(100.0)))
                .andExpect(jsonPath("$.refundPercentage", is(50)))
                .andExpect(jsonPath("$.refundAmount", is(50.0)))
                .andExpect(jsonPath("$.cancellationFee", is(50.0)));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testPreviewAlreadyCancelledBooking_Returns400BadRequest() throws Exception {
        when(bookingService.getBookingById(3L)).thenReturn(alreadyCancelledBooking);

        mockMvc.perform(get("/user/bookings/3/cancellation-preview"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "USER")
    void testPreviewDifferentUserBooking_Returns403Forbidden() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(validBookingFuture);

        mockMvc.perform(get("/user/bookings/1/cancellation-preview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testPreviewNonExistentBooking_Returns404NotFound() throws Exception {
        when(bookingService.getBookingById(999L)).thenReturn(null);

        mockMvc.perform(get("/user/bookings/999/cancellation-preview"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testCancelBookingMoreThan4Hours_Returns200WithDetails() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(validBookingFuture);

        Booking cancelledBooking = new Booking();
        cancelledBooking.setId(1L);
        cancelledBooking.setUser(ownerUser);
        cancelledBooking.setShow(showFuture);
        cancelledBooking.setSeatNumbers("C1,C2");
        cancelledBooking.setTotalAmount(300.0);
        cancelledBooking.setStatus("CANCELLED");
        cancelledBooking.setCancelledAt(LocalDateTime.now());
        cancelledBooking.setRefundAmount(270.0);
        cancelledBooking.setCancellationFee(30.0);
        cancelledBooking.setRefundStatus("REFUNDED");

        when(bookingService.cancelBooking(eq(validBookingFuture), any())).thenReturn(cancelledBooking);

        mockMvc.perform(post("/user/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.refundAmount", is(270.0)))
                .andExpect(jsonPath("$.cancellationFee", is(30.0)))
                .andExpect(jsonPath("$.refundStatus", is("REFUNDED")));
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testCancelLessThan1HourBeforeShow_Returns400BadRequest() throws Exception {
        when(bookingService.getBookingById(2L)).thenReturn(validBookingNear);

        mockMvc.perform(post("/user/bookings/2/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "USER")
    void testCancelDifferentUserBooking_Returns403Forbidden() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(validBookingFuture);

        mockMvc.perform(post("/user/bookings/1/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testCancelAlreadyCancelledBooking_Returns409Conflict() throws Exception {
        when(bookingService.getBookingById(3L)).thenReturn(alreadyCancelledBooking);

        mockMvc.perform(post("/user/bookings/3/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "USER")
    void testSeatReleaseVerification() throws Exception {
        when(showService.getShowById(100L)).thenReturn(showFuture);

        // Before cancellation: seats C1 and C2 are occupied
        when(bookingService.getOccupiedSeats(showFuture)).thenReturn(List.of("C1", "C2"));

        mockMvc.perform(get("/user/shows/100/occupied-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder("C1", "C2")));

        // After cancellation: seats C1 and C2 are released
        when(bookingService.getOccupiedSeats(showFuture)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/user/shows/100/occupied-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(containsInAnyOrder("C1", "C2"))));
    }
}
