package com.MovieMatic.dto;

public class SeatHoldResponse {

    private String status;
    private int expiresInSeconds;

    public SeatHoldResponse() {
    }

    public SeatHoldResponse(String status, int expiresInSeconds) {
        this.status = status;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
