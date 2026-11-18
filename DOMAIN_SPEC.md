# Domain Specification: MovieMatic

## 1. Domain Entities & Invariants

### 1.1 Coupon
The `Coupon` entity represents promotional discount codes that customers can apply during checkout.

| Field | Type | Nullable | Unique | Constraints & Invariants |
|-------|------|----------|--------|--------------------------|
| `id` | `Long` | No | Yes | Primary Key, Auto-generated Identity |
| `code` | `String` | No | Yes | Case-insensitive identifier (e.g., `SAVE20`, `FLAT50`) |
| `discountType` | `DiscountType` | No | No | `PERCENTAGE` or `FLAT` |
| `discountValue` | `double` | No | No | > 0. For `PERCENTAGE`, 0.0 - 100.0 |
| `minOrderAmount` | `double` | Yes | No | Minimum booking total required to apply coupon (default: 0.0) |
| `validUntil` | `LocalDateTime` | Yes | No | Coupon expiration timestamp; must be in the future to apply |
| `usageLimit` | `Integer` | Yes | No | Maximum times a coupon can be redeemed across all orders |
| `usedCount` | `int` | No | No | Number of times redeemed (default: 0; incremented upon successful booking) |
| `isActive` | `boolean` | No | No | Master active switch (default: true) |

### 1.2 Booking Updates
The `Booking` entity tracks reservations made by users.

| Field | Type | Description |
|-------|------|-------------|
| `couponCode` | `String` | Applied coupon code (null if no promo code was applied) |
| `discountAmount` | `double` | Total discount deducted from base seat price (default: 0.0) |
| `totalAmount` | `double` | Net payable amount after applying promotional discount |

---

## 2. Business Rules & Validation Engine

1. **Case-Insensitive Matching:**
   - Coupon codes are looked up case-insensitively using `findByCodeIgnoreCaseAndIsActiveTrue(code)`.
   - Codes like `save20`, `SAVE20`, and `Save20` resolve to the same underlying record.

2. **Server-Side Recalculation (Tamper Resistance):**
   - The client never dictates the final price or discount amount.
   - During `POST /user/bookings`, the server queries the `Show` entity to determine seat price, computes `baseAmount = price * seatCount`, validates the coupon against `baseAmount`, and computes the true discounted total.

3. **Discount Computation:**
   - **PERCENTAGE:** `discount = (totalAmount * discountValue) / 100.0`
   - **FLAT:** `discount = discountValue`
   - **Boundary Clamping:** `discount = min(discount, totalAmount)`. Final amount can never be negative.
   - **Rounding:** Rounded to 2 decimal places using `RoundingMode.HALF_UP`.

4. **Minimum Spend Rule:**
   - If `minOrderAmount > 0` and `totalAmount < minOrderAmount`, reject with:
     `Minimum booking amount of ₹<minOrderAmount> required.`

5. **Expiry & Usage Limits:**
   - Expired coupons (`validUntil < now()`) or exhausted coupons (`usedCount >= usageLimit`) must be rejected with:
     `Invalid or expired coupon code`

6. **One Coupon Per Booking:**
   - Exactly one coupon can be applied per checkout order.
