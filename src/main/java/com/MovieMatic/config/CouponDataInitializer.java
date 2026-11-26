package com.MovieMatic.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.MovieMatic.model.Coupon;
import com.MovieMatic.model.DiscountType;
import com.MovieMatic.repository.CouponRepository;

/**
 * Startup database initializer that seeds baseline promotional coupons into the repository
 * if no coupons are present. Ensures that demonstration coupons such as 'SAVE20' are available
 * out-of-the-box for automated verification and user checkout flows.
 *
 * @author CodeFactoryAgent
 */
@Component
public class CouponDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CouponDataInitializer.class);

    private final CouponRepository couponRepository;

    @Autowired
    public CouponDataInitializer(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (couponRepository.findByCodeIgnoreCase("SAVE20").isEmpty()) {
                Coupon save20 = new Coupon(
                        "SAVE20",
                        DiscountType.PERCENTAGE,
                        20.0,
                        300.0,
                        LocalDateTime.now().plusYears(2),
                        500,
                        true
                );
                couponRepository.save(save20);
                log.info("Initialized default promotional coupon: SAVE20 (20% off, min spend ₹300.00)");
            }

            if (couponRepository.findByCodeIgnoreCase("FLAT50").isEmpty()) {
                Coupon flat50 = new Coupon(
                        "FLAT50",
                        DiscountType.FLAT,
                        50.0,
                        200.0,
                        LocalDateTime.now().plusYears(2),
                        500,
                        true
                );
                couponRepository.save(flat50);
                log.info("Initialized default promotional coupon: FLAT50 (₹50 flat discount, min spend ₹200.00)");
            }
        } catch (Exception e) {
            log.warn("Coupon data initialization skipped or completed with notification: {}", e.getMessage());
        }
    }
}
