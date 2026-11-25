# MovieMatic Domain Specification & Business Rules

## 1. Core Domain Entities

### Coupon (`coupons`)
Represents a promotional discount code applicable during the booking checkout flow.
- `id` (BIGINT, Primary Key, Auto-increment)
- `code` (VARCHAR, Unique, Non-null): Promotional code string (e.g., `SAVE20`).
- `discount_type` (VARCHAR / ENUM: `PERCENTAGE`, `FLAT`, Non-null): Strategy for calculating discounts.
- `discount_value` (DOUBLE, Non-null): Numeric value representing percentage or currency amount.
- `min_order_amount` (DOUBLE, Default 0.0): Minimum cart/booking subtotal required before the coupon applies.
- `valid_until` (TIMESTAMP / DATETIME): Expiration datetime after which coupon is invalid.
- `usage_limit` (INTEGER): Maximum total times the coupon can be applied across all users.
- `used_count` (INTEGER, Default 0): Number of times the coupon has been used.
- `is_active` (BOOLEAN, Default true): Operational toggle to enable/disable coupons.

### Booking (`bookings`)
Represents a confirmed ticket reservation for a specific show and user.
- `id` (BIGINT, Primary Key, Auto-increment)
- `user_id` (FK -> `User`): The customer reserving seats.
- `show_id` (FK -> `Show`): The showtime being reserved.
- `seat_numbers` (VARCHAR): Comma-separated list of selected seats (e.g., `A1,A2`).
- `total_amount` (DOUBLE): Final payable amount after promotional discounts.
- `discount_amount` (DOUBLE): Amount saved via promotional discount (0.0 if none).
- `coupon_code` (VARCHAR, Nullable): Code of the applied promotional coupon.
- `booking_time` (TIMESTAMP): Creation timestamp recorded via `@PrePersist`.

### Show (`Show`)
- `id` (BIGINT, Primary Key)
- `movie` (FK -> `Movie`)
- `showDate` (DATE)
- `showTime` (TIME)
- `screen` (VARCHAR)
- `price` (DOUBLE): Base ticket price per seat.

---

## 2. Business Invariants & Rules

### Promotional Discount Rules
1. **Case-Insensitive Codes:** Promo codes are evaluated case-insensitively (`SAVE20` == `save20` == `Save20`).
2. **Server-Side Price Integrity:** 
   - All discounts are computed on the server.
   - When `/user/bookings` is called, the original order amount is computed as `show.price * seatCount`.
   - The coupon discount is validated and calculated independently of any client-supplied totals.
3. **One Coupon Per Order:** A single booking transaction accepts at most one coupon.
4. **Expiration & Inactivity:**
   - Any coupon with `is_active = false` is rejected.
   - Any coupon where current system time is strictly after `valid_until` is rejected.
   - Rejection message: `"Invalid or expired coupon code"`.
5. **Usage Limit:**
   - Any coupon where `used_count >= usage_limit` (for `usage_limit > 0`) is rejected.
   - Rejection message: `"Invalid or expired coupon code"`.
6. **Minimum Spend Requirement:**
   - If `subtotal < min_order_amount`, coupon cannot be applied.
   - Rejection message format: `"Minimum booking amount of ₹%.2f required."` (e.g., `"Minimum booking amount of ₹300.00 required."`).
7. **Discount Calculation:**
   - `PERCENTAGE`: `subtotal * (discount_value / 100.0)`.
   - `FLAT`: `discount_value`.
   - Discount amount is capped at `subtotal` (amount payable never drops below zero).
   - All currency values are rounded to 2 decimal places.
