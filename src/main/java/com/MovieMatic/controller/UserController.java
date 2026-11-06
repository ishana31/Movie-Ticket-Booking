package com.MovieMatic.controller;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.MovieMatic.dto.CancellationPreviewResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.ShowService;
import com.MovieMatic.service.UserService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
    private MovieService movieService;

    @Autowired
    private ShowService showService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/movies")
    public String listMovies(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("movies", movieService.getNowShowing());
        model.addAttribute("topMovies", movieService.getTop10ByShowPrice());
        model.addAttribute("newReleases", movieService.getNewReleases());
        model.addAttribute("upcomingMovies", movieService.getUpcomingMovies());
        return "user/movies";
    }
    
    @GetMapping("movies/filter-by-genre")
    @ResponseBody
    public List<Movie> filterMoviesByGenre(@RequestParam String genre) {
        if (genre.equalsIgnoreCase("All")) {
            return movieService.getAllMovies();
        } else {
            return movieService.getMoviesByGenre(genre);
        }
    }
    
    @GetMapping("/movies/top")
    public String topMovies(Model model) {
        model.addAttribute("movies", showService.getTop10MoviesByPrice());
        model.addAttribute("sectionTitle", "Top 10 Movies");
        return "user/movie-list";
    }
    
    @GetMapping("/movies/new")
    public String newReleases(Model model) {
        model.addAttribute("movies", movieService.getNewReleases());
        model.addAttribute("sectionTitle", "New Releases");
        return "user/movie-list";
    }
    
    @GetMapping("/movies/upcoming")
    public String upcomingMovies(Model model) {
        model.addAttribute("movies", movieService.getUpcomingMovies());
        model.addAttribute("sectionTitle", "Coming Soon");
        return "user/movie-list";
    }

    @GetMapping("/movies/{id}/shows")
    public String listShows(@PathVariable Long id, Model model) {
        Movie movie = movieService.getMovieById(id);
        List<Show> shows = showService.getShowsByMovieId(id);
        model.addAttribute("movie", movie);
        model.addAttribute("shows", shows);
        return "user/shows";
    }
    
    @GetMapping("/movie-details/shows/{id}/book")
    public String bookForm(@PathVariable Long id, Model model, 
                          @AuthenticationPrincipal UserDetails userDetails) {
        Show show = showService.getShowById(id);
        User user = userService.findByEmail(userDetails.getUsername());
        Movie movie = show.getMovie();
        List<String> occupiedSeats = bookingService.getOccupiedSeats(show);
        model.addAttribute("show", show);
        model.addAttribute("movie", movie);
        model.addAttribute("user", user);
        model.addAttribute("occupiedSeats", occupiedSeats);
        return "user/book";
    }
    
    @PostMapping("/movie-details/shows/{id}/book")
    public String bookSeats(@PathVariable Long id,
                           @RequestParam String seatNumbers,
                           @AuthenticationPrincipal UserDetails userDetails) {
        Show show = showService.getShowById(id);
        User user = userService.findByEmail(userDetails.getUsername());
        
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeatNumbers(seatNumbers);
        booking.setTotalAmount(show.getPrice() * seatNumbers.split(",").length);
        
        bookingService.saveBooking(booking);
        return "redirect:/user/my-bookings";
    }
    
    @GetMapping("/movie-details/{id}")
    public String getMovieDetails(@PathVariable Long id, Model model, 
                          @AuthenticationPrincipal UserDetails userDetails) {
    	Movie movie = movieService.getMovieById(id);
        List<Show> show = showService.getShowsByMovieId(movie.getId());
        User user = userService.findByEmail(userDetails.getUsername());
        
        model.addAttribute("shows", show);
        model.addAttribute("movie", movie);
        model.addAttribute("user", user);
        return "user/movie-details";
    }
    
    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmail(userDetails.getUsername());
        List<Booking> bookings = bookingService.getUserBookings(user);
        bookings.sort((b1, b2) -> b2.getBookingTime().compareTo(b1.getBookingTime()));
        model.addAttribute("bookings", bookings);
        model.addAttribute("user", user);
        return "user/my-bookings";
    }
    
    @GetMapping("/movies/image/{id}")
    @ResponseBody
    public byte[] getMovieImage(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return movie.getPosterData();
    }
    
    @GetMapping("/movies/background/{id}")
    @ResponseBody
    public byte[] getMovieBackground(@PathVariable Long id) {
        Movie movie = movieService.getMovieById(id);
        return movie.getBackgroundData();
    }
    
    @GetMapping("/booking/download/{id}")
    public void downloadTicket(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              HttpServletResponse response) throws Exception {
        Booking booking = bookingService.getBookingById(id);
        User user = userService.findByEmail(userDetails.getUsername());
        
        // Authorization check
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            response.sendRedirect("/user/my-bookings");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=MovieMatic_Ticket_" + id + ".pdf");

        Document document = new Document(PageSize.A5.rotate());
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        
        document.open();

        // Add metadata
        document.addTitle("Namma Ticket - " + booking.getShow().getMovie().getTitle());
        document.addSubject("E-Ticket for NammaTickets");
        document.addKeywords("Movie, Ticket, Booking, Cinema");
        document.addAuthor("Namma Tickets");
        document.addCreator("Namma Tickets");

        // Create fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.BLUE);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);

        // Create a table for layout
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2});

        // Left column - Ticket details
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(20);

        // Add title
        leftCell.addElement(new Paragraph("E-TICKET", titleFont));
        leftCell.addElement(new Paragraph("Namma Ticket Cinemas", smallFont));
        leftCell.addElement(Chunk.NEWLINE);

        // Add movie details
        leftCell.addElement(new Paragraph("MOVIE DETAILS", headerFont));
        leftCell.addElement(new Paragraph(booking.getShow().getMovie().getTitle(), normalFont));
        leftCell.addElement(new Paragraph("Genre: " + booking.getShow().getMovie().getGenre(), smallFont));
        leftCell.addElement(Chunk.NEWLINE);

        // Add show details
        leftCell.addElement(new Paragraph("SHOW INFORMATION", headerFont));
        leftCell.addElement(new Paragraph("Date: " + booking.getShow().getShowDate(), normalFont));
        leftCell.addElement(new Paragraph("Time: " + booking.getShow().getShowTime(), normalFont));
        leftCell.addElement(new Paragraph("Screen: " + booking.getShow().getScreen(), normalFont));
        leftCell.addElement(Chunk.NEWLINE);

        // Add booking details
        leftCell.addElement(new Paragraph("BOOKING DETAILS", headerFont));
        leftCell.addElement(new Paragraph("Seats: " + booking.getSeatNumbers(), normalFont));
        leftCell.addElement(new Paragraph("Total: ₹" + booking.getTotalAmount(), normalFont));
        leftCell.addElement(new Paragraph("Booking ID: " + booking.getId(), smallFont));
        leftCell.addElement(Chunk.NEWLINE);

        // Right column - QR code and barcode
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(20);
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Generate QR code
        String qrData = "BookingID:" + booking.getId() + 
                       "|Movie:" + booking.getShow().getMovie().getTitle() + 
                       "|Show:" + booking.getShow().getShowDate() + " " + booking.getShow().getShowTime() + 
                       "|Seats:" + booking.getSeatNumbers();
        
        BarcodeQRCode qrCode = new BarcodeQRCode(qrData, 300, 300, null);
        Image qrCodeImage = qrCode.getImage();
        qrCodeImage.scaleToFit(200, 200);
        rightCell.addElement(qrCodeImage);
        rightCell.addElement(Chunk.NEWLINE);

        // Add cells to table
        table.addCell(leftCell);
        table.addCell(rightCell);

        // Add table to document
        document.add(table);

        document.close();
    }
    
    @GetMapping("/booking/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails,
        Principal principal) {
        String email = principal != null ? principal.getName() : (userDetails != null ? userDetails.getUsername() : null);
        if (email == null) {
            return "redirect:/login";
        }

        Booking booking = bookingService.getBookingById(id);
        User user = userService.findByEmail(email);
        if (booking != null && booking.getUser() != null && booking.getUser().getId().equals(user.getId())) {
            if (!"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                try {
                    bookingService.cancelBooking(booking, LocalDateTime.now());
                } catch (Exception ignored) {
                }
            }
        }
        return "redirect:/user/my-bookings";
    }

    /**
     * Preview cancellation refund details for a booking.
     * GET /user/bookings/{bookingId}/cancellation-preview
     * 
     * - Returns 200 OK with total paid, refund percentage, refund amount, and cancellation fee.
     * - Returns 400 Bad Request if the booking is already cancelled or show starts in < 1 hour / already started.
     * - Returns 403 Forbidden if the booking belongs to a different user or unauthorized.
     * - Returns 404 Not Found if the booking does not exist.
     */
    @GetMapping("/bookings/{bookingId}/cancellation-preview")
    @ResponseBody
    public ResponseEntity<?> getCancellationPreview(@PathVariable Long bookingId,
                                                   @AuthenticationPrincipal UserDetails userDetails,
                                                   Principal principal) {
        String email = principal != null ? principal.getName() : (userDetails != null ? userDetails.getUsername() : null);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "User unauthorized"));
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Booking not found with ID: " + bookingId));
        }

        User currentUser = userService.findByEmail(email);
        if (currentUser == null || booking.getUser() == null || 
            !booking.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to view this booking"));
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Booking is already cancelled"));
        }

        Show show = booking.getShow();
        if (show == null || show.getShowDate() == null || show.getShowTime() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Show details unavailable"));
        }

        LocalDateTime showStartTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        LocalDateTime now = LocalDateTime.now();
        if (!showStartTime.isAfter(now) || Duration.between(now, showStartTime).getSeconds() < 3600) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Show starts in less than 1 hour or has already started and is not eligible for cancellation"));
        }

        try {
            CancellationPreviewResponse preview = bookingService.getCancellationPreview(booking, now);
            return ResponseEntity.ok(preview);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Cancel an existing booking and process refund according to tiered refund policy.
     * POST /user/bookings/{bookingId}/cancel
     * 
     * - Returns 200 OK with the cancelled booking details.
     * - Returns 400 Bad Request if the show starts in less than 1 hour or has already started.
     * - Returns 403 Forbidden if unauthorized.
     * - Returns 409 Conflict if the booking is already cancelled.
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelBookingPost(@PathVariable Long bookingId,
                                               @AuthenticationPrincipal UserDetails userDetails,
                                               Principal principal) {
        String email = principal != null ? principal.getName() : (userDetails != null ? userDetails.getUsername() : null);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "User unauthorized"));
        }

        Booking booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Booking not found with ID: " + bookingId));
        }

        User currentUser = userService.findByEmail(email);
        if (currentUser == null || booking.getUser() == null || 
            !booking.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You are not authorized to cancel this booking"));
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Booking is already cancelled"));
        }

        Show show = booking.getShow();
        if (show == null || show.getShowDate() == null || show.getShowTime() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Show details unavailable"));
        }

        LocalDateTime showStartTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        LocalDateTime now = LocalDateTime.now();
        if (!showStartTime.isAfter(now) || Duration.between(now, showStartTime).getSeconds() < 3600) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Show starts in less than 1 hour or has already started"));
        }

        try {
            Booking cancelledBooking = bookingService.cancelBooking(booking, now);
            return ResponseEntity.ok(cancelledBooking);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
    
    //User profile crud
    @GetMapping("/profile")
    public String profile(Model model,
        @AuthenticationPrincipal UserDetails userDetails) {
      User u = userService.findByEmail(userDetails.getUsername());
      model.addAttribute("user", u);
      return "user/profile";
    }

    @PostMapping("/profile/update")
    public String updateUserProfile(@ModelAttribute User updatedUser,
                                     @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                     Principal principal, RedirectAttributes redirectAttributes) {
        return userService.updateUserProfile(updatedUser, imageFile, principal, redirectAttributes);
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getUserImage(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent() && userOpt.get().getProfileImage() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(userOpt.get().getProfileImage());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/bookings/{id}/download")
    public void downloadTicket(@PathVariable Long id, HttpServletResponse response) {
        bookingService.generatePdfTicket(id, response);
    }
    
    @PostMapping("/book")
    public String bookTicket(@ModelAttribute Booking booking, Principal principal) {
        Booking bookings = bookingService.getBookingById(booking.getId());
        return "redirect:/user/my-bookings";
    }
    
    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<String> createBooking(@RequestBody Map<String, Object> bookingData,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userService.findByEmail(userDetails.getUsername());
            Long showId = Long.valueOf(bookingData.get("showId").toString());
            Show show = showService.getShowById(showId);
            String seatNumbers = (String) bookingData.get("seatNumbers");

            String[] requestedSeats = seatNumbers.split(",");
            double totalAmount = show.getPrice() * requestedSeats.length;

            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setShow(show);
            booking.setSeatNumbers(seatNumbers);
            booking.setTotalAmount(totalAmount);
            booking.setStatus("CONFIRMED");
            booking.setBookingTime(LocalDateTime.now());

            bookingService.saveBooking(booking);

            // Send email with PDF attachment
            emailService.sendBookingConfirmationEmail(booking.getId(), currentUser.getEmail());

            return ResponseEntity.ok("Booking successful! Confirmation email sent to " + currentUser.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Booking failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/shows/{showId}/occupied-seats")
    @ResponseBody
    public ResponseEntity<List<String>> getOccupiedSeats(@PathVariable Long showId) {
        Show show = showService.getShowById(showId);
        if (show == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> occupiedSeats = bookingService.getOccupiedSeats(show);
        return ResponseEntity.ok(occupiedSeats);
    }
    
    @GetMapping("/all-movies")
    public String getAllMovies(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        return "user/all-movies";
    }
    
    @GetMapping("/about")
    public String getAbout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
    	User user = userService.findByEmail(userDetails.getUsername());
    	model.addAttribute("user", user);
    	return "user/about";
    }
    
    @GetMapping("/contact")
    public String getContact(Model model, @AuthenticationPrincipal UserDetails userDetails) {
    	User user = userService.findByEmail(userDetails.getUsername());
    	model.addAttribute("user", user);
    	return "user/contact";
    }
}
