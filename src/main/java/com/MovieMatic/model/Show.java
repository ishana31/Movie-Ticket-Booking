package com.MovieMatic.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private Movie movie;

    private LocalDate showDate;

    private LocalTime showTime;

    private String screen;

    private double price;
    
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Movie getMovie() {
		return movie;
	}

	public void setMovie(Movie movie) {
		this.movie = movie;
	}

	public LocalDate getShowDate() {
		return showDate;
	}

	public void setShowDate(LocalDate showDate) {
		this.showDate = showDate;
	}

	public LocalTime getShowTime() {
		return showTime;
	}

	public void setShowTime(LocalTime showTime) {
		this.showTime = showTime;
	}

	public String getScreen() {
		return screen;
	}

	public void setScreen(String screen) {
		this.screen = screen;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
}
