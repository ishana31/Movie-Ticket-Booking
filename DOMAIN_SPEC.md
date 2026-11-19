# Domain Specification – MovieMatic

## Coupon & Promotional Discount Engine (KAN-13)

### Entity Invariants & Fields

#### `Coupon` (`coupons` table)
- `id`: Unique identifier (Long, primary key, auto-generated identity).
- `code`: Promotional code string, case-insensitive, unique (`@Column(unique = true, nullable = false)`).
- `discount_type`: Enum string representing discount mode: `PERCENTAGE` or `FLAT`.
- `discount_value`: Double value indicating discount amount:
  - For `PERCENTAGE`: 0.0 to 100.0 (e.g. 20.0 for 20% discount).
  - For `FLAT`: Flat currency deduction (e.g. 100.0 for ₹100 deduction).
- `min_order_amount`: Double threshold indicating the minimum booking total required to apply the coupon.
- `valid_until`: LocalDateTime specifying coupon expiration timestamp.
- `usage_limit`: Integer specifying maximum total redemption limit across all orders.
- `used_count`: Integer tracking redemption count (defaults to 0, incremented upon successful booking).
- `is_active`: Boolean flag indicating if coupon is enabled.

#### `Booking` (`booking` table updates)
- `coupon_code`: The promotional coupon code applied to the booking (nullable).
- `discount_amount`: The calculated discount deducted from the ticket price.
- `total_amount`: The final payable amount after discount deduction.

### Business Rules & Validations
1. **Case-Insensitive Matching**: Coupon code lookup is case-insensitive (e.g. `save20`, `Save20`, and `SAVE20` evaluate to `SAVE20`).
2. **Server-Side Price Validation**: Discount amounts and final totals are always recalculated server-side during checkout to prevent client-side price tampering.
3. **One Coupon Per Booking**: Only one promotional discount may be applied to any given booking reservation.
4. **Expiration & Inactive Checks**: Coupons with `is_active = false` or `valid_until < now()` must be rejected with `"Invalid or expired coupon code"`.
5. **Usage Limit Checks**: If `usage_limit > 0` and `used_count >= usage_limit`, the coupon must be rejected with `"Invalid or expired coupon code"`.
6. **Minimum Spend Threshold**: If `totalAmount < min_order_amount`, rejection message must be: `"Minimum booking amount of ₹<amount> required."`.
7. **Discount Cap**: Discount amount cannot exceed total booking price (`discountAmount <= totalAmount`).
