# MovieMatic Domain Specification

## Domain Overview
MovieMatic is a full-stack movie ticketing platform supporting seat reservation, temporary holds with TTL auto-release, payment integration, and ticket fulfillment.

---

## 1. Core Domain Entities

### 1.1 ShowSeat
Represents the status and reservation lifecycle of an individual seat for a specific movie show.

- **Table**: `show_seats`
- **Fields**:
  - `id` (Long, PK): Unique identifier.
  - `show_id` (Long, FK -> Show): Associated show.
  - `seat_number` (String): Seat label (e.g., `A1`, `B4`).
  - `status` (SeatStatus): Enum representing seat state:
    - `AVAILABLE`: Seat is free for selection.
    - `HELD`: Seat is temporarily locked by a customer during checkout.
    - `BOOKED`: Seat is permanently purchased and confirmed.
  - `held_at` (LocalDateTime): Timestamp when temporary hold was placed.
  - `held_by_user_id` (Long): User identifier of the customer holding the seat.
- **Constraints**:
  - Unique constraint on `(show_id, seat_number)` to prevent duplicate seat allocations.

### 1.2 Show
Represents a scheduled screening of a movie.
- Fields: `id`, `movie` (FK), `showDate`, `showTime`, `screen`, `price`.

### 1.3 Booking
Represents a confirmed customer reservation.
- Fields: `id`, `user` (FK), `show` (FK), `seatNumbers`, `totalAmount`, `bookingTime`.

---

## 2. Business Rules & Invariants

### 2.1 Temporary Seat Hold & TTL (10 Minutes)
- When a user selects seats and confirms hold, selected seats immediately transition from `AVAILABLE` to `HELD`.
- Hold TTL is strictly 10 minutes (`600 seconds`).
- Expired holds (`held_at < NOW - 10 minutes`) are automatically invalid and will revert to `AVAILABLE`.
- Background scheduler `@Scheduled(fixedRate = 60000)` runs every 60 seconds to clean up expired holds.

### 2.2 Atomic Reservation
- All requested seats must be available simultaneously.
- If even one requested seat is already held or booked, the entire hold request is rejected with `409 Conflict`, and no seats are modified.

### 2.3 Single Owner Invariant
- Only the specific customer who placed the temporary hold can complete payment and confirm the purchase.
- Another customer attempting to complete purchase on held seats receives `409 Conflict`.

### 2.4 State Transitions
```
[AVAILABLE] 
     │
     │ (User clicks Confirm Booking -> POST /user/shows/{showId}/hold)
     ▼
  [HELD] ──── (10-min TTL expires or cancelled) ────► [AVAILABLE]
     │
     │ (Payment Successful -> POST /user/bookings)
     ▼
 [BOOKED] (Permanent)
```

---

## 3. UI Status Colors
- **Available**: Green (`#28a745`) - Selectable by user.
- **Held**: Yellow/Orange (`#fd7e14`) - Active hold with timer, disabled for other users.
- **Booked**: Red/Gray (`#dc3545`) - Permanently reserved, non-clickable.
