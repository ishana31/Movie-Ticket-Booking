package com.MovieMatic.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class SeatReservationService {

    private static final Logger logger = LoggerFactory.getLogger(SeatReservationService.class);

    public static final int HOLD_TTL_SECONDS = 600; // 10 minutes
    public static final int HOLD_TTL_MINUTES = 10;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Concurrency lock monitor per show ID to ensure thread-safe hold and release
    private final ConcurrentHashMap<Long, Object> showLocks = new ConcurrentHashMap<>();

    private Object getLockForShow(Long showId) {
        return showLocks.computeIfAbsent(showId, id -> new Object());
    }

    /**
     * Atomically holds the requested seats for a show for 10 minutes.
     * If any seat is unavailable (already held by another user or booked), the operation fails
     * and no seats are held (Atomic Reservation).
     */
    @Transactional
    public HoldSeatResponse holdSeats(Long showId, String seatNumbersCsv, Long userId) {
        if (seatNumbersCsv == null || seatNumbersCsv.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat numbers cannot be empty");
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found with ID: " + showId));

        List<String> requestedSeats = parseSeats(seatNumbersCsv);

        synchronized (getLockForShow(showId)) {
            // First clean up expired holds for this show
            releaseExpiredHoldsForShow(showId);

            // Fetch existing ShowSeat records for requested seats
            List<ShowSeat> existingSeats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, requestedSeats);
            Set<String> legacyBookedSeats = getLegacyBookedSeatsForShow(show);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoff = now.minusMinutes(HOLD_TTL_MINUTES);

            // Check if ANY requested seat is unavailable
            for (String seatNum : requestedSeats) {
                if (legacyBookedSeats.contains(seatNum)) {
                    throw new SeatAlreadyOccupiedException("Seats already held or booked");
                }

                Optional<ShowSeat> match = existingSeats.stream()
                        .filter(s -> s.getSeatNumber().equalsIgnoreCase(seatNum))
                        .findFirst();

                if (match.isPresent()) {
                    ShowSeat seat = match.get();
                    if (seat.getStatus() == SeatStatus.BOOKED) {
                        throw new SeatAlreadyOccupiedException("Seats already held or booked");
                    }
                    if (seat.getStatus() == SeatStatus.HELD) {
                        boolean isExpired = seat.getHeldAt() == null || seat.getHeldAt().isBefore(cutoff);
                        boolean isOtherUser = seat.getHeldByUserId() != null && !seat.getHeldByUserId().equals(userId);
                        if (!isExpired && isOtherUser) {
                            throw new SeatAlreadyOccupiedException("Seats already held or booked");
                        }
                    }
                }
            }

            // All seats are available -> Atomically apply HELD status to all requested seats
            List<ShowSeat> toSave = new ArrayList<>();
            for (String seatNum : requestedSeats) {
                Optional<ShowSeat> match = existingSeats.stream()
                        .filter(s -> s.getSeatNumber().equalsIgnoreCase(seatNum))
                        .findFirst();

                ShowSeat seat;
                if (match.isPresent()) {
                    seat = match.get();
                } else {
                    seat = new ShowSeat();
                    seat.setShow(show);
                    seat.setSeatNumber(seatNum);
                }
                seat.setStatus(SeatStatus.HELD);
                seat.setHeldAt(now);
                seat.setHeldByUserId(userId);
                toSave.add(seat);
            }

            showSeatRepository.saveAll(toSave);
            return new HoldSeatResponse("HELD", HOLD_TTL_SECONDS);
        }
    }

    /**
     * Retrieves currently occupied seats (booked and active held) for a show.
     */
    @Transactional(readOnly = true)
    public OccupiedSeatsResponse getOccupiedSeats(Long showId) {
        Show show = showRepository.findById(showId).orElse(null);
        if (show == null) {
            return new OccupiedSeatsResponse(Collections.emptyList(), Collections.emptyList());
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HOLD_TTL_MINUTES);

        List<ShowSeat> allSeats = showSeatRepository.findByShowId(showId);
        Set<String> legacyBooked = getLegacyBookedSeatsForShow(show);

        Set<String> bookedSeats = new HashSet<>(legacyBooked);
        Set<String> heldSeats = new HashSet<>();

        for (ShowSeat seat : allSeats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                bookedSeats.add(seat.getSeatNumber());
            } else if (seat.getStatus() == SeatStatus.HELD) {
                if (seat.getHeldAt() != null && seat.getHeldAt().isAfter(cutoff)) {
                    if (!bookedSeats.contains(seat.getSeatNumber())) {
                        heldSeats.add(seat.getSeatNumber());
                    }
                }
            }
        }

        List<String> sortedBooked = new ArrayList<>(bookedSeats);
        Collections.sort(sortedBooked);

        List<String> sortedHeld = new ArrayList<>(heldSeats);
        Collections.sort(sortedHeld);

        return new OccupiedSeatsResponse(sortedBooked, sortedHeld);
    }

    /**
     * Releases seat hold explicitly (e.g. upon user abandonment or checkout cancellation).
     */
    @Transactional
    public void releaseHold(Long showId, String seatNumbersCsv, Long userId) {
        if (seatNumbersCsv == null || seatNumbersCsv.trim().isEmpty()) {
            return;
        }
        List<String> requestedSeats = parseSeats(seatNumbersCsv);

        synchronized (getLockForShow(showId)) {
            List<ShowSeat> seats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, requestedSeats);
            List<ShowSeat> toUpdate = new ArrayList<>();
            for (ShowSeat seat : seats) {
                if (seat.getStatus() == SeatStatus.HELD && (userId == null || userId.equals(seat.getHeldByUserId()))) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setHeldAt(null);
                    seat.setHeldByUserId(null);
                    toUpdate.add(seat);
                }
            }
            if (!toUpdate.isEmpty()) {
                showSeatRepository.saveAll(toUpdate);
            }
        }
    }

    /**
     * Confirms booking and transitions seats permanently from HELD to BOOKED.
     * Enforces Single Owner invariant: only the user who placed the hold can complete the purchase.
     */
    @Transactional
    public Booking confirmBooking(Show show, String seatNumbersCsv, User user) {
        if (seatNumbersCsv == null || seatNumbersCsv.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat numbers cannot be empty");
        }

        Long showId = show.getId();
        List<String> requestedSeats = parseSeats(seatNumbersCsv);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(HOLD_TTL_MINUTES);

        synchronized (getLockForShow(showId)) {
            List<ShowSeat> seats = showSeatRepository.findByShowIdAndSeatNumberIn(showId, requestedSeats);
            Set<String> legacyBooked = getLegacyBookedSeatsForShow(show);

            for (String seatNum : requestedSeats) {
                if (legacyBooked.contains(seatNum)) {
                    throw new SeatAlreadyOccupiedException("Seat " + seatNum + " is already booked");
                }

                Optional<ShowSeat> match = seats.stream()
                        .filter(s -> s.getSeatNumber().equalsIgnoreCase(seatNum))
                        .findFirst();

                if (match.isPresent()) {
                    ShowSeat seat = match.get();
                    if (seat.getStatus() == SeatStatus.BOOKED) {
                        throw new SeatAlreadyOccupiedException("Seat " + seatNum + " is already booked");
                    }
                    if (seat.getStatus() == SeatStatus.HELD) {
                        boolean isExpired = seat.getHeldAt() == null || seat.getHeldAt().isBefore(cutoff);
                        boolean isOtherUser = seat.getHeldByUserId() != null && !seat.getHeldByUserId().equals(user.getId());
                        if (isOtherUser) {
                            throw new SeatAlreadyOccupiedException("Selected seat(s) are currently reserved by another customer.");
                        }
                        if (isExpired) {
                            throw new SeatAlreadyOccupiedException("Hold expired. Please select seats again.");
                        }
                    }
                }
            }

            // Transition all requested seats to BOOKED
            List<ShowSeat> toSave = new ArrayList<>();
            for (String seatNum : requestedSeats) {
                Optional<ShowSeat> match = seats.stream()
                        .filter(s -> s.getSeatNumber().equalsIgnoreCase(seatNum))
                        .findFirst();

                ShowSeat seat;
                if (match.isPresent()) {
                    seat = match.get();
                } else {
                    seat = new ShowSeat();
                    seat.setShow(show);
                    seat.setSeatNumber(seatNum);
                }
                seat.setStatus(SeatStatus.BOOKED);
                seat.setHeldAt(null);
                seat.setHeldByUserId(user.getId());
                toSave.add(seat);
            }
            showSeatRepository.saveAll(toSave);

            // Create and save the Booking entity
            double totalAmount = show.getPrice() * requestedSeats.size();
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setShow(show);
            booking.setSeatNumbers(String.join(",", requestedSeats));
            booking.setTotalAmount(totalAmount);
            booking.setBookingTime(now);

            return bookingRepository.save(booking);
        }
    }

    /**
     * Background cleanup task running every 60 seconds to release seats
     * whose hold time exceeded 10 minutes back to AVAILABLE.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoReleaseExpiredHolds() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HOLD_TTL_MINUTES);
        List<ShowSeat> expiredSeats = showSeatRepository.findByStatusAndHeldAtBefore(SeatStatus.HELD, cutoff);

        if (!expiredSeats.isEmpty()) {
            for (ShowSeat seat : expiredSeats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldAt(null);
                seat.setHeldByUserId(null);
            }
            showSeatRepository.saveAll(expiredSeats);
            logger.info("Auto-released {} expired seat holds back to AVAILABLE status.", expiredSeats.size());
        }
    }

    private void releaseExpiredHoldsForShow(Long showId) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HOLD_TTL_MINUTES);
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        List<ShowSeat> expired = new ArrayList<>();
        for (ShowSeat seat : seats) {
            if (seat.getStatus() == SeatStatus.HELD && (seat.getHeldAt() == null || seat.getHeldAt().isBefore(cutoff))) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldAt(null);
                seat.setHeldByUserId(null);
                expired.add(seat);
            }
        }
        if (!expired.isEmpty()) {
            showSeatRepository.saveAll(expired);
        }
    }

    private Set<String> getLegacyBookedSeatsForShow(Show show) {
        List<Booking> bookings = bookingRepository.findByShow(show);
        Set<String> booked = new HashSet<>();
        for (Booking b : bookings) {
            if (b.getSeatNumbers() != null && !b.getSeatNumbers().trim().isEmpty()) {
                String[] parts = b.getSeatNumbers().split(",");
                for (String p : parts) {
                    if (!p.trim().isEmpty()) {
                        booked.add(p.trim());
                    }
                }
            }
        }
        return booked;
    }

    private List<String> parseSeats(String seatNumbersCsv) {
        return Arrays.stream(seatNumbersCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
