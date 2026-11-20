package com.MovieMatic.dto;

public class BookingRequest {
	private Long showId;

	private String seatNumbers;

	private int seatCount;

	private String couponCode;

	public BookingRequest() {
	}

	public BookingRequest(Long showId, String seatNumbers, String couponCode) {
		this.showId = showId;
		this.seatNumbers = seatNumbers;
		this.couponCode = couponCode;
	}

	// Getters and setters
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
