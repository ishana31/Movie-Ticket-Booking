package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.dto.SeatHoldResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;

@Service
public class SeatReservationService {

    private static final Logger logger = LoggerFactory.getLogger(SeatReservationService.class);

    public static final int HOLD_TTL_SECONDS = 600; // 10 minutes

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Per-show locks to ensure concurrency safety across simultaneous hold/book requests
    private final Map<Long, ReentrantLock> showLocks = new ConcurrentHashMap<>();

    private ReentrantLock getLockForShow(Long showId) {
        return showLocks.computeIfAbsent(showId, id -> new ReentrantLock(true));
    }

    /**
     * Atomically holds the requested seats for a user on a given show.
     * All selected seats must be available; if even one is held or booked, none are held.
     */
    @Transactional
    public SeatHoldResponse holdSeats(Long showId, List<String> seatNumbers, Long userId) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("No seats specified for hold");
        }

        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

        List<String> cleanSeats = seatNumbers.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());

        if (cleanSeats.isEmpty()) {
            throw new IllegalArgumentException("No valid seats specified for hold");
        }

        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minusSeconds(HOLD_TTL_SECONDS);

            // 1. Check existing confirmed bookings in booking table
            List<Booking> existingBookings = bookingRepository.findByShow(show);
            Set<String> bookedSeats = new HashSet<>();
            for (Booking b : existingBookings) {
                if (b.getSeatNumbers() != null) {
                    for (String s : b.getSeatNumbers().split(",")) {
                        bookedSeats.add(s.trim());
                    }
                }
            }

            // 2. Query existing show_seats records
            List<ShowSeat> existingShowSeats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, cleanSeats);
            Map<String, ShowSeat> seatMap = existingShowSeats.stream()
                .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));

            // Verify atomic availability for ALL requested seats
            for (String seat : cleanSeats) {
                if (bookedSeats.contains(seat)) {
                    throw new SeatConflictException("Seats already held or booked");
                }

                ShowSeat showSeat = seatMap.get(seat);
                if (showSeat != null) {
                    if (showSeat.getStatus() == SeatStatus.BOOKED) {
                        throw new SeatConflictException("Seats already held or booked");
                    }
                    if (showSeat.getStatus() == SeatStatus.HELD) {
                        // Check if active hold by another user
                        boolean isExpired = showSeat.getHeldAt() == null || showSeat.getHeldAt().isBefore(cutoff);
                        if (!isExpired) {
                            Long holder = showSeat.getHeldByUserId();
                            if (holder != null && !Objects.equals(holder, userId)) {
                                throw new SeatConflictException("Seats already held or booked");
                            }
                        }
                    }
                }
            }

            // All seats are free (or already held by this user/expired) -> Atomically apply hold
            for (String seat : cleanSeats) {
                ShowSeat showSeat = seatMap.get(seat);
                if (showSeat == null) {
                    showSeat = new ShowSeat();
                    showSeat.setShow(show);
                    showSeat.setSeatNumber(seat);
                }
                showSeat.setStatus(SeatStatus.HELD);
                showSeat.setHeldAt(now);
                showSeat.setHeldByUserId(userId);
                showSeatRepository.save(showSeat);
            }

            logger.info("Successfully held {} seats for user {} on show {}", cleanSeats, userId, showId);
            return new SeatHoldResponse("HELD", HOLD_TTL_SECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns booked and active held seats for a show.
     * Expired holds are automatically filtered out.
     */
    @Transactional(readOnly = true)
    public OccupiedSeatsResponse getOccupiedSeats(Long showId) {
        Show show = showRepository.findById(showId).orElse(null);
        if (show == null) {
            return new OccupiedSeatsResponse();
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TTL_SECONDS);

        // Confirmed bookings from booking table
        Set<String> bookedSeats = new HashSet<>();
        List<Booking> bookings = bookingRepository.findByShow(show);
        for (Booking b : bookings) {
            if (b.getSeatNumbers() != null) {
                for (String s : b.getSeatNumbers().split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        bookedSeats.add(trimmed);
                    }
                }
            }
        }

        // Show seats status from show_seats table
        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        Set<String> heldSeats = new HashSet<>();

        for (ShowSeat ss : showSeats) {
            if (ss.getStatus() == SeatStatus.BOOKED) {
                bookedSeats.add(ss.getSeatNumber());
            } else if (ss.getStatus() == SeatStatus.HELD) {
                if (ss.getHeldAt() != null && !ss.getHeldAt().isBefore(cutoff)) {
                    // Active hold
                    heldSeats.add(ss.getSeatNumber());
                }
            }
        }

        // Exclude seats that are booked from held list
        heldSeats.removeAll(bookedSeats);

        List<String> sortedBooked = new ArrayList<>(bookedSeats);
        Collections.sort(sortedBooked);

        List<String> sortedHeld = new ArrayList<>(heldSeats);
        Collections.sort(sortedHeld);

        return new OccupiedSeatsResponse(sortedBooked, sortedHeld);
    }

    /**
     * Confirms purchase and permanently transitions seats from HELD to BOOKED.
     * Enforces Single-Owner invariant: only the user who held the seat can complete purchase.
     */
    @Transactional
    public void confirmBooking(Long showId, List<String> seatNumbers, Long userId) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("No seats to confirm");
        }

        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TTL_SECONDS);
            List<String> cleanSeats = seatNumbers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

            List<ShowSeat> existingShowSeats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, cleanSeats);
            Map<String, ShowSeat> seatMap = existingShowSeats.stream()
                .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));

            // Validate Single Owner & TTL
            for (String seat : cleanSeats) {
                ShowSeat ss = seatMap.get(seat);
                if (ss != null) {
                    if (ss.getStatus() == SeatStatus.BOOKED) {
                        throw new SeatConflictException("Seat " + seat + " is already booked");
                    }
                    if (ss.getStatus() == SeatStatus.HELD) {
                        boolean isExpired = ss.getHeldAt() == null || ss.getHeldAt().isBefore(cutoff);
                        if (isExpired) {
                            throw new SeatConflictException("Hold on seat " + seat + " has expired. Please select again.");
                        }
                        if (ss.getHeldByUserId() != null && !Objects.equals(ss.getHeldByUserId(), userId)) {
                            throw new SeatConflictException("Seat " + seat + " is reserved by another customer.");
                        }
                    }
                }
            }

            // Permanently mark seats as BOOKED
            for (String seat : cleanSeats) {
                ShowSeat ss = seatMap.get(seat);
                if (ss == null) {
                    ss = new ShowSeat();
                    ss.setShow(show);
                    ss.setSeatNumber(seat);
                }
                ss.setStatus(SeatStatus.BOOKED);
                ss.setHeldAt(null);
                ss.setHeldByUserId(userId);
                showSeatRepository.save(ss);
            }

            logger.info("Successfully confirmed booking for seats {} by user {} on show {}", cleanSeats, userId, showId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Background cleanup task running every 60 seconds to release seats
     * whose hold time exceeded 10 minutes (600s) back to AVAILABLE.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public int releaseExpiredHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TTL_SECONDS);
        int releasedCount = showSeatRepository.releaseExpiredHolds(
            SeatStatus.HELD,
            SeatStatus.AVAILABLE,
            cutoff
        );
        if (releasedCount > 0) {
            logger.info("Released {} expired seat holds back to AVAILABLE", releasedCount);
        }
        return releasedCount;
    }

    /**
     * Release hold manually (e.g. if user cancels hold or navigates away)
     */
    @Transactional
    public void releaseSeatsForUser(Long showId, List<String> seatNumbers, Long userId) {
        if (seatNumbers == null || seatNumbers.isEmpty()) return;

        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            List<ShowSeat> seats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, seatNumbers);
            for (ShowSeat ss : seats) {
                if (ss.getStatus() == SeatStatus.HELD && Objects.equals(ss.getHeldByUserId(), userId)) {
                    ss.setStatus(SeatStatus.AVAILABLE);
                    ss.setHeldAt(null);
                    ss.setHeldByUserId(null);
                    showSeatRepository.save(ss);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
