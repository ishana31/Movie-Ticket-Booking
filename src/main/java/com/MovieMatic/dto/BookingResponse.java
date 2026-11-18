package com.MovieMatic.dto;

public class BookingResponse {

    private Long bookingId;
    private double totalAmount;
    private double discountAmount;

    public BookingResponse() {
    }

    public BookingResponse(Long bookingId, double totalAmount, double discountAmount) {
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
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
}
