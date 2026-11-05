package com.MovieMatic.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

import com.MovieMatic.dto.OccupiedSeatsResponse;
import com.MovieMatic.dto.SeatHoldRequest;
import com.MovieMatic.dto.SeatHoldResponse;
import com.MovieMatic.exception.SeatUnavailableException;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Movie;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.UserRepository;
import com.MovieMatic.service.BookingService;
import com.MovieMatic.service.EmailService;
import com.MovieMatic.service.MovieService;
import com.MovieMatic.service.SeatReservationService;
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

    @Autowired
    private SeatReservationService seatReservationService;
    
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

    @GetMapping("/book")
    public String bookByParam(@RequestParam("showId") Long showId, Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        return bookForm(showId, model, userDetails);
    }

    @GetMapping("/shows/{id}/book")
    public String bookShowById(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        return bookForm(id, model, userDetails);
    }
    
    @GetMapping("/movie-details/shows/{id}/book")
    public String bookForm(@PathVariable Long id, Model model, 
                          @AuthenticationPrincipal UserDetails userDetails) {
        Show show = showService.getShowById(id);
        User user = userDetails != null ? userService.findByEmail(userDetails.getUsername()) : null;
        Movie movie = show != null ? show.getMovie() : null;
        List<String> occupiedSeats = show != null ? bookingService.getOccupiedSeats(show) : Collections.emptyList();
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
        
        List<String> requestedSeats = Arrays.stream(seatNumbers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        seatReservationService.bookSeatsDirectly(id, requestedSeats, user.getId());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setSeatNumbers(seatNumbers);
        booking.setTotalAmount(show.getPrice() * requestedSeats.size());
        
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
        @AuthenticationPrincipal UserDetails userDetails) {

        Booking booking = bookingService.getBookingById(id);
        User user = userService.findByEmail(userDetails.getUsername());
        if (booking != null && booking.getUser().getId().equals(user.getId())) {
            bookingService.deleteBooking(id);
        }
        return "redirect:/user/my-bookings";
    }
    
    // User profile crud
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

    /**
     * POST /user/shows/{showId}/hold
     * Request Body: { "seatNumbers": "A1,A2" }
     * Responses:
     *   - 200 OK with { "status": "HELD", "expiresInSeconds": 600 }
     *   - 409 Conflict with { "message": "Seats already held or booked" }
     */
    @PostMapping("/shows/{showId}/hold")
    @ResponseBody
    public ResponseEntity<?> holdSeats(@PathVariable Long showId,
                                       @RequestBody SeatHoldRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = null;
            if (userDetails != null) {
                User user = userService.findByEmail(userDetails.getUsername());
                if (user != null) {
                    userId = user.getId();
                }
            } else if (request.getUserId() != null) {
                userId = request.getUserId();
            }

            List<String> seatNumbers = request.getSeatNumberList();
            if (seatNumbers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Seat numbers must not be empty"));
            }

            SeatHoldResponse response = seatReservationService.holdSeats(showId, seatNumbers, userId);
            return ResponseEntity.ok(response);
        } catch (SeatUnavailableException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Seats already held or booked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to hold seats: " + e.getMessage()));
        }
    }

    /**
     * GET /user/shows/{showId}/occupied-seats
     * Response: 200 OK with lists of booked seats and currently held seats:
     * {
     *   "bookedSeats": ["B1", "B2"],
     *   "heldSeats": ["A1", "A2"]
     * }
     */
    @GetMapping("/shows/{showId}/occupied-seats")
    @ResponseBody
    public ResponseEntity<?> getOccupiedSeats(@PathVariable Long showId) {
        Show show = showService.getShowById(showId);
        if (show == null) {
            return ResponseEntity.notFound().build();
        }

        OccupiedSeatsResponse response = seatReservationService.getOccupiedSeats(showId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/bookings")
    @Transactional
    public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> bookingData,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = null;
            if (userDetails != null) {
                currentUser = userService.findByEmail(userDetails.getUsername());
            }

            Long showId = Long.valueOf(bookingData.get("showId").toString());
            Show show = showService.getShowById(showId);
            if (show == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Show not found"));
            }

            String seatNumbers = (String) bookingData.get("seatNumbers");
            if (seatNumbers == null || seatNumbers.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Seat numbers must not be empty"));
            }

            List<String> requestedSeats = Arrays.stream(seatNumbers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            Long userId = currentUser != null ? currentUser.getId() :
                    (bookingData.containsKey("userId") ? Long.valueOf(bookingData.get("userId").toString()) : null);

            // Enforce Single Owner and Timeout rules
            seatReservationService.confirmBooking(showId, requestedSeats, userId);

            double totalAmount = show.getPrice() * requestedSeats.size();

            Booking booking = new Booking();
            booking.setUser(currentUser);
            booking.setShow(show);
            booking.setSeatNumbers(String.join(",", requestedSeats));
            booking.setTotalAmount(totalAmount);
            booking.setBookingTime(LocalDateTime.now());

            bookingService.saveBooking(booking);

            if (currentUser != null && currentUser.getEmail() != null) {
                try {
                    // Send email with PDF attachment
                    emailService.sendBookingConfirmationEmail(booking.getId(), currentUser.getEmail());
                } catch (Exception ex) {
                    // Log email error without failing transaction
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Booking successful! Confirmation email sent to " + (currentUser != null ? currentUser.getEmail() : "user"),
                    "bookingId", booking.getId(),
                    "status", "BOOKED"
            ));
        } catch (SeatUnavailableException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Booking failed: " + e.getMessage()));
        }
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
