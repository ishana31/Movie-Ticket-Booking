package com.MovieMatic.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.repository.CouponRepository;

@Component
public class CouponDataLoader implements CommandLineRunner {

    @Autowired
    private CouponRepository couponRepository;

    @Override
    public void run(String... args) {
        if (couponRepository.findByCodeIgnoreCase("SAVE20").isEmpty()) {
            Coupon save20 = new Coupon(
                "SAVE20",
                "PERCENTAGE",
                20.0,
                300.0,
                LocalDateTime.now().plusYears(5),
                1000,
                0,
                true
            );
            couponRepository.save(save20);
        }

        if (couponRepository.findByCodeIgnoreCase("FLAT50").isEmpty()) {
            Coupon flat50 = new Coupon(
                "FLAT50",
                "FLAT",
                50.0,
                200.0,
                LocalDateTime.now().plusYears(5),
                1000,
                0,
                true
            );
            couponRepository.save(flat50);
        }
    }
}
