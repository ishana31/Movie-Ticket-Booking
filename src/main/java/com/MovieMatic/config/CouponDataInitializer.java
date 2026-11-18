package com.MovieMatic.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

@Component
public class CouponDataInitializer implements CommandLineRunner {

    @Autowired
    private CouponRepository couponRepository;

    @Override
    public void run(String... args) {
        if (couponRepository.count() == 0) {
            // SAVE20: 20% discount, min spend 300, valid for 1 year
            Coupon save20 = new Coupon(
                    "SAVE20",
                    DiscountType.PERCENTAGE,
                    20.0,
                    300.0,
                    LocalDateTime.now().plusYears(1),
                    500,
                    0,
                    true
            );

            // FLAT50: FLAT 50 discount, min spend 200, valid for 1 year
            Coupon flat50 = new Coupon(
                    "FLAT50",
                    DiscountType.FLAT,
                    50.0,
                    200.0,
                    LocalDateTime.now().plusYears(1),
                    500,
                    0,
                    true
            );

            // EXPIRED: already expired coupon for negative testing
            Coupon expired = new Coupon(
                    "EXPIRED",
                    DiscountType.PERCENTAGE,
                    15.0,
                    100.0,
                    LocalDateTime.now().minusDays(2),
                    100,
                    0,
                    true
            );

            // LIMITREACHED: usage limit reached
            Coupon limitReached = new Coupon(
                    "LIMITREACHED",
                    DiscountType.FLAT,
                    30.0,
                    100.0,
                    LocalDateTime.now().plusYears(1),
                    10,
                    10,
                    true
            );

            couponRepository.save(save20);
            couponRepository.save(flat50);
            couponRepository.save(expired);
            couponRepository.save(limitReached);
        }
    }
}
