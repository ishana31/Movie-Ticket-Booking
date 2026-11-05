package com.MovieMatic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.MovieMatic.model.User;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
	
	@Autowired
	private MovieService movieService;
	
	@Autowired
    private ShowService showService;
	
	@Autowired
	private BookingService bookingService;
	
	@Autowired
	private EmailService emailService;

	@GetMapping("/start")
	public String landing() {
		return "index";
	}
	
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        try {
            if (userRepository.existsByEmail(user.getEmail())) {
                model.addAttribute("error", "Email already exists!");
                return "register"; // Show the same register page with error
            }
            
            user.setRole(user.getRole());
            
            userService.saveUser(user);
//            userRepository.save(user); // Save user

            // ✅ Send welcome email
            try {
                emailService.sendEmail(
                    user.getEmail(),
                    "Account created successfully!",
                    "Welcome to Namma Tickets. Continue your cine journey... Thank you!"
                );
            } catch (Exception emailEx) {
                emailEx.printStackTrace();
                model.addAttribute("error", "Account created, but failed to send confirmation email.");
                return "register"; // Stay on page with partial success
            }

           
            return "redirect:/login?registered"; // Redirect to login after success

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Email or username already exists");
            return "register";

        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while creating your account");
            return "register";
        }
    }


    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
    
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        // Add stats data
        model.addAttribute("totalMovies", movieService.getTotalMoviesCount());
        model.addAttribute("totalShows", showService.getTotalShowsCount());
        model.addAttribute("totalBookings", bookingService.getTotalBookingsCount()); // Replace with actual service call
        model.addAttribute("totalRevenue", bookingService.getTotalRevenue()); // Replace with actual service call
        
        // Add movies and shows for the tabs
        model.addAttribute("movies", movieService.getAllMovies());
        model.addAttribute("shows", showService.getAllShows());
        model.addAttribute("bookings", bookingService.getAllBookings());
        
        return "admin/dashboard";
    }	
    
    
    
}
