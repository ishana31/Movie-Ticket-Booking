package com.MovieMatic.dto;

/**
 * Response payload returned after creating or updating a booking with promotional discounts applied.
 *
 * @author CodeFactoryAgent
 */
public class BookingResponse {

    /**
     * Unique identifier of the generated booking.
     */
    private Long bookingId;

    /**
     * Final payable total amount for the booking.
     */
    private double totalAmount;

    /**
     * Total promotional discount deducted from original seat prices.
     */
    private double discountAmount;

    /**
     * Informative confirmation message.
     */
    private String message;

    /**
     * Default constructor for serialization frameworks.
     */
    public BookingResponse() {
    }

    /**
     * Parameterized constructor matching the booking response requirement.
     *
     * @param bookingId      unique booking identifier
     * @param totalAmount    final payable amount
     * @param discountAmount discount savings amount
     */
    public BookingResponse(Long bookingId, double totalAmount, double discountAmount) {
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
    }

    /**
     * Full constructor including status message.
     *
     * @param bookingId      unique booking identifier
     * @param totalAmount    final payable amount
     * @param discountAmount discount savings amount
     * @param message        confirmation message
     */
    public BookingResponse(Long bookingId, double totalAmount, double discountAmount, String message) {
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
