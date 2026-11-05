package com.MovieMatic.service;

import java.awt.Color;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            String[] seats = booking.getSeatNumbers().split(","); // e.g., "A1,B2"
            occupied.addAll(Arrays.asList(seats));
        }
        return occupied;
    }
    
    
//    public List<String> getOccupiedSeats(Show show) {
//        List<Booking> bookings = bookingRepository.findByShow(show);
//        List<String> occupied = new ArrayList<>();
//
//        for (Booking b : bookings) {
//            String[] seats = b.getSeatNumbers().split(",");
//            occupied.addAll(Arrays.asList(seats));
//        }
//        return occupied;
//    }

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
    

//    public void deleteBooking(Long id) {
//        bookingRepository.deleteById(id);
//    }	
//    
   

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

            // Table Headers (Optional, but good for clarity)
            // table.addCell(getCell("Detail", PdfPCell.ALIGN_LEFT, headerFont));
            // table.addCell(getCell("Value", PdfPCell.ALIGN_LEFT, headerFont));

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

	public void deleteBooking(Long id) {
		bookingRepository.deleteById(id);
	}

}