package com.store.billing.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Bill implements Serializable {
    private int id;
    private int customerId;
    private String customerName;
    private double totalAmount;
    private double paidAmount;
    private double pendingAmount;
    private String paymentMode;
    private String billDate;
    private List<BillItem> items = new ArrayList<>();

    public Bill() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName != null ? customerName : "Walk-in Customer"; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    public String getPaymentMode() { return paymentMode != null ? paymentMode : "CASH"; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getBillDate() { return billDate != null ? billDate : ""; }
    public void setBillDate(String billDate) { this.billDate = billDate; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
}
