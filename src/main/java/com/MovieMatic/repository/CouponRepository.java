package com.MovieMatic.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MovieMatic.model.Coupon;

/**
 * Spring Data JPA repository for Coupon entity operations.
 * Provides custom query methods to retrieve promotional coupons by code.
 *
 * @author CodeFactoryAgent
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Finds an active promotional coupon matching the supplied code in a case-insensitive manner.
     *
     * @param code promotional code (e.g., 'SAVE20')
     * @return Optional containing the active Coupon if found
     */
    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    /**
     * Finds any promotional coupon matching the supplied code in a case-insensitive manner,
     * regardless of its active status.
     *
     * @param code promotional code
     * @return Optional containing the Coupon if found
     */
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
