# MovieMatic Architecture: Concurrency-Safe Seat Reservation System (KAN-9)

## 1. Architectural Overview

The Seat Reservation System introduces high-concurrency seat locking with temporary holds, TTL auto-release, and real-time state synchronization between concurrent users.

```
[ Browser 1 (User 1) ]        [ Browser 2 (User 2) ]
         │                              │
         ▼                              ▼
  POST /hold                     POST /hold
         │                              │
         └─────────────┬────────────────┘
                       ▼
            [ UserController ]
                       │
                       ▼
        [ SeatReservationService ]
        (ReentrantLock per ShowId)
                       │
           ┌───────────┴───────────┐
           ▼                       ▼
  [ ShowSeatRepository ]  [ BookingRepository ]
           │                       │
           └───────────┬───────────┘
                       ▼
           [ show_seats (DB Table) ]
       (UK: show_id + seat_number)
```

---

## 2. Layered Responsibilities

### 2.1 Controller Layer (`com.MovieMatic.controller`)
- **`UserController`**:
  - `POST /user/shows/{showId}/hold`: Acquires temporary 10-minute hold on seat batch.
  - `GET /user/shows/{showId}/occupied-seats`: Returns `{ bookedSeats: [...], heldSeats: [...] }`.
  - `GET /user/book?showId={showId}`: Direct seat selection view by parameter.
  - `POST /user/bookings`: Validates hold ownership before persisting final booking and marking seats `BOOKED`.

### 2.2 Service Layer (`com.MovieMatic.service`)
- **`SeatReservationService`**:
  - Thread-safe coordination using `ConcurrentHashMap<Long, ReentrantLock>` to serialize seat reservations per show without contention across unrelated shows.
  - Enforces atomic reservation: either all seats are secured or none are.
  - Enforces Single Owner validation during booking confirmation.
  - Evaluates 600-second TTL dynamically during checks.
  - Background `@Scheduled(fixedRate = 60000)` auto-release task reclaiming abandoned holds.
- **`BookingService`**:
  - Coordinates PDF ticket creation and booking history.
  - Queries both `Booking` records and `ShowSeat` entities to compute occupied seats.

### 2.3 Repository Layer (`com.MovieMatic.repository`)
- **`ShowSeatRepository`**:
  - Spring Data JPA repository for `ShowSeat` entity.
  - Queries active holds (`findByShowIdAndStatus`, `findActiveHoldsByShowId`).
  - Queries expired holds (`findByStatusAndHeldAtBefore`).
  - Cascading deletion cleanup on show deletion (`deleteByShowId`).

### 2.4 Data Model Layer (`com.MovieMatic.model`)
- **`ShowSeat`**:
  - Columns: `id`, `show_id`, `seat_number`, `status`, `held_at`, `held_by_user_id`.
  - Database constraint: `@UniqueConstraint(name = "uk_show_seat", columnNames = {"show_id", "seat_number"})`.
- **`SeatStatus`**:
  - `AVAILABLE`, `HELD`, `BOOKED`.

---

## 3. Concurrency & Safety Guarantee

1. **JVM-Level Lock**:
   - `ReentrantLock` keyed by `showId` ensures two requests for the same show are evaluated serially, preventing race conditions between check and update operations.
2. **Database-Level Constraint**:
   - Unique constraint `uk_show_seat(show_id, seat_number)` guarantees database-level integrity against concurrent inserts.
3. **Transaction Isolation**:
   - Methods are annotated with `@Transactional` to ensure changes to multiple seats commit or roll back together.
