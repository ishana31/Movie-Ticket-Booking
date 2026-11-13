package com.MovieMatic.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.MovieMatic.dto.HoldSeatRequest;
import com.MovieMatic.dto.HoldSeatResponse;
import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.exception.SeatAlreadyOccupiedException;
import com.MovieMatic.model.User;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.SeatReservationService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

@ExtendWith(MockitoExtension.class)
public class SeatReservationControllerTest {

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private UserService userService;

    @Mock
    private MovieService movieService;

    @Mock
    private ShowService showService;

    @Mock
    private BookingService bookingService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("customer@example.com");

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("customer@example.com");
    }

    @Test
    @DisplayName("POST /user/shows/{showId}/hold returns 200 OK with HELD status and 600 seconds TTL")
    void testHoldSeats_Success() {
        when(userService.findByEmail("customer@example.com")).thenReturn(testUser);
        when(seatReservationService.holdSeats(1L, "A1,A2", 10L))
                .thenReturn(new HoldSeatResponse("HELD", 600));

        HoldSeatRequest request = new HoldSeatRequest("A1,A2");
        ResponseEntity<?> response = userController.holdSeats(1L, request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        HoldSeatResponse holdResponse = (HoldSeatResponse) response.getBody();
        assertEquals("HELD", holdResponse.getStatus());
        assertEquals(600, holdResponse.getExpiresInSeconds());
    }

    @Test
    @DisplayName("POST /user/shows/{showId}/hold returns 409 Conflict with exact error message when seat is taken")
    void testHoldSeats_Conflict() {
        when(userService.findByEmail("customer@example.com")).thenReturn(testUser);
        when(seatReservationService.holdSeats(1L, "A1,A2", 10L))
                .thenThrow(new SeatAlreadyOccupiedException("Seats already held or booked"));

        HoldSeatRequest request = new HoldSeatRequest("A1,A2");
        ResponseEntity<?> response = userController.holdSeats(1L, request, userDetails);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Seats already held or booked", body.get("message"));
    }

    @Test
    @DisplayName("GET /user/shows/{showId}/occupied-seats returns 200 OK with bookedSeats and heldSeats")
    void testGetOccupiedSeats() {
        OccupiedSeatsResponse mockResponse = new OccupiedSeatsResponse(
                List.of("B1", "B2"),
                List.of("A1", "A2")
        );
        when(seatReservationService.getOccupiedSeats(1L)).thenReturn(mockResponse);

        ResponseEntity<OccupiedSeatsResponse> response = userController.getOccupiedSeats(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(List.of("B1", "B2"), response.getBody().getBookedSeats());
        assertEquals(List.of("A1", "A2"), response.getBody().getHeldSeats());
    }

    @Test
    @DisplayName("POST /user/shows/{showId}/release-hold successfully calls service to release seats")
    void testReleaseHold() {
        when(userService.findByEmail("customer@example.com")).thenReturn(testUser);

        HoldSeatRequest request = new HoldSeatRequest("A1,A2");
        ResponseEntity<?> response = userController.releaseHold(1L, request, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(seatReservationService).releaseHold(eq(1L), eq("A1,A2"), eq(10L));
    }
}
