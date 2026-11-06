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
import org.springframework.transaction.annotation.Transactional;

import com.MovieMatic.model.SeatStatus;
import com.MovieMatic.model.ShowSeat;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    Optional<ShowSeat> findByShowIdAndSeatNumber(Long showId, String seatNumber);

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndSeatNumberIn(Long showId, Collection<String> seatNumbers);

    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    List<ShowSeat> findByStatusAndHeldAtBefore(SeatStatus status, LocalDateTime cutoff);

    @Query("SELECT s FROM ShowSeat s WHERE s.show.id = :showId AND s.status = :status AND s.heldAt >= :cutoff")
    List<ShowSeat> findActiveHoldsByShowId(
        @Param("showId") Long showId,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Transactional
    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = :newStatus, s.heldAt = null, s.heldByUserId = null WHERE s.status = :currentStatus AND s.heldAt < :cutoff")
    int releaseExpiredHolds(
        @Param("currentStatus") SeatStatus currentStatus,
        @Param("newStatus") SeatStatus newStatus,
        @Param("cutoff") LocalDateTime cutoff
    );

    @Transactional
    @Modifying
    @Query("DELETE FROM ShowSeat s WHERE s.show.id = :showId")
    void deleteByShowId(@Param("showId") Long showId);
}
