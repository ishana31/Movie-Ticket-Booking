package com.MovieMatic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;

import jakarta.transaction.Transactional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    
    List<Booking> findByShow(Show show);
    
    @Query("SELECT SUM(b.totalAmount) FROM Booking b")
    Double sumTotalAmount();
    
    @Transactional
    @Modifying
    @Query("DELETE FROM Booking b WHERE b.show.id = :showId")
    void deleteByShowId(@Param("showId") Long showId);

}
