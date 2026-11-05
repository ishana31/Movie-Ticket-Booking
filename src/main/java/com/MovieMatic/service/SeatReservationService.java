package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.dto.SeatHoldResponse;
import com.MovieMatic.exception.SeatUnavailableException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;

@Service
public class SeatReservationService {

    public static final int HOLD_TIMEOUT_SECONDS = 600; // 10 minutes TTL

    private final ConcurrentHashMap<Long, ReentrantLock> showLocks = new ConcurrentHashMap<>();

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private ReentrantLock getLockForShow(Long showId) {
        return showLocks.computeIfAbsent(showId, k -> new ReentrantLock(true));
    }

    /**
     * Atomically holds the requested seats for a 10-minute countdown.
     * All requested seats must be available; if any one is unavailable, none are held.
     */
    @Transactional
    public SeatHoldResponse holdSeats(Long showId, List<String> seatNumbers, Long userId) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new IllegalArgumentException("Seat numbers must not be empty");
        }

        List<String> distinctSeats = seatNumbers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (distinctSeats.isEmpty()) {
            throw new IllegalArgumentException("Seat numbers must not be empty");
        }

        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            Show show = showRepository.findById(showId)
                    .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minusSeconds(HOLD_TIMEOUT_SECONDS);

            // 1. Gather all currently booked seats (ShowSeat BOOKED status + existing Booking records)
            Set<String> bookedSeats = new HashSet<>();
            List<Booking> showBookings = bookingRepository.findByShow(show);
            for (Booking b : showBookings) {
                if (b.getSeatNumbers() != null) {
                    for (String s : b.getSeatNumbers().split(",")) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty()) {
                            bookedSeats.add(trimmed);
                        }
                    }
                }
            }

            List<ShowSeat> existingSeats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, distinctSeats);
            for (ShowSeat ss : existingSeats) {
                if (ss.getStatus() == SeatStatus.BOOKED) {
                    bookedSeats.add(ss.getSeatNumber());
                }
            }

            // Check if any seat is already booked
            for (String seat : distinctSeats) {
                if (bookedSeats.contains(seat)) {
                    throw new SeatUnavailableException("Seats already held or booked");
                }
            }

            // 2. Check if any seat is actively held by another user
            for (ShowSeat ss : existingSeats) {
                if (ss.getStatus() == SeatStatus.HELD && ss.getHeldAt() != null && ss.getHeldAt().isAfter(cutoff)) {
                    // Active hold. Check single-owner / user check
                    if (userId == null || ss.getHeldByUserId() == null || !userId.equals(ss.getHeldByUserId())) {
                        throw new SeatUnavailableException("Seats already held or booked");
                    }
                }
            }

            // 3. Atomic reservation: All seats are available, persist/update them
            Map<String, ShowSeat> seatMap = existingSeats.stream()
                    .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));

            List<ShowSeat> toSave = new ArrayList<>();
            for (String seat : distinctSeats) {
                ShowSeat ss = seatMap.get(seat);
                if (ss == null) {
                    ss = new ShowSeat();
                    ss.setShow(show);
                    ss.setSeatNumber(seat);
                }
                ss.setStatus(SeatStatus.HELD);
                ss.setHeldAt(now);
                ss.setHeldByUserId(userId);
                toSave.add(ss);
            }

            showSeatRepository.saveAll(toSave);
            return new SeatHoldResponse("HELD", HOLD_TIMEOUT_SECONDS);
        } catch (DataIntegrityViolationException dive) {
            throw new SeatUnavailableException("Seats already held or booked", dive);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves occupied seats for a show, categorized into bookedSeats and heldSeats.
     */
    @Transactional(readOnly = true)
    public OccupiedSeatsResponse getOccupiedSeats(Long showId) {
        Show show = showRepository.findById(showId).orElse(null);
        if (show == null) {
            return new OccupiedSeatsResponse(Collections.emptyList(), Collections.emptyList());
        }

        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TIMEOUT_SECONDS);

        Set<String> bookedSeats = new LinkedHashSet<>();
        List<ShowSeat> allSeats = showSeatRepository.findByShowId(showId);
        for (ShowSeat ss : allSeats) {
            if (ss.getStatus() == SeatStatus.BOOKED) {
                bookedSeats.add(ss.getSeatNumber());
            }
        }

        // Backward compatibility: also include seats recorded in Booking entities
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

        // Active holds (excluding any that are marked booked)
        Set<String> heldSeats = new LinkedHashSet<>();
        for (ShowSeat ss : allSeats) {
            if (ss.getStatus() == SeatStatus.HELD && ss.getHeldAt() != null && ss.getHeldAt().isAfter(cutoff)) {
                if (!bookedSeats.contains(ss.getSeatNumber())) {
                    heldSeats.add(ss.getSeatNumber());
                }
            }
        }

        return new OccupiedSeatsResponse(new ArrayList<>(bookedSeats), new ArrayList<>(heldSeats));
    }

    /**
     * Confirms booking and transitions seats permanently from HELD to BOOKED.
     * Enforces Single Owner and Timeout rules.
     */
    @Transactional
    public void confirmBooking(Long showId, List<String> seatNumbers, Long userId) {
        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            Show show = showRepository.findById(showId)
                    .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TIMEOUT_SECONDS);
            List<ShowSeat> existing = showSeatRepository.findByShowIdAndSeatNumberIn(showId, seatNumbers);
            Map<String, ShowSeat> seatMap = existing.stream()
                    .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));

            for (String seat : seatNumbers) {
                ShowSeat ss = seatMap.get(seat);
                if (ss == null) {
                    throw new SeatUnavailableException("Seat " + seat + " was not held");
                }
                if (ss.getStatus() == SeatStatus.BOOKED) {
                    throw new SeatUnavailableException("Seat " + seat + " is already booked");
                }
                if (ss.getStatus() != SeatStatus.HELD || ss.getHeldAt() == null || ss.getHeldAt().isBefore(cutoff)) {
                    throw new SeatUnavailableException("Hold for seat " + seat + " has expired");
                }
                if (userId != null && ss.getHeldByUserId() != null && !userId.equals(ss.getHeldByUserId())) {
                    throw new SeatUnavailableException("Seat " + seat + " is held by another customer");
                }
            }

            List<ShowSeat> toBook = new ArrayList<>();
            for (String seat : seatNumbers) {
                ShowSeat ss = seatMap.get(seat);
                ss.setStatus(SeatStatus.BOOKED);
                toBook.add(ss);
            }
            showSeatRepository.saveAll(toBook);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Direct booking fallback for form posts.
     */
    @Transactional
    public void bookSeatsDirectly(Long showId, List<String> seatNumbers, Long userId) {
        ReentrantLock lock = getLockForShow(showId);
        lock.lock();
        try {
            Show show = showRepository.findById(showId)
                    .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

            List<ShowSeat> existing = showSeatRepository.findByShowIdAndSeatNumberIn(showId, seatNumbers);
            Map<String, ShowSeat> seatMap = existing.stream()
                    .collect(Collectors.toMap(ShowSeat::getSeatNumber, s -> s));

            List<ShowSeat> toBook = new ArrayList<>();
            for (String seat : seatNumbers) {
                ShowSeat ss = seatMap.get(seat);
                if (ss == null) {
                    ss = new ShowSeat();
                    ss.setShow(show);
                    ss.setSeatNumber(seat);
                }
                ss.setStatus(SeatStatus.BOOKED);
                ss.setHeldByUserId(userId);
                toBook.add(ss);
            }
            showSeatRepository.saveAll(toBook);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Background cleanup task running every 60 seconds to release expired holds.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOLD_TIMEOUT_SECONDS);
        List<ShowSeat> expiredSeats = showSeatRepository.findByStatusAndHeldAtBefore(SeatStatus.HELD, cutoff);
        if (expiredSeats != null && !expiredSeats.isEmpty()) {
            for (ShowSeat ss : expiredSeats) {
                ss.setStatus(SeatStatus.AVAILABLE);
                ss.setHeldAt(null);
                ss.setHeldByUserId(null);
            }
            showSeatRepository.saveAll(expiredSeats);
        }
    }
}
