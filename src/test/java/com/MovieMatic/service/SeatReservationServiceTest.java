package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.MovieMatic.dto.HoldSeatResponse;
import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.exception.SeatAlreadyOccupiedException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;

@ExtendWith(MockitoExtension.class)
public class SeatReservationServiceTest {

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private SeatReservationService seatReservationService;

    private Show testShow;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        testShow = new Show();
        testShow.setId(1L);
        testShow.setPrice(200.0);

        user1 = new User();
        user1.setId(101L);
        user1.setName("User One");
        user1.setEmail("user1@example.com");

        user2 = new User();
        user2.setId(102L);
        user2.setName("User Two");
        user2.setEmail("user2@example.com");
    }

    @Test
    @DisplayName("Successfully hold available seats for 10 minutes")
    void testHoldSeats_Success() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(showSeatRepository.findByShowIdAndSeatNumberIn(any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());
        when(showSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        HoldSeatResponse response = seatReservationService.holdSeats(1L, "A1,A2", user1.getId());

        assertNotNull(response);
        assertEquals("HELD", response.getStatus());
        assertEquals(600, response.getExpiresInSeconds());
        verify(showSeatRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Atomic Reservation: If one seat is already held, no seats are reserved")
    void testHoldSeats_AtomicReservation_Rollback() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));

        // Seat A2 is already held by user2 within the last 5 minutes
        ShowSeat heldSeatA2 = new ShowSeat(testShow, "A2", SeatStatus.HELD, LocalDateTime.now().minusMinutes(2), user2.getId());
        when(showSeatRepository.findByShowIdAndSeatNumberIn(any(), any())).thenReturn(Collections.singletonList(heldSeatA2));
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());

        // User 1 requests A1 and A2
        SeatAlreadyOccupiedException ex = assertThrows(SeatAlreadyOccupiedException.class, () -> {
            seatReservationService.holdSeats(1L, "A1,A2", user1.getId());
        });

        assertEquals("Seats already held or booked", ex.getMessage());
    }

    @Test
    @DisplayName("Single Owner: Only user who placed the hold can complete booking purchase")
    void testConfirmBooking_SingleOwnerRule() {
        ShowSeat heldA1 = new ShowSeat(testShow, "A1", SeatStatus.HELD, LocalDateTime.now().minusMinutes(2), user1.getId());
        when(showSeatRepository.findByShowIdAndSeatNumberIn(any(), any())).thenReturn(Collections.singletonList(heldA1));
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());

        // User 2 attempts to confirm booking on seat held by User 1 -> MUST FAIL
        SeatAlreadyOccupiedException ex = assertThrows(SeatAlreadyOccupiedException.class, () -> {
            seatReservationService.confirmBooking(testShow, "A1", user2);
        });

        assertTrue(ex.getMessage().contains("reserved by another customer"));

        // User 1 confirms booking -> MUST SUCCEED
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(500L);
            return b;
        });

        Booking confirmed = seatReservationService.confirmBooking(testShow, "A1", user1);
        assertNotNull(confirmed);
        assertEquals(500L, confirmed.getId());
        assertEquals(SeatStatus.BOOKED, heldA1.getStatus());
    }

    @Test
    @DisplayName("Hold Expiration: Holds older than 10 minutes are expired and can be re-reserved")
    void testHoldExpiration_ReReserveAfterTTL() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));

        // Seat A1 was held 11 minutes ago by User 2 -> Expired
        ShowSeat expiredSeat = new ShowSeat(testShow, "A1", SeatStatus.HELD, LocalDateTime.now().minusMinutes(11), user2.getId());
        when(showSeatRepository.findByShowIdAndSeatNumberIn(any(), any())).thenReturn(Collections.singletonList(expiredSeat));
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());
        when(showSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // User 1 should now be able to hold seat A1
        HoldSeatResponse response = seatReservationService.holdSeats(1L, "A1", user1.getId());
        assertNotNull(response);
        assertEquals("HELD", response.getStatus());
        assertEquals(user1.getId(), expiredSeat.getHeldByUserId());
    }

    @Test
    @DisplayName("Scheduled Cleanup: Releases expired seats back to AVAILABLE")
    void testScheduledAutoReleaseExpiredHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        ShowSeat expired1 = new ShowSeat(testShow, "C1", SeatStatus.HELD, cutoff.minusMinutes(1), 101L);
        ShowSeat expired2 = new ShowSeat(testShow, "C2", SeatStatus.HELD, cutoff.minusMinutes(5), 102L);

        when(showSeatRepository.findByStatusAndHeldAtBefore(any(), any())).thenReturn(List.of(expired1, expired2));

        seatReservationService.autoReleaseExpiredHolds();

        assertEquals(SeatStatus.AVAILABLE, expired1.getStatus());
        assertEquals(SeatStatus.AVAILABLE, expired2.getStatus());
        verify(showSeatRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Occupied Seats: Correctly splits booked and held seats into response DTO")
    void testGetOccupiedSeats() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));

        ShowSeat bookedSeat = new ShowSeat(testShow, "B1", SeatStatus.BOOKED, null, 101L);
        ShowSeat activeHeldSeat = new ShowSeat(testShow, "A1", SeatStatus.HELD, LocalDateTime.now().minusMinutes(3), 102L);
        ShowSeat expiredHeldSeat = new ShowSeat(testShow, "D1", SeatStatus.HELD, LocalDateTime.now().minusMinutes(15), 103L);

        when(showSeatRepository.findByShowId(1L)).thenReturn(List.of(bookedSeat, activeHeldSeat, expiredHeldSeat));
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());

        OccupiedSeatsResponse response = seatReservationService.getOccupiedSeats(1L);

        assertNotNull(response);
        assertTrue(response.getBookedSeats().contains("B1"));
        assertTrue(response.getHeldSeats().contains("A1"));
        assertFalse(response.getHeldSeats().contains("D1"), "Expired hold should not be listed in active held seats");
    }

    @Test
    @DisplayName("Concurrency: Simultaneous hold requests by 2 users on the same seat - exactly one succeeds")
    void testConcurrency_SimultaneousHold() throws Exception {
        when(showRepository.findById(1L)).thenReturn(Optional.of(testShow));
        when(bookingRepository.findByShow(testShow)).thenReturn(Collections.emptyList());

        // In-memory seat state shared between threads
        List<ShowSeat> simulatedDbSeats = Collections.synchronizedList(new ArrayList<>());

        when(showSeatRepository.findByShowIdAndSeatNumberIn(any(), any())).thenAnswer(inv -> {
            synchronized (simulatedDbSeats) {
                return new ArrayList<>(simulatedDbSeats);
            }
        });

        when(showSeatRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<ShowSeat> saved = inv.getArgument(0);
            synchronized (simulatedDbSeats) {
                for (ShowSeat s : saved) {
                    simulatedDbSeats.removeIf(existing -> existing.getSeatNumber().equals(s.getSeatNumber()));
                    simulatedDbSeats.add(s);
                }
            }
            return saved;
        });

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Runnable user1Task = () -> {
            try {
                startGate.await();
                seatReservationService.holdSeats(1L, "E5", user1.getId());
                successCount.incrementAndGet();
            } catch (SeatAlreadyOccupiedException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runnable user2Task = () -> {
            try {
                startGate.await();
                seatReservationService.holdSeats(1L, "E5", user2.getId());
                successCount.incrementAndGet();
            } catch (SeatAlreadyOccupiedException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Future<?> f1 = executor.submit(user1Task);
        Future<?> f2 = executor.submit(user2Task);

        // Release both threads simultaneously
        startGate.countDown();

        f1.get();
        f2.get();
        executor.shutdown();

        // Exactly one user must succeed, and the other must receive a conflict exception
        assertEquals(1, successCount.get(), "Exactly one user must successfully hold seat E5");
        assertEquals(1, conflictCount.get(), "The other user must receive a SeatAlreadyOccupiedException");
    }
}
