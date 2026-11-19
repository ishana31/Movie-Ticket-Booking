# Architecture Guide – MovieMatic

## Overview
MovieMatic is built on Spring Boot 3 using a layered monolithic architecture with Spring Security, Spring Data JPA, and Thymeleaf with Bootstrap 5.

## Layering Conventions
1. **Controller Layer (`com.MovieMatic.controller`)**:
   - Handles HTTP requests, authentication principal resolution (`@AuthenticationPrincipal`), route mapping, and payload deserialization.
   - Delegates business logic to services.
   - Produces JSON responses (`@ResponseBody` / `ResponseEntity<?>`) for REST APIs and view names (`String`) for Thymeleaf templates.

2. **Service Layer (`com.MovieMatic.service`)**:
   - Encapsulates business logic, domain validations, discount calculations, and transactions (`@Transactional`).
   - `CouponService`: Validates promo codes, checks expiration, minimum order thresholds, and calculates percentage and flat discount values.
   - `BookingService`: Manages ticket reservations and occupied seat states.

3. **Repository Layer (`com.MovieMatic.repository`)**:
   - Extends Spring Data `JpaRepository`.
   - `CouponRepository`: Exposes `findByCodeIgnoreCaseAndIsActiveTrue(String code)` for case-insensitive lookup of active coupons.

4. **Domain Model (`com.MovieMatic.model`)**:
   - JPA entities with identity-based generation strategy.
   - `Coupon`: Represents coupon configurations and usage counters.
   - `Booking`: Tracks reservation details, seat assignments, applied `coupon_code`, and `discount_amount`.

5. **DTO & Exception Layer (`com.MovieMatic.dto`, `com.MovieMatic.exception`)**:
   - Data transfer objects (`CouponValidationRequest`, `CouponValidationResponse`, `BookingRequest`).
   - Domain-specific exceptions (`CouponValidationException`) caught by controllers to return informative HTTP status codes.

## HTTP Status Conventions
- `200 OK`: Request succeeded (e.g., successful validation or booking).
- `400 Bad Request`: Input validation failed, coupon expired/invalid, or minimum order threshold unmet.
- `401 Unauthorized / 403 Forbidden`: Unauthenticated or unauthorized access enforced by Spring Security.
- `500 Internal Server Error`: Unhandled server runtime exceptions.
