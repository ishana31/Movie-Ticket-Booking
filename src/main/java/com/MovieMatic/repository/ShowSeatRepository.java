package com.MovieMatic.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;

import jakarta.transaction.Transactional;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShow(Show show);

    List<ShowSeat> findByShowId(Long showId);

    Optional<ShowSeat> findByShowIdAndSeatNumber(Long showId, String seatNumber);

    List<ShowSeat> findByShowIdAndSeatNumberIn(Long showId, Collection<String> seatNumbers);

    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    @Query("SELECT s FROM ShowSeat s WHERE s.show.id = :showId AND s.status = :status AND s.heldAt > :cutoff")
    List<ShowSeat> findActiveHoldsByShowId(@Param("showId") Long showId,
                                          @Param("status") SeatStatus status,
                                          @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT s FROM ShowSeat s WHERE s.status = :status AND s.heldAt > :cutoff")
    List<ShowSeat> findActiveHolds(@Param("status") SeatStatus status,
                                   @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT s FROM ShowSeat s WHERE s.status = :status AND s.heldAt <= :cutoff")
    List<ShowSeat> findByStatusAndHeldAtBefore(@Param("status") SeatStatus status,
                                              @Param("cutoff") LocalDateTime cutoff);

    @Transactional
    @Modifying
    @Query("DELETE FROM ShowSeat s WHERE s.show.id = :showId")
    void deleteByShowId(@Param("showId") Long showId);
}
