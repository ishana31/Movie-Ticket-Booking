# MovieMatic Domain Specification

## 1. Domain Entities & Invariants

### 1.1 User (`users`)
- `id`: Long (PK, auto-generated)
- `name`: String (non-null)
- `email`: String (non-null, unique)
- `password`: String (BCrypt hashed)
- `role`: String (`ADMIN`, `USER`)
- `phone`: String (optional)
- `profileImage`: byte[] (BLOB)
- `createdAt`: LocalDateTime (set on creation)
- `lastLogin`: LocalDateTime
- `active`: boolean

### 1.2 Movie (`movie`)
- `id`: Long (PK, auto-generated)
- `title`: String (non-null)
- `description`: String
- `genre`: String
- `language`: String
- `duration`: int (minutes)
- `releaseDate`: LocalDate
- `posterData`: byte[] (BLOB)
- `backgroundData`: byte[] (BLOB)

### 1.3 Show (`shows`)
- `id`: Long (PK, auto-generated)
- `movie`: Many-to-One -> Movie
- `showDate`: LocalDate (non-null)
- `showTime`: LocalTime (non-null)
- `screen`: String (non-null)
- `price`: double (non-null, price per seat)

### 1.4 Booking (`booking`)
- `id`: Long (PK, auto-generated)
- `user`: Many-to-One -> User (non-null)
- `show`: Many-to-One -> Show (non-null)
- `seatNumbers`: String (comma-separated list of selected seat labels, e.g. "A1,A2")
- `totalAmount`: double (final payable amount after applied discounts)
- `couponCode`: String (nullable, promo code applied to this booking)
- `discountAmount`: double (rupee value discounted from gross total; 0.0 if no coupon)
- `bookingTime`: LocalDateTime (timestamp of booking)

### 1.5 Coupon (`coupons`)
- `id`: Long (PK, auto-generated)
- `code`: String (non-null, UNIQUE, case-insensitive match)
- `discountType`: Enum `DiscountType` (`PERCENTAGE`, `FLAT`)
- `discountValue`: double (non-null, e.g. 20.0 for 20% or 50.0 for ₹50 off)
- `minOrderAmount`: Double (nullable/optional, minimum booking total required to apply coupon)
- `validUntil`: LocalDateTime (nullable/optional, expiration deadline)
- `usageLimit`: Integer (nullable/optional, total permitted uses across all customers)
- `usedCount`: int (running count of successfully finalized bookings using this coupon)
- `isActive`: boolean (flag indicating administrative enablement)

---

## 2. Business Rules & Validation Invariants

### 2.1 Coupon Validation Rules
1. **Case-Insensitive Matching**:
   - `save20`, `Save20`, and `SAVE20` evaluate to the identical coupon definition.
2. **Active Status**:
   - Only coupons with `is_active = true` may be matched or redeemed.
3. **Expiration Enforcement**:
   - If `valid_until` is defined, the booking attempt time must be strictly before `valid_until`. If `now() >= valid_until`, validation fails with `"Invalid or expired coupon code"`.
4. **Usage Limit Enforcement**:
   - If `usage_limit` is defined and `used_count >= usage_limit`, validation fails with `"Invalid or expired coupon code"`.
5. **Minimum Order Spend**:
   - If `min_order_amount` is defined and `totalAmount < min_order_amount`, validation fails with `"Minimum booking amount of ₹<minOrderAmount> required."`.
6. **Discount Calculation**:
   - `PERCENTAGE`: `discount = (totalAmount * discountValue) / 100.0`.
   - `FLAT`: `discount = min(discountValue, totalAmount)`.
   - The discount amount cannot exceed the total order amount (`0.0 <= discountAmount <= totalAmount`).
   - `finalAmount = max(0.0, totalAmount - discountAmount)`.
7. **Single Coupon Constraint**:
   - Only one coupon can be applied per booking checkout.
8. **Server-Side Re-calculation**:
   - The server re-evaluates the coupon against the show seat price and requested seat count when creating a booking, ignoring any client-supplied totals to avoid tampering.
9. **Atomic Usage Increment**:
   - Upon confirmed booking creation, the coupon's `used_count` is incremented by 1.
