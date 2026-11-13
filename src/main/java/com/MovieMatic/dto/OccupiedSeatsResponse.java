package com.MovieMatic.dto;

import java.util.ArrayList;
import java.util.List;

public class OccupiedSeatsResponse {

    private List<String> bookedSeats = new ArrayList<>();
    private List<String> heldSeats = new ArrayList<>();

    public OccupiedSeatsResponse() {
    }

    public OccupiedSeatsResponse(List<String> bookedSeats, List<String> heldSeats) {
        this.bookedSeats = bookedSeats != null ? bookedSeats : new ArrayList<>();
        this.heldSeats = heldSeats != null ? heldSeats : new ArrayList<>();
    }

    public List<String> getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(List<String> bookedSeats) {
        this.bookedSeats = bookedSeats != null ? bookedSeats : new ArrayList<>();
    }

    public List<String> getHeldSeats() {
        return heldSeats;
    }

    public void setHeldSeats(List<String> heldSeats) {
        this.heldSeats = heldSeats != null ? heldSeats : new ArrayList<>();
    }
}
