package com.MovieMatic.controller;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.MovieMatic.model.User;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.EmailService;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

@Controller
public class PasswordResetController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
    private EmailService emailService;
    
	@Autowired
    private PasswordEncoder passwordEncoder;
	
	@GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "reset-password";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String email, HttpSession session, Model model) {
        User user = userRepository.findByEmail(email);

        if(user.isActive()) {
        	// Generate 6-digit OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            
            // Store OTP in session with email as key
            session.setAttribute("reset_otp_" + email, otp);
            session.setAttribute("reset_otp_expiry_" + email, 
                LocalDateTime.now().plusMinutes(15)); // 15 minute expiry

            // Send OTP email
            emailService.sendEmail(
                email,
                "Password Reset OTP",
                "Your OTP for password reset is: " + otp + "\nValid for 5 minutes."
            );

            model.addAttribute("email", email);
            model.addAttribute("message", "OTP sent to your email");
        } else {
            model.addAttribute("message", "Unable to sent OTP on your email");
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword,
            HttpSession session,
            Model model) {
        
        // Retrieve stored OTP and expiry
        String storedOtp = (String) session.getAttribute("reset_otp_" + email);
        LocalDateTime expiry = (LocalDateTime) session.getAttribute("reset_otp_expiry_" + email);

        // Validate
        if (storedOtp == null || !storedOtp.equals(otp)) {
            model.addAttribute("error", "Invalid OTP");
            model.addAttribute("email", email);
            return "reset-password";
        }

        if (expiry.isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "OTP has expired");
            model.addAttribute("email", email);
            return "reset-password";
        }

        // Update password
        User user = userRepository.findByEmail(email);
        if(user.isActive()) {
        	user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Clear session attributes
            session.removeAttribute("reset_otp_" + email);
            session.removeAttribute("reset_otp_expiry_" + email);

            model.addAttribute("message", "Password updated successfully");
        } else {
        	model.addAttribute("message", "Unable to update password");
        }
        return "login"; // Redirect to login page
    }

}
