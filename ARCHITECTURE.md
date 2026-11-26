# MovieMatic Architecture & System Design

## 1. Overview
MovieMatic is an enterprise-grade movie ticket booking web application built with Spring Boot 3, Spring Security, Spring Data JPA, and Thymeleaf + Bootstrap 5.

## 2. Layered Architecture
MovieMatic adheres to standard layered Spring architectural patterns:

- **Presentation / Web Layer (`com.MovieMatic.controller`):**
  - Thymeleaf Controllers: Render dynamic views for users and administrators.
  - REST Controllers (`@RestController`): Expose JSON endpoints for asynchronous client interactions (e.g., `CouponController` at `/user/coupons`, `UserController` REST endpoints).
  
- **Service Layer (`com.MovieMatic.service`):**
  - Contains domain business logic, transactional boundaries (`@Transactional`), and orchestration.
  - Examples: `CouponService`, `BookingService`, `MovieService`, `ShowService`, `UserService`, `EmailService`.
  
- **Persistence Layer (`com.MovieMatic.repository`):**
  - Spring Data JPA repositories extending `JpaRepository`.
  - Examples: `CouponRepository`, `BookingRepository`, `MovieRepository`, `ShowRepository`, `UserRepository`.

- **Domain Model (`com.MovieMatic.model`):**
  - JPA entities mapping database tables with relational mappings (`@ManyToOne`, `@OneToMany`).
  - Entities: `Coupon`, `Booking`, `Show`, `Movie`, `User`, `DiscountType`.

- **Data Transfer Objects (`com.MovieMatic.dto`):**
  - Encapsulate request and response payloads decoupled from persistence entities.
  - Examples: `CouponValidationRequest`, `CouponValidationResponse`, `BookingRequest`.

## 3. Security Architecture
- **Spring Security Filter Chain (`SecurityConfig`):**
  - Role-based authorization:
    - `/admin/**` requires `ROLE_ADMIN`
    - `/user/**` requires `ROLE_USER`
    - Public resources: `/login`, `/register`, `/reset-password`, `/send-otp`, static assets (`/css/**`, `/js/**`).
  - Passwords hashed using `BCryptPasswordEncoder`.

## 4. Promotional Discount Engine Architecture (KAN-13)
- **Validation Pipeline:**
  1. Case-insensitive lookup of active coupons (`CouponRepository.findByCodeIgnoreCaseAndIsActiveTrue`).
  2. Expiration verification against current server timestamp (`validUntil.isBefore(LocalDateTime.now())`).
  3. Per-coupon usage limit verification (`usedCount >= usageLimit`).
  4. Minimum booking spend threshold check (`totalAmount < minOrderAmount`).
  5. Discount recalculation:
     - `PERCENTAGE`: `totalAmount * (discountValue / 100.0)`
     - `FLAT`: `discountValue` clamped to totalAmount
  6. Final payable amount computation: `max(0.0, totalAmount - discountAmount)`.

- **Tamper-Proof Server-Side Verification:**
  - Client-side calculations in `book.html` provide immediate UI feedback.
  - During booking creation (`POST /user/bookings`), `UserController` delegates to `CouponService` to recalculate discounts from the database, preventing price manipulation.
  - Single coupon enforcement: Only one coupon can be applied per booking order.
  - Atomic usage tracking: When a booking is finalized, `usedCount` is incremented.

## 5. HTTP Status and Error Handling Conventions
- `200 OK`: Successful retrieval, validation, or creation with payload body.
- `400 Bad Request`: Validation failure (e.g., invalid/expired promo code, unmet minimum spend, missing seat numbers).
- `401 Unauthorized` / `403 Forbidden`: Authentication / authorization failures managed by Spring Security.
- `404 Not Found`: Resource lookup failures (e.g., show or movie not found).
- `500 Internal Server Error`: Unhandled infrastructure errors.
