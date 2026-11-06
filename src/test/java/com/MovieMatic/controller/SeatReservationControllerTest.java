package com.MovieMatic.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.MovieRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.SeatReservationService;

@SpringBootTest
@AutoConfigureMockMvc
public class SeatReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private SeatReservationService seatReservationService;

    private Show testShow;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        showSeatRepository.deleteAll();
        bookingRepository.deleteAll();

        testUser = userRepository.findByEmail("ctrl_user@example.com");
        if (testUser == null) {
            testUser = new User("Ctrl User", "ctrl_user@example.com", "password", "USER");
            testUser = userRepository.save(testUser);
        }

        otherUser = userRepository.findByEmail("ctrl_other@example.com");
        if (otherUser == null) {
            otherUser = new User("Other User", "ctrl_other@example.com", "password", "USER");
            otherUser = userRepository.save(otherUser);
        }

        Movie movie = new Movie();
        movie.setTitle("Controller Test Movie");
        movie.setGenre("Sci-Fi");
        movie.setDuration(110);
        movie = movieRepository.save(movie);

        testShow = new Show();
        testShow.setMovie(movie);
        testShow.setShowDate(LocalDate.now().plusDays(2));
        testShow.setShowTime(LocalTime.of(20, 0));
        testShow.setScreen("Screen 2");
        testShow.setPrice(300.0);
        testShow = showRepository.save(testShow);
    }

    @Test
    @WithMockUser(username = "ctrl_user@example.com", roles = {"USER"})
    @DisplayName("POST /user/shows/{showId}/hold - successfully holds seats")
    void testHoldSeatsEndpointSuccess() throws Exception {
        String requestJson = "{\"seatNumbers\":\"A1,A2\"}";

        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));
    }

    @Test
    @WithMockUser(username = "ctrl_other@example.com", roles = {"USER"})
    @DisplayName("POST /user/shows/{showId}/hold - 409 Conflict when seats are already held")
    void testHoldSeatsEndpointConflict() throws Exception {
        // User 1 holds A1
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("A1"), testUser.getId());

        // Other user attempts to hold A1
        String requestJson = "{\"seatNumbers\":\"A1\"}";

        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seats already held or booked"));
    }

    @Test
    @WithMockUser(username = "ctrl_user@example.com", roles = {"USER"})
    @DisplayName("GET /user/shows/{showId}/occupied-seats - returns bookedSeats and heldSeats")
    void testGetOccupiedSeatsEndpoint() throws Exception {
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("E1"), testUser.getId());
        seatReservationService.confirmBooking(testShow.getId(), Collections.singletonList("E2"), testUser.getId());

        mockMvc.perform(get("/user/shows/" + testShow.getId() + "/occupied-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldSeats[0]").value("E1"))
                .andExpect(jsonPath("$.bookedSeats[0]").value("E2"));
    }

    @Test
    @WithMockUser(username = "ctrl_user@example.com", roles = {"USER"})
    @DisplayName("POST /user/bookings - successfully completes booking for held seats")
    void testCompleteBookingSuccess() throws Exception {
        // Hold seat F1
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("F1"), testUser.getId());

        String bookingJson = "{\"showId\":" + testShow.getId() + ",\"seatNumbers\":\"F1\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").exists());
    }

    @Test
    @WithMockUser(username = "ctrl_other@example.com", roles = {"USER"})
    @DisplayName("POST /user/bookings - single owner rule: other user cannot purchase held seats")
    void testCompleteBookingSingleOwnerEnforcement() throws Exception {
        // User 1 holds seat G1
        seatReservationService.holdSeats(testShow.getId(), Collections.singletonList("G1"), testUser.getId());

        // Other user attempts to complete booking for G1
        String bookingJson = "{\"showId\":" + testShow.getId() + ",\"seatNumbers\":\"G1\"}";

        mockMvc.perform(post("/user/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson))
                .andExpect(status().isConflict());
    }
}
