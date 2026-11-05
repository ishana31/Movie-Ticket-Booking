package com.MovieMatic.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.MovieMatic.model.Movie;
import com.MovieMatic.repository.MovieRepository;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    public List<Movie> getAllMovies() {
    	return movieRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public void saveMovie(Movie movie) {
        movieRepository.save(movie);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id).orElse(null);
    }

    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
    }
    
    public long getTotalMoviesCount() {
        return movieRepository.count();
    }
    
//    public List<Movie> getNewReleases() {
//        LocalDate now = LocalDate.now();
//        LocalDate startOfMonth = now.withDayOfMonth(1);
//        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
//        return movieRepository.findByReleaseDateBetween(startOfMonth, endOfMonth);
//    }
//    
//    public List<Movie> getNowShowing() {
//        return movieRepository.findByReleaseDateBefore(LocalDate.now());
//    }
//    
//    public List<Movie> getUpcomingMovies() {
//        LocalDate nextMonthStart = LocalDate.now().plusMonths(1).withDayOfMonth(1);
//        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
//        return movieRepository.findByReleaseDateBetween(nextMonthStart, nextMonthEnd);
//    }
    
    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenre(genre);
    }
    
    
    public List<Movie> getNowShowing() {
        return movieRepository.findNowShowing();
    }

    public List<Movie> getUpcomingMovies() {
        return movieRepository.findUpcomingMovies();
    }

    public List<Movie> getNewReleases() {
        return movieRepository.findLatestMovies();
    }

    public List<Movie> getTop10ByShowPrice() {
        return movieRepository.findTop10MoviesByHighestShowPrice();
    }
}