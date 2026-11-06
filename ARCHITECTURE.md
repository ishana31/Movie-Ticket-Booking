# MovieMatic Architectural Architecture

## Architecture Overview
MovieMatic follows a layered Spring Boot architecture comprising Controller, Service, Repository, and Model layers, with Thymeleaf frontend and PostgreSQL / MySQL persistence.

---

## 1. Concurrency-Safe Seat Reservation System (KAN-11)

### 1.1 Architectural Layers

1. **Presentation / Web Layer**:
   - `UserController`: Exposes `/user/shows/{showId}/hold`, `/user/shows/{showId}/occupied-seats`, and handles booking orchestration.
   - `book.html`: Interactive seat grid supporting 3 distinct status colors (Green = Available, Orange = Held, Red = Booked), a ticking 10-minute countdown timer modal, and real-time conflict alerts.

2. **Service Layer**:
   - `SeatReservationService`:
     - Implements per-show transactional locking (`ReentrantLock` & `@Transactional`) to serialize concurrent seat hold requests.
     - Validates atomic reservations across requested seat batches.
     - Enforces the Single Owner invariant.
     - Background scheduled task (`@Scheduled(fixedRate = 60000)`) to auto-release expired holds.
   - `BookingService`: Manages confirmed bookings and integrates with PDF generation.

3. **Repository Layer**:
   - `ShowSeatRepository`: JPA repository with custom queries for active holds, expired holds, and bulk status updates.
   - `BookingRepository`: Queries existing bookings.

4. **Domain Layer**:
   - `ShowSeat`: Entity mapped to `show_seats` table with unique constraint `(show_id, seat_number)`.
   - `SeatStatus`: Enum `AVAILABLE`, `HELD`, `BOOKED`.

---

## 2. Concurrency Controls & Thread Safety

- **In-Memory Locking**: A thread-safe `ConcurrentHashMap<Long, ReentrantLock>` maintains fair reentrant locks per `showId`. This guarantees that concurrent web requests attempting to hold or confirm seats on the same show are evaluated serially without race conditions.
- **Database Unique Constraint**: A table-level constraint `uk_show_seat(show_id, seat_number)` guarantees database-level integrity against race conditions across distributed instances.
- **Atomic Operations**: All seat status checks and writes are executed inside `@Transactional` boundaries, ensuring either all requested seats are held or none are.

---

## 3. Background Task & Auto-Release Lifecycle

- `@EnableScheduling` enabled on `MovieMaticApplication`.
- `releaseExpiredHolds()` executes every 60,000 ms.
- Reverts seats with status `HELD` and `held_at < (now - 600s)` back to `AVAILABLE`.
- Read queries automatically filter out expired holds even before the background job runs.
