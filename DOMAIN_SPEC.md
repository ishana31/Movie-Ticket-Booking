# MovieMatic Domain Specification

## 1. Domain Entities & Invariants

### 1.1 `ShowSeat`
Represents the real-time reservation state of a seat in a given movie show.
- **Table:** `show_seats`
- **Fields:**
  - `id`: Unique identifier (Primary Key, Long).
  - `show_id`: Associated `Show` (ManyToOne, Not Null).
  - `seat_number`: Alphanumeric seat designation, e.g., "A1", "B4" (VARCHAR, Not Null).
  - `status`: Current lifecycle state (`AVAILABLE`, `HELD`, `BOOKED`) (Enum VARCHAR, Not Null).
  - `held_at`: Timestamp when the hold was established (LocalDateTime, Nullable).
  - `held_by_user_id`: ID of the user holding the seat (Long, Nullable).
- **Constraints:**
  - Unique Constraint `uk_show_seat` on `(show_id, seat_number)` guarantees only one record per seat per show.

### 1.2 `SeatStatus` Lifecycle Invariants
1. **AVAILABLE:**
   - Seat is free for any authenticated user to hold.
   - `held_at` is `null`, `held_by_user_id` is `null`.
2. **HELD:**
   - Temporarily held by a single user (`held_by_user_id`).
   - Time-to-Live (TTL): Exactly 10 minutes (`600 seconds`) from `held_at`.
   - Cannot be held or purchased by any other customer while active (`held_at > now - 10 min`).
   - Can be renewed by the holding user if re-selected before expiration.
3. **BOOKED:**
   - Permanently purchased upon payment/checkout completion.
   - Terminal status (cannot be reverted to AVAILABLE or HELD except upon formal admin refund/cancellation).

---

## 2. Business Rules & Validations

### 2.1 Atomic Seat Hold
- When a user requests to hold multiple seats (e.g., `A1,A2`), all requested seats must be available.
- If even a single seat is currently booked or held by another user, the reservation fails entirely. None of the seats are held.

### 2.2 Single Owner Principle
- Only the user whose `user_id` matches `held_by_user_id` on an active hold may proceed to complete payment and convert the seats to `BOOKED`.
- If an unheld seat or seat held by another user is submitted to `/user/bookings`, the operation is rejected with `409 Conflict`.

### 2.3 Automatic Release (TTL & Background Cleanup)
- **Time-to-Live (TTL):** 10 minutes (600 seconds).
- **Real-Time Expiration:** Any query or hold operation treating seats older than `now - 10 minutes` as expired.
- **Background Task:** `@Scheduled(fixedRate = 60000)` runs every 60 seconds to reset any `ShowSeat` rows with `status = HELD` and `held_at <= now - 10 minutes` back to `AVAILABLE`.
- **Manual Abandonment:** Users closing checkout or clicking Cancel invoke `/user/shows/{showId}/release-hold` to immediately return seats to AVAILABLE.

---

## 3. UI Representation Guidelines
- **Available Seats:** Distinct Green border/background (`#28a745`), interactive on click.
- **Held Seats:** Distinct Orange background (`#fd7e14`), non-clickable, disabled with title indicating reservation.
- **Booked Seats:** Distinct Red background (`#dc3545`), non-clickable, disabled.
- **Selected Seats:** Primary gradient glow (`#ff3366` to `#ff6b3d`).
- **Countdown Timer:** Prominently displayed modal alert showing "Complete booking within MM:SS or seats will be released".
