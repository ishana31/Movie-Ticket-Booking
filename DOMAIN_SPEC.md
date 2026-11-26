# Domain Specification: MovieMatic

## Overview
MovieMatic is an online cinema ticketing application. This document details domain rules, invariants, validation constraints, and transaction specifications, focusing on the Coupon & Promotional Discount Engine implemented in KAN-13.

---

## 1. Promotional Coupon Domain

### 1.1 Coupon Entity Invariants
A `Coupon` represents a promotional voucher enabling price discounts on movie ticket bookings.
- **Table:** `coupons`
- **Columns & Attributes:**
  - `id` (Long, Primary Key, Auto-generated Identity)
  - `code` (String, Unique, Non-null): Case-insensitive promo code string (e.g. `SAVE20`).
  - `discount_type` (Enum `DiscountType`: `PERCENTAGE`, `FLAT`, Non-null)
  - `discount_value` (Double, Non-null, > 0.0): Either discount percentage (0 - 100) or flat rupee amount.
  - `min_order_amount` (Double, Non-null, >= 0.0): Minimum pre-discount spend required to apply coupon.
  - `valid_until` (LocalDateTime, Non-null): Timestamp after which the coupon expires.
  - `usage_limit` (Integer, Non-null, > 0): Maximum total allowed redemptions across all users.
  - `used_count` (Integer, Non-null, Default: 0): Current total count of redemptions.
  - `is_active` (Boolean, Non-null, Default: true): Soft activation toggle.

### 1.2 Discount Calculation Invariants
1. **Case-Insensitive Code Lookup:**
   - Coupon codes are matched without case sensitivity (`save20` == `SAVE20`).
2. **Eligibility Validation:**
   - Active status must be `true`.
   - Current time must be prior to `valid_until`. If `now >= valid_until`, reject with `"Invalid or expired coupon code"`.
   - `used_count` must be strictly less than `usage_limit`. If `used_count >= usage_limit`, reject with `"Invalid or expired coupon code"`.
   - `totalAmount` must be greater than or equal to `min_order_amount`. If `totalAmount < min_order_amount`, reject with `"Minimum booking amount of ₹%.2f required."`.
3. **Discount Computation:**
   - **PERCENTAGE:** `discount = (totalAmount * discount_value) / 100.0`
   - **FLAT:** `discount = discount_value`
   - **Cap:** Discount savings cannot exceed `totalAmount` (i.e. `finalAmount = Math.max(0.0, totalAmount - discount)`).
   - **Rounding:** All currency figures are rounded to 2 decimal places.
4. **Server-Side Security Enforcement:**
   - Client-side calculations are purely cosmetic for user experience.
   - When a booking is submitted to `POST /user/bookings`, the server must independently recalculate `baseAmount = show.getPrice() * seatCount` and validate the `couponCode` using `CouponService`.
   - Price tampering from browser requests is impossible.
5. **Usage Limit Tracking:**
   - Upon successful persistence of a booking with an applied coupon, `used_count` is incremented by 1.

---

## 2. Booking Domain

### 2.1 Booking Entity Extensions
- **Table:** `bookings`
- **New Columns:**
  - `coupon_code` (VARCHAR, Nullable): Stores the uppercase code of the coupon applied to the booking.
  - `discount_amount` (Double, Default: 0.0): Stores the exact discount savings granted to the user.
  - `total_amount` (Double, Non-null): Stores the post-discount payable total.

### 2.2 Invariant Rules
- One Coupon Per Booking: Only a single promotional code can be applied per reservation.
- Occupied Seats: Selected seats must not already be booked for the same show.

---

## 3. Endpoints & REST Contracts

### 3.1 Validate Coupon
- **Method & Path:** `POST /user/coupons/validate`
- **Request Body:**
  ```json
  { "code": "SAVE20", "totalAmount": 500.0 }
  ```
- **Responses:**
  - `200 OK`:
    ```json
    { "valid": true, "discountAmount": 100.0, "finalAmount": 400.0 }
    ```
  - `400 Bad Request`:
    ```json
    { "message": "Invalid or expired coupon code", "valid": false }
    ```
    or:
    ```json
    { "message": "Minimum booking amount of ₹300.00 required.", "valid": false }
    ```

### 3.2 Create Booking
- **Method & Path:** `POST /user/bookings`
- **Request Body:**
  ```json
  { "showId": 1, "seatNumbers": "A1,A2", "couponCode": "SAVE20" }
  ```
- **Response:**
  - `200 OK`:
    ```json
    { "bookingId": 1, "totalAmount": 400.0, "discountAmount": 100.0 }
    ```
