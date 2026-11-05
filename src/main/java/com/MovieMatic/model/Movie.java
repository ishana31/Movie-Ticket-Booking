package com.MovieMatic.model;

import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Movie {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String genre;

    private String language;
    
    private LocalDate releaseDate;

    public LocalDate getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(LocalDate releaseDate) {
		this.releaseDate = releaseDate;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
    private byte[] posterData;
	
	@Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] backgroundData;

	private int duration; // in minutes

    public byte[] getBackgroundData() {
		return backgroundData;
	}

	public void setBackgroundData(byte[] backgroundData) {
		this.backgroundData = backgroundData;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public byte[] getPosterData() {
		return posterData;
	}

	public void setPosterData(byte[] posterData) {
		this.posterData = posterData;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

}
