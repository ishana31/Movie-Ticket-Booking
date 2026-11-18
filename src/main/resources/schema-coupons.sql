-- Coupon & Promotional Discount Engine Schema Updates

CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DOUBLE PRECISION NOT NULL,
    min_order_amount DOUBLE PRECISION DEFAULT 0.0,
    valid_until TIMESTAMP,
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Update bookings table with coupon tracking columns
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(255);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS discount_amount DOUBLE PRECISION DEFAULT 0.0;
