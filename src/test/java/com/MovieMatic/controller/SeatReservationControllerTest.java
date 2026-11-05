package com.MovieMatic.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.repository.BookingRepository;
import com.MovieMatic.repository.MovieRepository;
import com.MovieMatic.repository.ShowRepository;
import com.MovieMatic.repository.ShowSeatRepository;

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
    private BookingRepository bookingRepository;

    private Show testShow;

    @BeforeEach
    void setUp() {
        showSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        showRepository.deleteAll();
        movieRepository.deleteAll();

        Movie movie = new Movie();
        movie.setTitle("Inception");
        movie.setGenre("Sci-Fi");
        movie.setDuration(148);
        movie.setReleaseDate(LocalDate.now());
        movieRepository.save(movie);

        Show show = new Show();
        show.setMovie(movie);
        show.setShowDate(LocalDate.now());
        show.setShowTime(LocalTime.of(20, 0));
        show.setScreen("Screen IMAX");
        show.setPrice(300.0);
        testShow = showRepository.save(show);
    }

    @Test
    @WithMockUser(username = "user1@example.com", roles = {"USER"})
    @DisplayName("POST /user/shows/{showId}/hold - 200 OK with HELD and expiresInSeconds: 600")
    void testHoldSeatsSuccessEndpoint() throws Exception {
        String requestJson = "{\"seatNumbers\": \"A1,A2\"}";

        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HELD")))
                .andExpect(jsonPath("$.expiresInSeconds", is(600)));
    }

    @Test
    @WithMockUser(username = "user1@example.com", roles = {"USER"})
    @DisplayName("POST /user/shows/{showId}/hold - 409 Conflict when seats already held")
    void testHoldSeatsConflictEndpoint() throws Exception {
        String requestJson = "{\"seatNumbers\": \"A1,A2\"}";

        // First user holds A1, A2
        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());

        // Second user attempts to hold A1
        String conflictJson = "{\"seatNumbers\": \"A1,B1\", \"userId\": 99}";
        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(conflictJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Seats already held or booked")));
    }

    @Test
    @WithMockUser(username = "user1@example.com", roles = {"USER"})
    @DisplayName("GET /user/shows/{showId}/occupied-seats - 200 OK with bookedSeats and heldSeats")
    void testGetOccupiedSeatsEndpoint() throws Exception {
        // Hold seats A1, A2
        mockMvc.perform(post("/user/shows/" + testShow.getId() + "/hold")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatNumbers\": \"A1,A2\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/shows/" + testShow.getId() + "/occupied-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldSeats", containsInAnyOrder("A1", "A2")))
                .andExpect(jsonPath("$.bookedSeats").isArray());
    }
}
