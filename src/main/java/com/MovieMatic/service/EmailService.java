package com.MovieMatic.service;
//EmailService.java

import com.MovieMatic.model.Booking;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

 @Autowired
 private JavaMailSender javaMailSender;

 @Autowired
 private BookingService bookingService;

 @Async
 public void sendEmail(String toEmail, String subject, String message) {
     SimpleMailMessage mailMessage = new SimpleMailMessage();
     mailMessage.setTo(toEmail);
     mailMessage.setSubject(subject);
     mailMessage.setText(message);
     mailMessage.setFrom("ishanapujara@gmail.com");
     javaMailSender.send(mailMessage);
 }

 public void sendBookingConfirmationEmail(Long bookingId, String toEmail) {
     try {
         Booking booking = bookingService.getBookingById(bookingId);
         File tempFile = File.createTempFile("MovieMatic_Ticket_" + bookingId, ".pdf");

         // Generate PDF ticket
         try (FileOutputStream fos = new FileOutputStream(tempFile)) {
             createPdfTicket(booking, fos);
         }

         // Send email with PDF attachment
         MimeMessage message = javaMailSender.createMimeMessage();
         MimeMessageHelper helper = new MimeMessageHelper(message, true);
         helper.setTo(toEmail);
         helper.setSubject("🎟 Booking Confirmed: Namma E-Ticket");
         helper.setText("Hi " + booking.getUser().getName() + ",\n\n" +
                 "Thank you for booking with Namma Tickets!\n\n" +
                 "Please find your e-ticket attached.\n\n" +
                 "Movie: " + booking.getShow().getMovie().getTitle() + "\n" +
                 "Show Time: " + booking.getShow().getShowDate() + " at " + booking.getShow().getShowTime() + "\n" +
                 "Seats: " + booking.getSeatNumbers() + "\n\n" +
                 "Enjoy your movie!\n\n" +
                 "- Namma Tickets Team");

         helper.addAttachment("Namma_Ticket_" + bookingId + ".pdf", new FileSystemResource(tempFile));
         javaMailSender.send(message);

         tempFile.delete(); // optional cleanup
     } catch (Exception e) {
         e.printStackTrace();
     }
 }

 private void createPdfTicket(Booking booking, FileOutputStream fos) throws Exception {
     Document document = new Document(PageSize.A5.rotate());
     PdfWriter.getInstance(document, fos);
     document.open();

     Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.BLUE);
     Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
     Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);

     PdfPTable table = new PdfPTable(2);
     table.setWidthPercentage(100);
     table.setWidths(new float[]{3, 2});

     // Left
     PdfPCell left = new PdfPCell();
     left.setBorder(Rectangle.NO_BORDER);
     left.setPadding(20);
     left.addElement(new Paragraph("E-TICKET", titleFont));
     left.addElement(new Paragraph("Movie: " + booking.getShow().getMovie().getTitle(), normalFont));
     left.addElement(new Paragraph("Genre: " + booking.getShow().getMovie().getGenre(), normalFont));
     left.addElement(new Paragraph("Date: " + booking.getShow().getShowDate(), normalFont));
     left.addElement(new Paragraph("Time: " + booking.getShow().getShowTime(), normalFont));
     left.addElement(new Paragraph("Screen: " + booking.getShow().getScreen(), normalFont));
     left.addElement(new Paragraph("Seats: " + booking.getSeatNumbers(), normalFont));
     left.addElement(new Paragraph("Amount: ₹" + booking.getTotalAmount(), normalFont));
     left.addElement(new Paragraph("Booking ID: " + booking.getId(), normalFont));

     // Right
     PdfPCell right = new PdfPCell();
     right.setBorder(Rectangle.NO_BORDER);
     right.setPadding(20);
     BarcodeQRCode qr = new BarcodeQRCode(
             "BookingID:" + booking.getId() +
                     "|Movie:" + booking.getShow().getMovie().getTitle() +
                     "|Seats:" + booking.getSeatNumbers(), 200, 200, null);
     Image qrImage = qr.getImage();
     qrImage.scaleToFit(180, 180);
     right.addElement(qrImage);

     table.addCell(left);
     table.addCell(right);
     document.add(table);
     document.close();
 }
}
