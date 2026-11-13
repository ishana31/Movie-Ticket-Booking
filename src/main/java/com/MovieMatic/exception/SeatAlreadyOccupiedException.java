package com.MovieMatic.exception;

/**
 * Thrown when requested seats are already held by another user or already booked.
 */
public class SeatAlreadyOccupiedException extends RuntimeException {

    public SeatAlreadyOccupiedException(String message) {
        super(message);
    }
}
