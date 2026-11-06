# MovieMatic Domain Specification

## 1. Booking Domain Model

### 1.1 Booking Entity & Lifecycle
The `Booking` entity represents a customer reservation for one or more seats in a movie show.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Primary Key (auto-incremented) |
| `user` | `User` | User who made the booking |
| `show` | `Show` | Show session being booked |
| `seatNumbers` | `String` | Comma-delimited list of seats (e.g. `A1,A2`) |
| `totalAmount` | `double` | Total payment amount for the booking |
| `bookingTime` | `LocalDateTime` | Timestamp when the booking was initiated |
| `status` | `String` | Status: `CONFIRMED` or `CANCELLED` (default: `CONFIRMED`) |
| `cancelledAt` | `LocalDateTime` | Timestamp when the booking was cancelled |
| `refundAmount` | `Double` | Refund amount credited to the customer |
| `cancellationFee` | `Double` | Cancellation deduction retained |
| `refundStatus` | `String` | Refund processing state (e.g. `REFUNDED`) |

### 1.2 Invariants & Business Rules
1. **Record Retention**: Booking records must **never** be deleted upon customer cancellation. Status is updated to `CANCELLED`, preserving historical booking and accounting data.
2. **Access Control**: Users may only preview or cancel bookings belonging directly to their authenticated account. Unauthorized requests return `403 Forbidden`.
3. **Cancellation Idempotency**:
   - Previewing cancellation on an already cancelled booking returns `400 Bad Request`.
   - Executing cancellation on an already cancelled booking returns `409 Conflict`.
4. **Automatic Seat Release**:
   - When a booking is marked `CANCELLED`, its seat numbers are immediately excluded from `BookingService.getOccupiedSeats(show)`.
   - Released seats become available immediately for reservation on the seat selection UI (`/user/movie-details/shows/{id}/book` and `/user/shows/{showId}/occupied-seats`).

---

## 2. Tiered Refund Policy

Refund amounts and cancellation fees are calculated relative to the remaining duration before the show start time (`showDate` + `showTime`):

| Timing Condition | Refund % | Cancellation Fee % | Eligibility |
|---|---|---|---|
| **> 4 hours** before show start | 90% | 10% | Eligible (`200 OK`) |
| **Between 1 and 4 hours** before show start (inclusive) | 50% | 50% | Eligible (`200 OK`) |
| **< 1 hour** before show start | 0% | 100% | Ineligible (`400 Bad Request`) |
| **Show already started / passed** | 0% | 100% | Ineligible (`400 Bad Request`) |

### Mathematical Formula
- `totalAmount` = Show price × Number of seats
- `refundAmount` = `roundToTwoDecimals(totalAmount * (refundPercentage / 100))`
- `cancellationFee` = `roundToTwoDecimals(totalAmount - refundAmount)`
