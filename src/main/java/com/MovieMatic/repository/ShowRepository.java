package com.MovieMatic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MovieMatic.model.Show;

public interface ShowRepository extends JpaRepository<Show, Long> {
	
    List<Show> findByMovieId(Long movieId);
    
    List<Show> findTop10ByOrderByPriceDesc();
    
    @Query("SELECT s FROM Show s WHERE LOWER(s.movie.genre) = LOWER(:genre)")
    List<Show> findShowsByMovieGenre(@Param("genre") String genre);

}