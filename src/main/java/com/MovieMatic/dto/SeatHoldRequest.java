package com.MovieMatic.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSetter;

public class SeatHoldRequest {

    private String seatNumbers;

    public SeatHoldRequest() {
    }

    public SeatHoldRequest(String seatNumbers) {
        this.seatNumbers = seatNumbers;
    }

    public String getSeatNumbers() {
        return seatNumbers;
    }

    @JsonSetter("seatNumbers")
    public void setSeatNumbers(Object raw) {
        if (raw instanceof Collection<?>) {
            this.seatNumbers = ((Collection<?>) raw).stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        } else if (raw != null) {
            this.seatNumbers = raw.toString();
        } else {
            this.seatNumbers = null;
        }
    }

    public List<String> getSeatList() {
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
