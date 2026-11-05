package com.MovieMatic.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.ShowSeat;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    Optional<ShowSeat> findByShowIdAndSeatNumber(Long showId, String seatNumber);

    Optional<ShowSeat> findByShowAndSeatNumber(Show show, String seatNumber);

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    List<ShowSeat> findByStatusAndHeldAtBefore(SeatStatus status, LocalDateTime expiryTime);

    @Query("SELECT s FROM ShowSeat s WHERE s.show.id = :showId AND s.seatNumber IN :seatNumbers")
    List<ShowSeat> findByShowIdAndSeatNumberIn(@Param("showId") Long showId, @Param("seatNumbers") List<String> seatNumbers);

    @Query("SELECT s FROM ShowSeat s WHERE s.show.id = :showId AND s.status = 'HELD' AND s.heldAt >= :activeSince")
    List<ShowSeat> findActiveHoldsByShowId(@Param("showId") Long showId, @Param("activeSince") LocalDateTime activeSince);

    void deleteByShowId(Long showId);
}
