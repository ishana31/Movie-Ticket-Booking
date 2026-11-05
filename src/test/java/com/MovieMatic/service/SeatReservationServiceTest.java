package com.MovieMatic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.dto.SeatHoldResponse;
import com.MovieMatic.exception.SeatUnavailableException;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.MovieRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;

@SpringBootTest
public class SeatReservationServiceTest {

    @Autowired
    private SeatReservationService seatReservationService;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Show testShow;

    @BeforeEach
    void setUp() {
        showSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showRepository.deleteAll();
        movieRepository.deleteAll();

        Movie movie = new Movie();
        movie.setTitle("Interstellar");
        movie.setGenre("Sci-Fi");
        movie.setDuration(169);
        movie.setReleaseDate(LocalDate.now());
        movieRepository.save(movie);

        Show show = new Show();
        show.setMovie(movie);
        show.setShowDate(LocalDate.now());
        show.setShowTime(LocalTime.of(19, 30));
        show.setScreen("Screen 1");
        show.setPrice(250.0);
        testShow = showRepository.save(show);
    }

    @Test
    @DisplayName("Hold seats successfully when seats are available")
    void testHoldSeatsSuccess() {
        SeatHoldResponse response = seatReservationService.holdSeats(testShow.getId(), Arrays.asList("A1", "A2"), 1L);

        assertNotNull(response);
        assertEquals("HELD", response.getStatus());
        assertEquals(600, response.getExpiresInSeconds());

        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertEquals(2, occupied.getHeldSeats().size());
        assertTrue(occupied.getHeldSeats().contains("A1"));
        assertTrue(occupied.getHeldSeats().contains("A2"));
        assertTrue(occupied.getBookedSeats().isEmpty());
    }

    @Test
    @DisplayName("Atomic Reservation: If one seat is unavailable, none are held")
    void testAtomicReservation() {
        // User 1 holds A1
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("A1"), 1L);

        // User 2 tries to hold A1 and A2 together
        assertThrows(SeatUnavailableException.class, () -> {
            seatReservationService.holdSeats(testShow.getId(), Arrays.asList("A1", "A2"), 2L);
        });

        // A2 must NOT have been held (atomic roll back / no partial hold)
        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertEquals(1, occupied.getHeldSeats().size());
        assertTrue(occupied.getHeldSeats().contains("A1"));
        assertFalse(occupied.getHeldSeats().contains("A2"));
    }

    @Test
    @DisplayName("Concurrency Test: Two simulated users attempting to reserve the same seat simultaneously")
    void testConcurrentHoldSameSeat() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= threads; i++) {
            final long userId = (long) i;
            futures.add(executor.submit(() -> {
                try {
                    startGate.await(); // ensure simultaneous execution
                    seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("B1"), userId);
                    successCount.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            }));
        }

        startGate.countDown(); // trigger both threads at the exact same moment
        endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one user must succeed, and the other must receive conflict
        assertEquals(1, successCount.get(), "Exactly one user should succeed in holding the seat");
        assertEquals(1, conflictCount.get(), "The other user should receive a conflict exception");

        // Verify only 1 ShowSeat record exists for B1
        List<ShowSeat> seats = showSeatRepository.findByShowIdAndSeatNumberIn(testShow.getId(), Collections.singletonList("B1"));
        assertEquals(1, seats.size());
        assertEquals(SeatStatus.HELD, seats.get(0).getStatus());
    }

    @Test
    @DisplayName("Single Owner: Only the user who placed the hold can complete the purchase")
    void testSingleOwnerRule() {
        seatReservationService.holdSeats(testShow.getId(), Arrays.asList("C1", "C2"), 1L);

        // User 2 attempts to confirm booking for seats held by User 1 -> should fail
        assertThrows(SeatUnavailableException.class, () -> {
            seatReservationService.confirmBooking(testShow.getId(), Arrays.asList("C1", "C2"), 2L);
        });

        // User 1 confirms booking -> should succeed
        seatReservationService.confirmBooking(testShow.getId(), Arrays.asList("C1", "C2"), 1L);

        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertEquals(2, occupied.getBookedSeats().size());
        assertTrue(occupied.getBookedSeats().contains("C1"));
        assertTrue(occupied.getBookedSeats().contains("C2"));
        assertTrue(occupied.getHeldSeats().isEmpty());
    }

    @Test
    @DisplayName("Timeout (TTL) and Auto-Release: Expired holds are released back to AVAILABLE")
    void testHoldTimeoutAndAutoRelease() {
        // Manually create an expired hold (> 10 minutes ago)
        ShowSeat expiredSeat = new ShowSeat();
        expiredSeat.setShow(testShow);
        expiredSeat.setSeatNumber("D1");
        expiredSeat.setStatus(SeatStatus.HELD);
        expiredSeat.setHeldAt(LocalDateTime.now().minusMinutes(11));
        expiredSeat.setHeldByUserId(99L);
        showSeatRepository.save(expiredSeat);

        // Before cleanup, occupied seats should NOT include D1 as active hold
        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertFalse(occupied.getHeldSeats().contains("D1"), "Expired hold should not be listed as actively held");

        // Another user can now hold D1
        SeatHoldResponse response = seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("D1"), 100L);
        assertEquals("HELD", response.getStatus());

        // Test auto-release task
        ShowSeat anotherExpired = new ShowSeat();
        anotherExpired.setShow(testShow);
        anotherExpired.setSeatNumber("E1");
        anotherExpired.setStatus(SeatStatus.HELD);
        anotherExpired.setHeldAt(LocalDateTime.now().minusMinutes(12));
        anotherExpired.setHeldByUserId(98L);
        showSeatRepository.save(anotherExpired);

        seatReservationService.releaseExpiredHolds();

        ShowSeat releasedSeat = showSeatRepository.findByShowIdAndSeatNumber(testShow.getId(), "E1").orElse(null);
        assertNotNull(releasedSeat);
        assertEquals(SeatStatus.AVAILABLE, releasedSeat.getStatus());
    }
}
