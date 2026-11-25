# MovieMatic Architecture Documentation

## 1. Overview
MovieMatic is an enterprise-grade full-stack cinema ticketing platform built on Spring Boot 3, Spring Data JPA, Spring Security, and Thymeleaf with Bootstrap 5. It serves two primary user roles:
- **Admin**: Movie catalog management, show scheduling, platform booking oversight, and revenue analytics.
- **User**: Movie discovery, interactive theater seat selection, promotional coupon discounts, Razorpay payment processing, booking history, and PDF ticket generation with QR verification.

---

## 2. Layered Architecture

The application strictly adheres to the classical multi-tier architectural pattern:

```
[ Client Browser / Frontend (Thymeleaf, Bootstrap, JS) ]
                          │  HTTP / JSON / HTML
                          ▼
            [ Controller Layer (Web & REST) ]
    - AuthController, MovieController, ShowController
    - BookingController, UserController, CouponController
                          │
                          ▼
                  [ Service Layer ]
    - UserService, MovieService, ShowService, BookingService
    - CouponService, EmailService, orderServices
                          │
                          ▼
                [ Repository Layer ]
    - UserRepository, MovieRepository, ShowRepository
    - BookingRepository, CouponRepository
                          │
                          ▼
              [ Database / JPA Entities ]
    - User, Movie, Show, Booking, Coupon
```

### 2.1 Presentation & Controller Layer
- Implements Spring MVC `@Controller` for server-rendered views using Thymeleaf templates.
- Implements `@RestController` or `@ResponseBody` endpoints for AJAX and REST operations:
  - `POST /user/coupons/validate`: Real-time promo code validation.
  - `POST /user/bookings`: Transactional booking creation with server-recalculated discounts.
  - `GET /getTransaction/{amount}`: Razorpay order initialization.
  - `GET /user/shows/{showId}/occupied-seats`: Seat occupancy queries.

### 2.2 Service Layer
- Encapsulates business logic, transactional boundaries (`@Transactional`), and entity orchestration.
- **CouponService**:
  - Validates promo code existence, case-insensitivity, activation status, expiry dates, and usage limits.
  - Validates minimum order spend thresholds.
  - Calculates discounts for both percentage and flat rate discount strategies.
  - Enforces server-side recalculation during checkout to prevent client-side price tampering.

### 2.3 Persistence Layer
- Utilizes Spring Data JPA repositories extending `JpaRepository`.
- Supports parameterized queries, derived finder methods (`findByCodeIgnoreCaseAndIsActiveTrue`), and JPQL aggregations.

---

## 3. Security Architecture
- Spring Security 6 with `SecurityFilterChain` bean.
- Role-based access control (RBAC):
  - `/admin/**`: Restricted to `ROLE_ADMIN`.
  - `/user/**`: Restricted to `ROLE_USER`.
  - `/`, `/login`, `/register`, `/css/**`, `/js/**`, `/reset-password`, `/send-otp`: Publicly accessible.
- Passwords salted and hashed with `BCryptPasswordEncoder`.

---

## 4. Coupon & Promotional Discount Engine

### 4.1 Discount Types
- `PERCENTAGE`: Calculates `(totalAmount * discountValue) / 100.0`. Cannot exceed `totalAmount`.
- `FLAT`: Fixed rupee deduction `min(discountValue, totalAmount)`.

### 4.2 Security & Tamper Resistance
- Clients cannot supply arbitrary final prices. All calculations are performed deterministically on the server.
- The `POST /user/coupons/validate` endpoint provides preview validation.
- The final price is recomputed from the show seat price and validated coupon in `POST /user/bookings`.

### 4.3 HTTP Status Conventions
- `200 OK`: Valid operation, coupon successfully validated or booking created.
- `400 Bad Request`: Invalid or expired coupon code, or minimum spend criteria not met.
- `404 Not Found`: Target resource (movie, show, user) not found.
- `500 Internal Server Error`: Unexpected runtime failure.
