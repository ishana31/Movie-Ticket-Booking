package com.MovieMatic.dto;

/**
 * Data transfer object encapsulating movie ticket booking requests.
 * Transports show identity, reserved seat numbers, seat count, and an optional coupon promo code.
 *
 * @author CodeFactoryAgent
 */
public class BookingRequest {

    /**
     * Unique identifier of the selected movie show.
     */
    private Long showId;

    /**
     * Comma-separated list of selected seat labels (e.g. 'A1,A2').
     */
    private String seatNumbers;

    /**
     * Number of seats selected for the booking.
     */
    private int seatCount;

    /**
     * Optional promotional coupon code applied at checkout.
     */
    private String couponCode;

    public BookingRequest() {
    }

    public BookingRequest(Long showId, String seatNumbers, String couponCode) {
        this.showId = showId;
        this.seatNumbers = seatNumbers;
        this.couponCode = couponCode;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
