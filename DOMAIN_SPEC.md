# MovieMatic Domain Specification: Seat Reservation & Hold System (KAN-9)

## 1. Domain Entities & Value Objects

### 1.1 `ShowSeat` Entity
Table: `show_seats`
Unique Constraint: `uk_show_seat(show_id, seat_number)`

| Column Name | Type | Nullable | Description |
|---|---|---|---|
| `id` | `BIGINT` | No | Primary Key (auto-increment) |
| `show_id` | `BIGINT` | No | Foreign Key to `Show` entity |
| `seat_number` | `VARCHAR` | No | Identifier of the seat (e.g., "A1", "B2") |
| `status` | `VARCHAR` | No | Current seat status: `AVAILABLE`, `HELD`, `BOOKED` |
| `held_at` | `TIMESTAMP` | Yes | Timestamp when temporary hold was acquired |
| `held_by_user_id`| `BIGINT` | Yes | ID of user who placed the temporary hold |

### 1.2 `SeatStatus` Enum
- `AVAILABLE`: Seat is open for selection and holding.
- `HELD`: Seat is temporarily locked by a customer for 10 minutes (countdown timer active).
- `BOOKED`: Seat is permanently booked after payment confirmation.

---

## 2. Business Rules & Invariants

1. **Atomic Reservation**:
   - All requested seats must be available to place a hold.
   - If any single seat in the batch is booked or actively held by another user, the entire request fails immediately with HTTP 409 Conflict (`"Seats already held or booked"`). No partial holds are persisted.

2. **Single Owner Rule**:
   - Only the specific user who placed the hold (`held_by_user_id`) can proceed to checkout and confirm the purchase.
   - Other users attempting to confirm seats held by someone else receive HTTP 409 Conflict.

3. **Temporary Hold Timeout (TTL)**:
   - Holds are valid for exactly 10 minutes (`600` seconds) from `held_at`.
   - Any query for occupied seats or hold validation treats seats with `held_at < now - 10 minutes` as expired / AVAILABLE.

4. **Automatic Background Release**:
   - An auto-release background scheduler runs every 60 seconds (`@Scheduled(fixedRate = 60000)`).
   - Any seat in `HELD` status whose `held_at` timestamp exceeds the 10-minute TTL is reverted to `AVAILABLE` status with `held_at = null` and `held_by_user_id = null`.

5. **Payment Completion Transition**:
   - Upon successful booking/payment, the seats permanently transition from `HELD` to `BOOKED`.
   - Booked seats cannot be released by the TTL cleaner.

---

## 3. UI Representation (book.html)

- **Available Seats**: Displayed in **Green** (`#28a745`), interactive and selectable.
- **Held Seats**: Displayed in **Orange** (`#fd7e14`), disabled with `not-allowed` cursor for other users.
- **Booked Seats**: Displayed in **Red** (`#dc3545`), disabled.
- **Countdown Modal**: Displays `Complete booking within 09:59 or seats will be released` during checkout.
