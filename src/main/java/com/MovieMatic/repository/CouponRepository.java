package com.MovieMatic.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MovieMatic.model.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code) AND c.isActive = true")
    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(@Param("code") String code);

    Optional<Coupon> findByCodeIgnoreCase(String code);
}
