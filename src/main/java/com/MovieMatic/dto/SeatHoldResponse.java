package com.MovieMatic.dto;

public class SeatHoldResponse {

    private String status;
    private long expiresInSeconds;

    public SeatHoldResponse() {
    }

    public SeatHoldResponse(String status, long expiresInSeconds) {
        this.status = status;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
