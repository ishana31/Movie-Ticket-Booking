package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MovieMatic.dto.CancellationPreviewResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Show show;
    private Booking booking;
    private LocalDateTime baseShowDateTime;

    @BeforeEach
    void setUp() {
        user = new User("John Doe", "john@example.com", "password", "USER");
        user.setId(1L);

        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");

        baseShowDateTime = LocalDateTime.of(2025, 12, 1, 18, 0);

        show = new Show();
        show.setId(1L);
        show.setMovie(movie);
        show.setShowDate(baseShowDateTime.toLocalDate());
        show.setShowTime(baseShowDateTime.toLocalTime());
        show.setPrice(100.0);
        show.setScreen("Screen 1");

        booking = new Booking();
        booking.setId(100L);
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeatNumbers("A1,A2");
        booking.setTotalAmount(200.0);
        booking.setStatus("CONFIRMED");
        booking.setBookingTime(LocalDateTime.of(2025, 11, 20, 10, 0));
    }

    @Test
    void testPreviewMoreThan4HoursBeforeShow_90PercentRefund() {
        // Request cancellation 5 hours before show (13:00 for 18:00 show)
        LocalDateTime requestTime = baseShowDateTime.minusHours(5);

        CancellationPreviewResponse preview = bookingService.getCancellationPreview(booking, requestTime);

        assertNotNull(preview);
        assertEquals(200.0, preview.getTotalPaid());
        assertEquals(90, preview.getRefundPercentage());
        assertEquals(180.0, preview.getRefundAmount());
        assertEquals(20.0, preview.getCancellationFee());
    }

    @Test
    void testPreviewBetween1And4HoursBeforeShow_50PercentRefund() {
        // Request cancellation 2.5 hours before show (15:30 for 18:00 show)
        LocalDateTime requestTime = baseShowDateTime.minusHours(2).minusMinutes(30);

        CancellationPreviewResponse preview = bookingService.getCancellationPreview(booking, requestTime);

        assertNotNull(preview);
        assertEquals(200.0, preview.getTotalPaid());
        assertEquals(50, preview.getRefundPercentage());
        assertEquals(100.0, preview.getRefundAmount());
        assertEquals(100.0, preview.getCancellationFee());
    }

    @Test
    void testPreviewLessThan1HourBeforeShow_ThrowsIllegalArgumentException() {
        // Request cancellation 45 minutes before show (17:15 for 18:00 show)
        LocalDateTime requestTime = baseShowDateTime.minusMinutes(45);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.getCancellationPreview(booking, requestTime);
        });
        assertTrue(exception.getMessage().contains("within 1 hour"));
    }

    @Test
    void testPreviewShowAlreadyPassed_ThrowsIllegalArgumentException() {
        // Request cancellation 10 minutes after show start
        LocalDateTime requestTime = baseShowDateTime.plusMinutes(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.getCancellationPreview(booking, requestTime);
        });
        assertTrue(exception.getMessage().contains("already started or passed"));
    }

    @Test
    void testPreviewAlreadyCancelled_ThrowsIllegalStateException() {
        booking.setStatus("CANCELLED");
        LocalDateTime requestTime = baseShowDateTime.minusHours(5);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.getCancellationPreview(booking, requestTime);
        });
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    void testCancelBookingMoreThan4HoursBeforeShow_Success() {
        LocalDateTime cancelTime = baseShowDateTime.minusHours(5);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(booking, cancelTime);

        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(cancelTime, cancelled.getCancelledAt());
        assertEquals(180.0, cancelled.getRefundAmount());
        assertEquals(20.0, cancelled.getCancellationFee());
        assertEquals("REFUNDED", cancelled.getRefundStatus());
    }

    @Test
    void testCancelBookingBetween1And4HoursBeforeShow_Success() {
        LocalDateTime cancelTime = baseShowDateTime.minusHours(3);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(booking, cancelTime);

        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(cancelTime, cancelled.getCancelledAt());
        assertEquals(100.0, cancelled.getRefundAmount());
        assertEquals(100.0, cancelled.getCancellationFee());
        assertEquals("REFUNDED", cancelled.getRefundStatus());
    }

    @Test
    void testCancelBookingLessThan1HourBeforeShow_Rejected() {
        LocalDateTime cancelTime = baseShowDateTime.minusMinutes(30);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking(booking, cancelTime);
        });
        assertTrue(exception.getMessage().contains("within 1 hour"));
    }

    @Test
    void testCancelBookingAlreadyCancelled_ThrowsIllegalStateException() {
        booking.setStatus("CANCELLED");
        LocalDateTime cancelTime = baseShowDateTime.minusHours(5);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            bookingService.cancelBooking(booking, cancelTime);
        });
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    void testSeatRelease_OccupiedSeatsExcludesCancelledBookings() {
        Booking booking1 = new Booking();
        booking1.setId(101L);
        booking1.setShow(show);
        booking1.setSeatNumbers("A1,A2");
        booking1.setStatus("CONFIRMED");

        Booking booking2 = new Booking();
        booking2.setId(102L);
        booking2.setShow(show);
        booking2.setSeatNumbers("B1,B2");
        booking2.setStatus("CONFIRMED");

        List<Booking> showBookings = new ArrayList<>();
        showBookings.add(booking1);
        showBookings.add(booking2);

        when(bookingRepository.findByShow(show)).thenReturn(showBookings);

        List<String> occupied = bookingService.getOccupiedSeats(show);
        assertEquals(4, occupied.size());
        assertTrue(occupied.contains("A1"));
        assertTrue(occupied.contains("A2"));
        assertTrue(occupied.contains("B1"));
        assertTrue(occupied.contains("B2"));

        // Now cancel booking1
        booking1.setStatus("CANCELLED");

        List<String> occupiedAfterCancel = bookingService.getOccupiedSeats(show);
        assertEquals(2, occupiedAfterCancel.size());
        assertFalse(occupiedAfterCancel.contains("A1"));
        assertFalse(occupiedAfterCancel.contains("A2"));
        assertTrue(occupiedAfterCancel.contains("B1"));
        assertTrue(occupiedAfterCancel.contains("B2"));
    }
}
