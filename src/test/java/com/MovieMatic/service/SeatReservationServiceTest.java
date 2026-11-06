package com.MovieMatic.service;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.dto.SeatHoldResponse;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.MovieRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;
import com.MovieMatic.repository.UserRepository;

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
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Show testShow;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        showSeatRepository.deleteAll();
        bookingRepository.deleteAll();

        // Create or find test users
        testUser1 = userRepository.findByEmail("testuser1@example.com");
        if (testUser1 == null) {
            testUser1 = new User("User One", "testuser1@example.com", "password", "USER");
            testUser1 = userRepository.save(testUser1);
        }

        testUser2 = userRepository.findByEmail("testuser2@example.com");
        if (testUser2 == null) {
            testUser2 = new User("User Two", "testuser2@example.com", "password", "USER");
            testUser2 = userRepository.save(testUser2);
        }

        // Create test movie
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setGenre("Action");
        movie.setDuration(120);
        movie = movieRepository.save(movie);

        // Create test show
        testShow = new Show();
        testShow.setMovie(movie);
        testShow.setShowDate(LocalDate.now().plusDays(1));
        testShow.setShowTime(LocalTime.of(18, 0));
        testShow.setScreen("Screen 1");
        testShow.setPrice(250.0);
        testShow = showRepository.save(testShow);
    }

    @Test
    @DisplayName("Should successfully hold available seats with 600s TTL")
    void testHoldSeatsSuccessfully() {
        List<String> seats = Arrays.asList("A1", "A2");
        SeatHoldResponse response = seatReservationService.holdSeats(testShow.getId(), seats, testUser1.getId());

        assertNotNull(response);
        assertEquals("HELD", response.getStatus());
        assertEquals(600, response.getExpiresInSeconds());

        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertTrue(occupied.getHeldSeats().contains("A1"));
        assertTrue(occupied.getHeldSeats().contains("A2"));
        assertFalse(occupied.getBookedSeats().contains("A1"));
    }

    @Test
    @DisplayName("Atomic Reservation: If one seat is already held, none are held")
    void testAtomicReservationFailure() {
        // User 1 holds A1
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("A1"), testUser1.getId());

        // User 2 tries to hold A1 and A2
        assertThrows(SeatConflictException.class, () -> {
            seatReservationService.holdSeats(testShow.getId(), Arrays.asList("A1", "A2"), testUser2.getId());
        });

        // Verify A2 remains free (not held by anyone)
        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertTrue(occupied.getHeldSeats().contains("A1"));
        assertFalse(occupied.getHeldSeats().contains("A2"));
    }

    @Test
    @DisplayName("Concurrency Test: Two or more concurrent threads attempt to hold the same seat simultaneously")
    void testConcurrentSeatHoldSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<String> requestedSeats = Collections.singletonList("B5");

        for (int i = 0; i < threadCount; i++) {
            final long simUserId = (long) (1000 + i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure simultaneous execution
                    seatReservationService.holdSeats(testShow.getId(), requestedSeats, simUserId);
                    successCount.incrementAndGet();
                } catch (SeatConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Fire all threads together
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Exactly one thread must have succeeded, all others must receive conflict
        assertEquals(1, successCount.get(), "Exactly one user should succeed in holding seat B5");
        assertEquals(threadCount - 1, conflictCount.get(), "All other concurrent users should receive conflict");

        // Verify state
        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertTrue(occupied.getHeldSeats().contains("B5"));
    }

    @Test
    @DisplayName("Single Owner: Only user who placed the hold can complete purchase")
    void testSingleOwnerRule() {
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("C1"), testUser1.getId());

        // User 2 attempts to confirm booking for User 1's held seat -> must fail
        assertThrows(SeatConflictException.class, () -> {
            seatReservationService.confirmBooking(testShow.getId(), Collections.singletonList("C1"), testUser2.getId());
        });

        // User 1 confirms booking -> must succeed
        seatReservationService.confirmBooking(testShow.getId(), Collections.singletonList("C1"), testUser1.getId());

        OccupiedSeatsResponse occupied = seatReservationService.getOccupiedSeats(testShow.getId());
        assertTrue(occupied.getBookedSeats().contains("C1"));
        assertFalse(occupied.getHeldSeats().contains("C1"));
    }

    @Test
    @DisplayName("Hold Expiration: Expired holds are released and become available for other users")
    void testHoldExpirationAndAutoRelease() {
        // Create an expired hold (older than 10 minutes)
        ShowSeat expiredSeat = new ShowSeat();
        expiredSeat.setShow(testShow);
        expiredSeat.setSeatNumber("D4");
        expiredSeat.setStatus(SeatStatus.HELD);
        expiredSeat.setHeldAt(LocalDateTime.now().minusMinutes(11));
        expiredSeat.setHeldByUserId(testUser1.getId());
        showSeatRepository.save(expiredSeat);

        // Background cleanup should release it
        int released = seatReservationService.releaseExpiredHolds();
        assertTrue(released >= 1);

        // User 2 can now hold D4
        SeatHoldResponse response = seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("D4"), testUser2.getId());
        assertNotNull(response);
        assertEquals("HELD", response.getStatus());
    }
}
