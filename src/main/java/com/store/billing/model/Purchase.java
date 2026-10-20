package com.store.billing.model;

import java.util.Date;

public class Purchase {
    private int id;
    private String supplierName;
    private String supplierMobile;
    private String supplierAddress;
    private String invoiceNumber;
    private Date purchaseDate;
    private double totalAmount;
    private double paidAmount;
    private double pendingAmount;
    private String paymentMode;
    private String description;
    private Date createdAt;

    public Purchase() {}

    public Purchase(int id, String supplierName, String supplierMobile, String supplierAddress,
                    String invoiceNumber, Date purchaseDate, double totalAmount, double paidAmount,
                    double pendingAmount, String paymentMode, String description) {
        this.id = id;
        this.supplierName = supplierName;
        this.supplierMobile = supplierMobile;
        this.supplierAddress = supplierAddress;
        this.invoiceNumber = invoiceNumber;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
        this.paymentMode = paymentMode;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierMobile() { return supplierMobile != null ? supplierMobile : ""; }
    public void setSupplierMobile(String supplierMobile) { this.supplierMobile = supplierMobile; }

    public String getSupplierAddress() { return supplierAddress != null ? supplierAddress : ""; }
    public void setSupplierAddress(String supplierAddress) { this.supplierAddress = supplierAddress; }

    public String getInvoiceNumber() { return invoiceNumber != null ? invoiceNumber : ""; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Date getPurchaseDate() { return purchaseDate != null ? purchaseDate : new Date(); }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    public String getPaymentMode() { return paymentMode != null ? paymentMode : "Cash"; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getStatus() {
        if (pendingAmount <= 0.009) {
            return "Paid";
        } else if (paidAmount > 0.009) {
            return "Partial";
        } else {
            return "Unpaid";
        }
    }
}
