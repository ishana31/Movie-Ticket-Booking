package com.MovieMatic.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;

@Entity
public class Booking {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Show show;

    private String seatNumbers;

    private double totalAmount;
    
    @Column(nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "status", nullable = false)
    private String status = "CONFIRMED";

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refund_amount")
    private Double refundAmount;

    @Column(name = "cancellation_fee")
    private Double cancellationFee;

    @Column(name = "refund_status")
    private String refundStatus;
    
    @PrePersist
    protected void onCreate() {
        if (bookingTime == null) {
            bookingTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "CONFIRMED";
        }
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Show getShow() {
		return show;
	}

	public void setShow(Show show) {
		this.show = show;
	}

	public String getSeatNumbers() {
		return seatNumbers;
	}

	public void setSeatNumbers(String seatNumbers) {
		this.seatNumbers = seatNumbers;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public LocalDateTime getBookingTime() {
		return bookingTime;
	}

	public void setBookingTime(LocalDateTime bookingTime) {
		this.bookingTime = bookingTime;
	}

	public String getStatus() {
		return status != null ? status : "CONFIRMED";
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCancelledAt() {
		return cancelledAt;
	}

	public void setCancelledAt(LocalDateTime cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public Double getRefundAmount() {
		return refundAmount;
	}

	public void setRefundAmount(Double refundAmount) {
		this.refundAmount = refundAmount;
	}

	public Double getCancellationFee() {
		return cancellationFee;
	}

	public void setCancellationFee(Double cancellationFee) {
		this.cancellationFee = cancellationFee;
	}

	public String getRefundStatus() {
		return refundStatus;
	}

	public void setRefundStatus(String refundStatus) {
		this.refundStatus = refundStatus;
	}

	public Integer getRefundPercentage() {
		if (totalAmount > 0 && refundAmount != null) {
			return (int) Math.round((refundAmount / totalAmount) * 100);
		}
		return null;
	}
}
