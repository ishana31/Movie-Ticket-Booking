package com.MovieMatic.controller;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.UserService;



@RequestMapping("/mail")
@RestController
public class EmailController {

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;


//    @GetMapping("/sendEmail")
//    public ResponseEntity<String> sendEmail() {
//        // Assuming email is sent after successful payment
//        emailService.sendEmail("ishanapujara131@gmail.com", "Payment Successful", "Your payment has been successfully processed.\nYour product is on the way. Thank you for shopping with us!");
//        return new ResponseEntity<>("Email Sent", HttpStatus.OK);
//      
//    }
    
    

    @PostMapping("/sendEmail")
    public ResponseEntity<String> sendEmail(@RequestBody Map<String, String> payload) {
    String email = payload.get("email");

    if (email == null || email.isEmpty()) {
    return ResponseEntity.badRequest().body("Email is missing");
    }

    try {
    emailService.sendEmail(
    email,
    "You're in! Account created successfully!",
    "Welcome to Namma Tickets — your gateway to the ultimate movie experience. Let the show begin ... Thank you!"  //customize this
    );
    return ResponseEntity.ok("Email sent to " + email);
    } catch (Exception e) {
    e.printStackTrace();
    return ResponseEntity.internalServerError().body("Failed to send email");
    }
    }
    
    
    
    @PostMapping("/sendmailafterpay")
    public ResponseEntity<String> sendEmailAfterPayment(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String bookingIdStr = payload.get("bookingId");

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("Email is missing");
        }

        if (bookingIdStr == null || bookingIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body("Booking ID is missing");
        }

        try {
            Long bookingId = Long.parseLong(bookingIdStr);

            // Send confirmation email with PDF ticket
            emailService.sendBookingConfirmationEmail(bookingId, email);

            return ResponseEntity.ok("Confirmation email with PDF ticket sent to " + email);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid Booking ID format");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to send email");
        }
    }

 
}

