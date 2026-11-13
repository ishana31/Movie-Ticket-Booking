package com.MovieMatic.dto;

public class HoldSeatRequest {

    private String seatNumbers;

    public HoldSeatRequest() {
    }

    public HoldSeatRequest(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public String getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }
}
