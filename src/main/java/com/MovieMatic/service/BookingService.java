package com.MovieMatic.service;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.MovieMatic.dto.CancellationPreviewResponse;
import com.MovieMatic.model.Booking;
import com.MovieMatic.model.Show;
import com.MovieMatic.model.User;
import com.MovieMatic.repository.BookingRepository;
// Corrected iText imports
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    public void saveBooking(Booking booking) {
        bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUser(user);
    }

    public List<String> getOccupiedSeats(Show show) {
        List<Booking> bookings = bookingRepository.findByShow(show);

        List<String> occupied = new ArrayList<>();
        for (Booking booking : bookings) {
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                continue;
            }
            if (booking.getSeatNumbers() != null && !booking.getSeatNumbers().trim().isEmpty()) {
                String[] seats = booking.getSeatNumbers().split(",");
                for (String seat : seats) {
                    String trimmed = seat.trim();
                    if (!trimmed.isEmpty()) {
                        occupied.add(trimmed);
                    }
                }
            }
        }
        return occupied;
    }

    public long getTotalBookingsCount() {
        return bookingRepository.count();
    }

    public double getTotalRevenue() {
        Double totalAmount = bookingRepository.sumTotalAmount();
        return totalAmount != null ? totalAmount.doubleValue() : 0.0;
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }
    
    public void deleteByShowId(Long showId) {
        bookingRepository.deleteByShowId(showId);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    /**
     * Preview cancellation refund details for a given booking.
     * 
     * Policy:
     * - More than 4 hours before show start: 90% refund (10% fee)
     * - Between 1 and 4 hours before show start: 50% refund (50% fee)
     * - Less than 1 hour or already passed: Ineligible (throws IllegalArgumentException)
     */
    public CancellationPreviewResponse getCancellationPreview(Booking booking, LocalDateTime requestTime) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        Show show = booking.getShow();
        if (show == null || show.getShowDate() == null || show.getShowTime() == null) {
            throw new IllegalStateException("Show information is incomplete for booking");
        }

        if (requestTime == null) {
            requestTime = LocalDateTime.now();
        }

        LocalDateTime showStartTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        if (!showStartTime.isAfter(requestTime)) {
            throw new IllegalArgumentException("Show has already started or passed");
        }

        long secondsUntilShow = Duration.between(requestTime, showStartTime).getSeconds();
        if (secondsUntilShow < 3600) {
            throw new IllegalArgumentException("Cancellations are not permitted within 1 hour of show start");
        }

        int refundPercentage = secondsUntilShow > 14400 ? 90 : 50;
        double totalPaid = booking.getTotalAmount();
        BigDecimal total = BigDecimal.valueOf(totalPaid).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundPct = BigDecimal.valueOf(refundPercentage);
        BigDecimal refundAmount = total.multiply(refundPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal cancellationFee = total.subtract(refundAmount);

        return new CancellationPreviewResponse(totalPaid, refundPercentage, refundAmount.doubleValue(), cancellationFee.doubleValue());
    }

    /**
     * Cancel a booking, update status to CANCELLED, set refund details and timestamp,
     * releasing seats automatically.
     */
    public Booking cancelBooking(Booking booking, LocalDateTime cancellationTime) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        Show show = booking.getShow();
        if (show == null || show.getShowDate() == null || show.getShowTime() == null) {
            throw new IllegalStateException("Show information is incomplete for booking");
        }

        if (cancellationTime == null) {
            cancellationTime = LocalDateTime.now();
        }

        LocalDateTime showStartTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        if (!showStartTime.isAfter(cancellationTime)) {
            throw new IllegalArgumentException("Show has already started or passed");
        }

        long secondsUntilShow = Duration.between(cancellationTime, showStartTime).getSeconds();
        if (secondsUntilShow < 3600) {
            throw new IllegalArgumentException("Cancellations are not permitted within 1 hour of show start");
        }

        int refundPercentage = secondsUntilShow > 14400 ? 90 : 50;
        double totalPaid = booking.getTotalAmount();
        BigDecimal total = BigDecimal.valueOf(totalPaid).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundPct = BigDecimal.valueOf(refundPercentage);
        BigDecimal refundAmount = total.multiply(refundPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal cancellationFee = total.subtract(refundAmount);

        booking.setStatus("CANCELLED");
        booking.setCancelledAt(cancellationTime);
        booking.setRefundAmount(refundAmount.doubleValue());
        booking.setCancellationFee(cancellationFee.doubleValue());
        booking.setRefundStatus("REFUNDED");

        return bookingRepository.save(booking);
    }

    public void generatePdfTicket(Long bookingId, HttpServletResponse response) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

            // Ensure show and movie are loaded (might need @Transactional or eager fetching)
            Show show = booking.getShow();
            if (show == null || show.getMovie() == null) {
                throw new RuntimeException("Booking details (Show or Movie) are incomplete.");
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=ticket_" + bookingId + ".pdf");

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLUE);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("🎬 Movie Ticket", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" ")); // Spacer

            // Create a table for booking details
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80); // Set table width to 80% of page width
            table.setSpacingBefore(10f); // Space before table
            table.setSpacingAfter(10f); // Space after table
            table.setHorizontalAlignment(Element.ALIGN_CENTER); // Center the table

            // Booking Details
            table.addCell(getCell("Booking ID:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell(String.valueOf(booking.getId()), PdfPCell.ALIGN_LEFT, cellFont));

            table.addCell(getCell("Movie Title:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell(show.getMovie().getTitle(), PdfPCell.ALIGN_LEFT, cellFont));

            table.addCell(getCell("Theater:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell(show.getScreen(), PdfPCell.ALIGN_LEFT, cellFont));

            // Format Date & Time
            String formattedDateTime = show.getShowDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                                     + " at " + show.getShowTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
            table.addCell(getCell("Date & Time:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell(formattedDateTime, PdfPCell.ALIGN_LEFT, cellFont));

            table.addCell(getCell("Seats:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell(booking.getSeatNumbers(), PdfPCell.ALIGN_LEFT, cellFont));

            table.addCell(getCell("Total Price:", PdfPCell.ALIGN_LEFT, headerFont));
            table.addCell(getCell("₹" + String.format("%.2f", booking.getTotalAmount()), PdfPCell.ALIGN_LEFT, cellFont));

            document.add(table);

            document.add(new Paragraph(" ")); // Spacer
            document.add(new Paragraph("Please arrive at least 15 minutes early.", subFont));
            document.add(new Paragraph("Enjoy your movie!", subFont));

            document.close();
        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        } catch (Exception e) { 
            throw new RuntimeException("An unexpected error occurred while generating PDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell getCell(String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(PdfPCell.NO_BORDER); 
        return cell;
    }
}
