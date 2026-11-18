# Architecture Specification: MovieMatic

## 1. Architectural Overview & Layering

MovieMatic follows a classic 4-layer Spring Boot architectural pattern:

```
[ Frontend: Thymeleaf + Bootstrap 5 + jQuery ]
                      │
                      ▼
[ Presentation Layer: Controllers (@Controller / @RestController) ]
  - CouponController (/user/coupons/validate)
  - UserController (/user/bookings, /user/movie-details/shows/{id}/book)
  - BookingController (/admin/bookings)
                      │
                      ▼
[ Service Layer: Business Logic & Orchestration ]
  - CouponService (Validation, Discount Calculations, Usage Tracking)
  - BookingService (Seat Reservations, Persistence, Ticket PDFs)
                      │
                      ▼
[ Persistence Layer: Spring Data JPA Repositories ]
  - CouponRepository (findByCodeIgnoreCaseAndIsActiveTrue)
  - BookingRepository (save, findByUser, findByShow)
                      │
                      ▼
[ Database Layer: PostgreSQL / MySQL ]
  - coupons, bookings, shows, movies, users
```

---

## 2. Coupon & Promotional Discount Engine Design

### 2.1 Validation & Calculation Pipeline
1. **Entry Point:** Client invokes `POST /user/coupons/validate` with code and order total.
2. **Repository Lookup:** Case-insensitive search on active coupons.
3. **Guard Clauses:**
   - Active status check.
   - Temporal expiration check (`validUntil.isBefore(now)`).
   - Usage limit exhaustion check (`usedCount >= usageLimit`).
   - Minimum spend threshold check (`totalAmount < minOrderAmount`).
4. **Discount Calculation:** Calculates percentage or flat deduction, clamps against maximum allowed total, and rounds to two decimal points.
5. **Checkout & Persistence:** During `POST /user/bookings`, the server re-executes the calculation against server-derived seat totals, increments `used_count` within a `@Transactional` boundary, and persists `coupon_code` and `discount_amount` on `Booking`.

---

## 3. Security & Conventions

- **Authentication & Authorization:** All user checkout endpoints (`/user/**`) require authentication under `ROLE_USER`.
- **Tamper Prevention:** Pricing is computed strictly server-side from `Show.price * seats.length`.
- **Response Format:** JSON responses adhere to explicit DTO schemas (`CouponValidationResponse`, `BookingResponse`).
- **HTTP Status Codes:**
  - `200 OK`: Valid operation.
  - `400 Bad Request`: Validation failure (expired code, unmet minimum spend, bad input).
  - `401 Unauthorized`: Unauthenticated request.
  - `500 Internal Server Error`: Unexpected server exception.
