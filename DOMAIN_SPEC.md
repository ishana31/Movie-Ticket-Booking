# MovieMatic Domain Specification

## 1. Domain Entities & Invariants

### 1.1 `Coupon` Entity (`coupons`)
Represents promotional codes that grant percentage or flat monetary discounts on ticket purchases.

| Field | Type | DB Column | Constraints | Description |
|-------|------|-----------|-------------|-------------|
| `id` | `Long` | `id` | PK, Auto Increment | Unique coupon identifier |
| `code` | `String` | `code` | UNIQUE, NOT NULL | Alphanumeric coupon code (e.g. `SAVE20`) |
| `discountType` | `String` | `discount_type` | NOT NULL | `PERCENTAGE` or `FLAT` |
| `discountValue` | `double` | `discount_value` | NOT NULL, > 0 | Percentage value (e.g. `20.0` for 20%) or flat amount in ₹ |
| `minOrderAmount` | `double` | `min_order_amount` | Default `0.0` | Minimum gross booking total required to apply coupon |
| `validUntil` | `LocalDateTime` | `valid_until` | Nullable | Expiration timestamp; coupon expired if `now() > validUntil` |
| `usageLimit` | `Integer` | `usage_limit` | Nullable | Maximum total redemptions permitted across platform |
| `usedCount` | `int` | `used_count` | NOT NULL, Default `0` | Running counter of total completed redemptions |
| `isActive` | `boolean` | `is_active` | NOT NULL, Default `true` | Administrative toggle to enable/disable coupon |

#### Coupon Invariants & Business Rules
1. **Case-Insensitive Uniqueness**: `SAVE20`, `save20`, and `Save20` refer to the identical coupon. Lookups use case-insensitive queries.
2. **Active Status**: Inactive coupons (`isActive = false`) cannot be applied or validated.
3. **Validity Window**: If `validUntil` is defined and `LocalDateTime.now().isAfter(validUntil)`, the coupon is expired.
4. **Usage Limit**: If `usageLimit != null` and `usedCount >= usageLimit`, redemptions are rejected with `"Invalid or expired coupon code"`.
5. **Minimum Spend Rule**: If `minOrderAmount > 0` and booking gross amount is strictly less than `minOrderAmount`, redemptions are rejected with `"Minimum booking amount of ₹<amount> required."`.
6. **Maximum Discount Rule**: The calculated discount must never exceed the total order amount (`discount <= totalAmount`).
7. **Single Coupon Usage**: Exactly zero or one coupon can be applied per booking transaction.

---

### 1.2 `Booking` Entity (`bookings`)
Represents an individual confirmed ticket reservation made by a user.

| Field | Type | DB Column | Constraints | Description |
|-------|------|-----------|-------------|-------------|
| `id` | `Long` | `id` | PK, Auto Increment | Unique booking identifier |
| `user` | `User` | `user_id` | FK, NOT NULL | User who initiated the reservation |
| `show` | `Show` | `show_id` | FK, NOT NULL | Show scheduled for this booking |
| `seatNumbers` | `String` | `seat_numbers` | NOT NULL | Comma-separated list of selected seats (e.g. `A1,A2`) |
| `totalAmount` | `double` | `total_amount` | NOT NULL, >= 0 | Final payable / charged monetary amount (after discount) |
| `couponCode` | `String` | `coupon_code` | Nullable | Canonical promo code applied to this booking |
| `discountAmount` | `double` | `discount_amount` | Default `0.0` | Monetary discount deducted from gross price |
| `bookingTime` | `LocalDateTime` | `booking_time` | NOT NULL | Timestamp when booking was created |

---

### 1.3 `Show` Entity (`show`)
Represents an individual scheduled screening of a movie.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | `Long` | PK | Show ID |
| `movie` | `Movie` | FK | Associated movie entity |
| `showDate` | `LocalDate` | NOT NULL | Date of screening |
| `showTime` | `LocalTime` | NOT NULL | Time of screening |
| `screen` | `String` | NOT NULL | Screen / Auditorium name |
| `price` | `double` | NOT NULL | Base price per seat in ₹ |

---

## 2. Coupon Validation & Booking Workflow

```
1. User Selects Seats:
   - Gross Amount = show.price * number_of_seats

2. User Enters Promo Code in Modal & Clicks "Apply":
   - Client sends POST /user/coupons/validate { code, totalAmount }
   - Server validates:
       * Code exists and isActive == true
       * now <= validUntil
       * usedCount < usageLimit
       * grossAmount >= minOrderAmount
   - If invalid: Server responds 400 with { message: "Invalid or expired coupon code" }
   - If under threshold: Server responds 400 with { message: "Minimum booking amount of ₹300.00 required." }
   - If valid: Server responds 200 with { valid: true, discountAmount: X, finalAmount: Y }
   - Client UI displays: "Coupon '<CODE>' applied! You saved ₹<discountAmount>" and updates payable total.

3. User Clicks "Proceed to Payment":
   - Client sends POST /user/bookings { showId, seatNumbers, couponCode }
   - Server re-validates coupon and recalculates discount server-side.
   - Server creates Booking entity with final amount and coupon code.
   - Server increments coupon.usedCount.
   - Server responds 200 with { bookingId, totalAmount, discountAmount }.
   - Client initiates Razorpay payment for server-confirmed totalAmount.
```
