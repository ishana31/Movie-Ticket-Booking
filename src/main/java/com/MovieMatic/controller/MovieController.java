package com.MovieMatic.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.MovieMatic.model.Movie;
import com.MovieMatic.service.MovieService;

@Controller
@RequestMapping("/admin")
public class MovieController {

    @Autowired
    private MovieService movieService;
    

    @GetMapping("/movies")
    public String listMovies(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "admin/movies";
    }

    @GetMapping("/movies/add")
    public String addMovieForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/add-movie";
    }

    @PostMapping("/movies/save")
    public String saveMovie(@ModelAttribute Movie movie,
                            @RequestParam(value = "image", required = false) MultipartFile file,
                            @RequestParam(value = "background", required = false) MultipartFile bgFile) throws IOException {

        Movie existingMovie = movie.getId() != null ? movieService.getMovieById(movie.getId()) : null;

        if (file != null && !file.isEmpty()) {
            movie.setPosterData(file.getBytes());
        } else if (existingMovie != null) {
            movie.setPosterData(existingMovie.getPosterData());
        }

        if (bgFile != null && !bgFile.isEmpty()) {
            movie.setBackgroundData(bgFile.getBytes());
        } else if (existingMovie != null) {
            movie.setBackgroundData(existingMovie.getBackgroundData());
        }

        // fill missing fields from existing movie if editing
        if (existingMovie != null) {
            if (movie.getTitle() == null || movie.getTitle().isEmpty())
                movie.setTitle(existingMovie.getTitle());
            if (movie.getDescription() == null || movie.getDescription().isEmpty())
                movie.setDescription(existingMovie.getDescription());
            if (movie.getGenre() == null || movie.getGenre().isEmpty())
                movie.setGenre(existingMovie.getGenre());
            if (movie.getLanguage() == null || movie.getLanguage().isEmpty())
                movie.setLanguage(existingMovie.getLanguage());
            if (movie.getReleaseDate() == null)
                movie.setReleaseDate(existingMovie.getReleaseDate());
            if (movie.getDuration() == 0)
                movie.setDuration(existingMovie.getDuration());
        }

        movieService.saveMovie(movie);
        return "redirect:/admin/movies";
    }

    @GetMapping("/movies/edit/{id}")
    public String editMovie(@PathVariable Long id, Model model) {
        Movie movie = movieService.getMovieById(id);
        model.addAttribute("movie", movie);
        return "admin/add-movie";
    }

    @GetMapping("/movies/delete/{id}")
    public String deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return "redirect:/admin/movies";
    }
    
    @GetMapping("/movies/image/{id}")
    @ResponseBody
    public byte[] getMovieImage(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return movie.getPosterData();
    }
    
    @GetMapping("/movies/background/{id}")
    @ResponseBody
    public byte[] getMovieBackground(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return movie.getBackgroundData();
    }
}
