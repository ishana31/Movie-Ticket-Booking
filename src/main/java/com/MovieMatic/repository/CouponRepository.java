package com.MovieMatic.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MovieMatic.model.Coupon;

/**
 * Spring Data JPA repository for managing {@link Coupon} persistence operations.
 *
 * @author CodeFactoryAgent
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Finds an active coupon by its promotional code, matching case-insensitively.
     *
     * @param code the promotional discount code string (e.g. "SAVE20", "save20")
     * @return an {@link Optional} containing the matching active coupon if found, or empty otherwise
     */
    Optional<Coupon> findByCodeIgnoreCaseAndIsActiveTrue(String code);

    /**
     * Finds a coupon by its promotional code irrespective of activation state, matching case-insensitively.
     *
     * @param code the promotional code string
     * @return an {@link Optional} containing the coupon if found
     */
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
