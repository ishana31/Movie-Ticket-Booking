# MovieMatic Architecture & Engineering Standards

## 1. Architectural Layers & Responsibilities

The application follows idiomatic Spring Boot multi-tier architecture:

```
[ HTTP Clients / Browsers ]
            │
            ▼
   [ Controller Layer ]      <-- Request handling, authentication context, validation, DTO transformation
            │
            ▼
    [ Service Layer ]        <-- Domain transactions, business invariants, concurrency locking, scheduled tasks
            │
            ▼
   [ Repository Layer ]      <-- Spring Data JPA Repositories, SQL queries, transactional DB access
            │
            ▼
    [ Database / DB ]        <-- PostgreSQL / MySQL with unique constraints and transactional ACID semantics
```

- **Controller (`com.MovieMatic.controller`):**
  Handles HTTP requests, extracts `@AuthenticationPrincipal UserDetails`, transforms JSON / form data, and maps domain exceptions to standard HTTP status codes.
- **Service (`com.MovieMatic.service`):**
  Encapsulates core business transactions (`@Transactional`), synchronization locks, business rules, and scheduled tasks.
- **Repository (`com.MovieMatic.repository`):**
  Provides Spring Data JPA access with derived and `@Query` methods.
- **Model / Entity (`com.MovieMatic.model`):**
  JPA entities defining schema tables and relational constraints.
- **DTO (`com.MovieMatic.dto`):**
  Typed request and response payloads decoupled from internal persistence entities.

---

## 2. Concurrency Safety Model

To prevent double-booking race conditions when multiple users attempt to reserve the same seat simultaneously:
1. **Application-Level Synchronization:**
   `SeatReservationService` utilizes fine-grained per-show monitors (`ConcurrentHashMap<Long, Object> showLocks`). Concurrent hold and confirmation requests for the same show are serialized cleanly across JVM threads.
2. **Database Integrity Constraint:**
   `show_seats` enforces a unique constraint `uk_show_seat` on `(show_id, seat_number)`. Even in distributed multi-node scenarios, no two concurrent transactions can insert duplicate active records for the same seat.
3. **Atomic Multi-Seat Validation:**
   Before persisting holds, all requested seat numbers are evaluated against active holds and bookings. If any seat is unavailable, an immediate `SeatAlreadyOccupiedException` is thrown before any seat status is altered.

---

## 3. HTTP Status & Exception Handling Conventions

| HTTP Status | Scenario | Example Response Body |
|-------------|----------|-----------------------|
| `200 OK` | Successful hold, release, occupied seat retrieval, or booking | `{"status": "HELD", "expiresInSeconds": 600}` |
| `400 BAD REQUEST` | Empty seat parameters or invalid inputs | `{"message": "Seat numbers cannot be empty"}` |
| `401 UNAUTHORIZED` | Unauthenticated access to user endpoints | `{"message": "User must be authenticated"}` |
| `409 CONFLICT` | Seats already held by another user or already booked | `{"message": "Seats already held or booked"}` |
| `500 INTERNAL SERVER ERROR` | Unhandled database or operational failures | `{"message": "Failed to hold seats: ..."}` |

---

## 4. Background Scheduling
- Spring's `@EnableScheduling` is activated on `MovieMaticApplication`.
- `SeatReservationService.autoReleaseExpiredHolds()` executes every 60 seconds (`@Scheduled(fixedRate = 60000)`) to release holds older than 10 minutes (`held_at <= now - 10 minutes`).
