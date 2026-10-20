package com.store.billing.model;

import java.util.Date;

public class SupplierPayment {
    private int id;
    private int purchaseId;
    private String supplierName;
    private double amountPaid;
    private String paymentMode;
    private Date paymentDate;
    private String notes;

    public SupplierPayment() {}

    public SupplierPayment(int id, int purchaseId, String supplierName, double amountPaid, String paymentMode, Date paymentDate, String notes) {
        this.id = id;
        this.purchaseId = purchaseId;
        this.supplierName = supplierName;
        this.amountPaid = amountPaid;
        this.paymentMode = paymentMode;
        this.paymentDate = paymentDate;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public String getSupplierName() { return supplierName != null ? supplierName : ""; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentMode() { return paymentMode != null ? paymentMode : "Cash"; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public Date getPaymentDate() { return paymentDate != null ? paymentDate : new Date(); }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }
}
