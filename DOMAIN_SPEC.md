# MovieMatic Domain Specification

## 1. Domain Entities & Invariants

### 1.1 Coupon (`coupons`)
Represents promotional codes applied by users during the booking checkout flow.

| Field | Type | Invariant / Validation Rule |
|---|---|---|
| `id` | Long | Primary Key, Auto-increment. |
| `code` | String | Unique, non-null, case-insensitive match (e.g. `save20` matches `SAVE20`). |
| `discount_type` | Enum (`PERCENTAGE`, `FLAT`) | Required. Percentage calculates `(total * value) / 100`, Flat deducts absolute value. |
| `discount_value` | double | Positive numeric value. |
| `min_order_amount` | double | Optional minimum order amount (if > 0, booking total before discount must be >= threshold). |
| `valid_until` | LocalDateTime | Expiration timestamp. If non-null and before `now()`, the coupon is expired. |
| `usage_limit` | Integer | Maximum allowable redemptions across all users. If `used_count >= usage_limit`, coupon is rejected. |
| `used_count` | int | Total number of times this coupon has been used in confirmed bookings. Default `0`. |
| `is_active` | boolean | Toggle for coupon availability. Default `true`. |

#### Coupon Business Rules:
- **Case-Insensitive:** Lookup and validation normalize codes case-insensitively.
- **Server-Side Recalculation:** All discount amounts and final payable sums MUST be calculated server-side during booking creation (`POST /user/bookings`) to prevent client-side price tampering.
- **Single Coupon:** Exactly one promotional coupon may be applied per booking transaction.
- **Capping:** Discount amount cannot exceed the order total (final amount cannot be negative).
- **Threshold Failure Message:** When order amount is less than `min_order_amount`, returns:
  `"Minimum booking amount of ₹<formatted_amount> required."` (e.g., `Minimum booking amount of ₹300.00 required.`).
- **Generic Invalidation Message:** When code is missing, not found, inactive, expired, or exceeds `usage_limit`, returns:
  `"Invalid or expired coupon code"`.

---

### 1.2 Booking (`bookings`)
Represents a reserved cinema seat order.

| Field | Type | Invariant / Description |
|---|---|---|
| `id` | Long | Primary Key, Auto-increment. |
| `user` | User | Customer who placed the booking. |
| `show` | Show | The scheduled cinema show. |
| `seatNumbers` | String | Comma-delimited list of seat numbers (e.g., `"A1,A2"`). |
| `totalAmount` | double | Final payable amount after promotional discounts. |
| `couponCode` | String | Uppercase coupon code applied, or `null`. |
| `discountAmount` | double | Deduction amount applied to the booking. Defaults to `0.0`. |
| `bookingTime` | LocalDateTime | Timestamp when the booking was persisted. |

---

### 1.3 Show (`Show`)
- Linked to a `Movie`.
- Attributes: `showDate`, `showTime`, `screen`, `price`.
- Seat availability is tracked by inspecting occupied seats across bookings for that show.

---

### 1.4 Movie (`Movie`)
- Attributes: `title`, `description`, `genre`, `language`, `duration`, `releaseDate`, `posterData`, `backgroundData`.

---

### 1.5 User (`users`)
- Attributes: `name`, `email` (unique), `password` (BCrypt encoded), `role` (`ROLE_USER` or `ROLE_ADMIN`), `phone`, `profileImage`.
