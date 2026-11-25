# MovieMatic Architecture & Engineering Specification

## 1. Architectural Overview

MovieMatic is a multi-tier Spring Boot 3 web application combining server-rendered views (Thymeleaf + Bootstrap 5) with RESTful JSON endpoints for dynamic checkout, payment processing, and seat reservation workflows.

### High-Level Layers
```
+-------------------------------------------------------------+
|                     Client Layer (Browser)                  |
| Thymeleaf Templates (HTML5 / Bootstrap 5 / jQuery / Razorpay)|
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     Presentation Layer                      |
| Controllers (@Controller / @RestController):                |
| - UserController (/user/**)                                 |
| - BookingController (/admin/**)                             |
| - MovieController (/admin/**)                               |
| - ShowController (/admin/**)                                |
| - AuthController (/login, /register, /logout)               |
| - paymentTransactionController (/getTransaction/{amount})   |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                       Service Layer                         |
| Business Rules, Calculations, External Gateway Integrations |
| - CouponService                                             |
| - BookingService                                            |
| - MovieService                                              |
| - ShowService                                               |
| - UserService                                               |
| - EmailService                                              |
| - orderServices (Razorpay Integration)                      |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                     Data Access Layer                       |
| Spring Data JPA Repositories:                               |
| - CouponRepository                                          |
| - BookingRepository                                         |
| - ShowRepository                                            |
| - MovieRepository                                           |
| - UserRepository                                            |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      Persistence Layer                      |
| PostgreSQL / MySQL Database via Hibernate ORM               |
+-------------------------------------------------------------+
```

---

## 2. Coupon & Promotional Discount Engine Design

### 2.1 Core Architectural Principles
1. **Server-Side Price Authority**: Discounts are never trusted from the client. While client-side AJAX validates promo codes for real-time UI feedback, the server completely recalculates discounts from the ground truth database show price and seat count during booking submission.
2. **Atomic Usage Tracking**: Coupons with configured `usage_limit` have their `used_count` incremented within the booking transaction.
3. **Single Coupon Per Order**: Only one coupon can be applied per checkout operation.
4. **Case-Insensitive Resolution**: Coupon codes are normalized and matched case-insensitively using repository queries (`findByCodeIgnoreCaseAndIsActiveTrue`).

### 2.2 Discount Models
- **PERCENTAGE**: Discount is computed as `(totalAmount * discountValue) / 100.0`.
- **FLAT**: Discount is a flat currency amount `discountValue`.
- In all cases, discount is capped at `totalAmount` (`Math.min(discount, totalAmount)`), ensuring final payable amount is non-negative.

### 2.3 Exception & Error Conventions
- Unmet business rules in coupon operations throw `CouponValidationException`.
- Controllers map this exception to HTTP `400 Bad Request` returning a consistent JSON error schema:
  ```json
  {
    "message": "Invalid or expired coupon code"
  }
  ```
- Minimum booking amount failures return:
  ```json
  {
    "message": "Minimum booking amount of ₹300.00 required."
  }
  ```

---

## 3. HTTP Status Conventions

| Status Code | Usage in MovieMatic |
|-------------|---------------------|
| `200 OK` | Successful retrieval, successful coupon validation, successful booking creation. |
| `400 Bad Request` | Missing parameters, invalid coupon code, expired coupon, unmet minimum spend. |
| `401 Unauthorized` | Unauthenticated access attempt to protected user/admin endpoints. |
| `403 Forbidden` | Authenticated user lacking the required role (`ROLE_ADMIN` vs `ROLE_USER`). |
| `404 Not Found` | Requested resource (Movie, Show, Booking, Image) does not exist. |
| `500 Internal Server Error` | Unexpected runtime failures during transaction processing. |

---

## 4. Security Configuration

- Public Routes: `/`, `/login`, `/register`, `/reset-password`, `/send-otp`, `/css/**`, `/js/**`.
- User Routes: `/user/**` requires `ROLE_USER`. Includes `/user/coupons/validate` and `/user/bookings`.
- Admin Routes: `/admin/**` requires `ROLE_ADMIN`.
- CSRF: Disabled in development configuration for REST/AJAX compatibility with frontend jQuery calls.
