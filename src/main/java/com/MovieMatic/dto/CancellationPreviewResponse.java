package com.MovieMatic.dto;

public class CancellationPreviewResponse {
    private double totalPaid;
    private int refundPercentage;
    private double refundAmount;
    private double cancellationFee;

    public CancellationPreviewResponse() {
    }

    public CancellationPreviewResponse(double totalPaid, int refundPercentage, double refundAmount, double cancellationFee) {
        this.totalPaid = totalPaid;
        this.refundPercentage = refundPercentage;
        this.refundAmount = refundAmount;
        this.cancellationFee = cancellationFee;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
    }

    public double getTotalAmount() {
        return totalPaid;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalPaid = totalAmount;
    }

    public int getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(int refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public double getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(double cancellationFee) {
        this.cancellationFee = cancellationFee;
    }
}
