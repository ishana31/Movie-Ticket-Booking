package com.MovieMatic.dto;

public class BookingRequest {
	private Long showId;

	private String seatNumbers;

	private int seatCount;

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
}