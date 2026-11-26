# Architecture Guide: MovieMatic

## Architectural Overview
MovieMatic follows the classic layered Spring Boot architecture, emphasizing separation of concerns, secure server-side domain validation, and idiomatic Spring MVC / REST conventions.

```
+-------------------------------------------------------------+
|                      Presentation Layer                     |
|  - Thymeleaf Templates (HTML5, Bootstrap 5, Razorpay SDK)   |
|  - REST & MVC Controllers (UserController, CouponController)|
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                        Service Layer                        |
|  - CouponService: discount calculation, quota & expiry check|
|  - BookingService: reservations, seat occupancy, PDF tickets|
|  - UserService, MovieService, ShowService, EmailService     |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                      Persistence Layer                      |
|  - Spring Data JPA Repositories (CouponRepository, etc.)    |
|  - Relational Database Entities (Coupon, Booking, Show, etc)|
+-------------------------------------------------------------+
```

---

## Layering Rules & Conventions

1. **Controllers (`com.MovieMatic.controller`):**
   - Controllers handle HTTP routing, request deserialization, session security extraction (`@AuthenticationPrincipal`), and response composition.
   - REST endpoints return typed DTOs or `ResponseEntity<?>` with explicit HTTP status codes (`200 OK`, `400 Bad Request`, `500 Internal Server Error`).
   - MVC handlers return view names (e.g., `user/book`, `user/my-bookings`).

2. **Services (`com.MovieMatic.service`):**
   - All business logic, price recalculations, and coupon eligibility checks reside in the service layer.
   - Methods modifying persistent state are annotated with `@Transactional`.
   - Logging uses SLF4J (`LoggerFactory.getLogger(...)`).

3. **Domain & Repositories (`com.MovieMatic.model`, `com.MovieMatic.repository`):**
   - JPA entities map directly to relational tables.
   - Repositories extend `JpaRepository` and leverage Spring Data method derivation (e.g. `findByCodeIgnoreCaseAndIsActiveTrue`).

4. **Exception Handling:**
   - Business rule violations throw domain-specific unchecked exceptions (e.g. `CouponException`).
   - Controllers catch `CouponException` and map to `400 Bad Request` with structured JSON bodies (`{ "message": "...", "valid": false }`).

---

## Security & Anti-Tampering
- All user checkout and coupon endpoints are protected by Spring Security (`ROLE_USER`).
- Discounts must ALWAYS be recalculated server-side during booking creation. Client amounts are treated as untrusted hints.
