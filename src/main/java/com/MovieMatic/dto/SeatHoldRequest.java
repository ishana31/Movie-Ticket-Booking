package com.MovieMatic.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SeatHoldRequest {

    private String seatNumbers;
    private Long userId;

    public SeatHoldRequest() {
    }

    public SeatHoldRequest(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public SeatHoldRequest(String seatNumbers, Long userId) {
        this.seatNumbers = seatNumbers;
        this.userId = userId;
    }

    public String getSeatNumbers() {
        return seatNumbers;
    }

    public void setSeatNumbers(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getSeatNumberList() {
        if (seatNumbers == null || seatNumbers.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(seatNumbers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
