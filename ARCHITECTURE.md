# MovieMatic Application Architecture

## 1. Overview
MovieMatic is a full-stack movie ticket booking web application built with **Spring Boot 3.5**, **Spring Data JPA**, **Spring Security**, and **Thymeleaf + Bootstrap 5**.

The platform supports two user roles:
- **Admin**: Movie management, show scheduling, and overall platform administration.
- **User**: Movie discovery, interactive seat booking, coupon/promo discounts, Razorpay checkout, and PDF ticketing.

---

## 2. Layering Architecture

```
+-----------------------------------------------------------+
|                      Presentation                         |
|   Thymeleaf HTML5 + Bootstrap 5 + jQuery + Razorpay JS     |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
|                   Controller Layer                        |
|  - AuthController: Login, registration, landing page       |
|  - UserController: Discovery, show selection, booking     |
|  - CouponController: Coupon validation REST endpoint       |
|  - BookingController (Admin): Booking oversight            |
|  - MovieController (Admin): Movie catalog management       |
|  - ShowController (Admin): Show scheduling                 |
|  - paymentTransactionController: Razorpay transaction init |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
|                     Service Layer                         |
|  - CouponService: Promotional discount calculations,       |
|                   validity & threshold enforcement         |
|  - BookingService: Seat booking management & PDF rendering|
|  - UserService / MovieService / ShowService / EmailService|
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
|                   Repository Layer                        |
|  - CouponRepository, BookingRepository, ShowRepository,   |
|    MovieRepository, UserRepository                        |
+-----------------------------------------------------------+
                             |
                             v
+-----------------------------------------------------------+
|                    Persistence (DB)                       |
|  PostgreSQL / MySQL tables: coupons, bookings, shows,      |
|                             movies, users                 |
+-----------------------------------------------------------+
```

---

## 3. Security & Access Control Conventions
- Handled via Spring Security `SecurityConfig`.
- Role-based authorization:
  - `/admin/**` -> `ROLE_ADMIN`
  - `/user/**` -> `ROLE_USER`
  - `/`, `/login`, `/register`, `/reset-password`, `/send-otp`, `/css/**`, `/js/**` -> `permitAll()`
- CSRF is disabled for API and transaction interactions.
- Passwords hashed with `BCryptPasswordEncoder`.

---

## 4. REST & HTTP Status Conventions
- `200 OK`: Successful retrieval, validation, or resource processing.
- `400 Bad Request`: Validation failure (e.g., invalid coupon code, expired promo, unmet minimum order amount, or missing parameters).
- `401 Unauthorized` / `403 Forbidden`: Unauthenticated access or role violation.
- `404 Not Found`: Resource does not exist.
- `500 Internal Server Error`: Unhandled server-side processing error.

---

## 5. Coupon & Promotional Discount Engine Architecture (KAN-13)

### Core Components
1. **Model (`Coupon`)**: Represents promotional coupons in table `coupons`.
   - Fields: `id`, `code` (UNIQUE), `discountType` (`PERCENTAGE`, `FLAT`), `discountValue`, `minOrderAmount`, `validUntil`, `usageLimit`, `usedCount`, `isActive`.
2. **Model (`Booking`)**: Extended with `couponCode` and `discountAmount`.
3. **Repository (`CouponRepository`)**:
   - `findByCodeIgnoreCaseAndIsActiveTrue(String code)`: Case-insensitive active coupon retrieval.
4. **Service (`CouponService`)**:
   - Encapsulates business validation: active status, expiration timestamp, usage limits, minimum order spend thresholds.
   - Calculates discount server-side based on `PERCENTAGE` or `FLAT` discount types.
   - Caps discount to order total.
   - Increments `usedCount` atomically upon booking confirmation.
5. **REST API**:
   - `POST /user/coupons/validate`: Pre-checkout coupon validation for real-time frontend price preview.
   - `POST /user/bookings`: Final booking creation recalculating coupon discount server-side to guarantee tampering prevention.
