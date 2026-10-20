package com.store.billing.model;

import java.util.Date;
import java.util.List;

public class Bill {
    private int id;
    private int dailyBillNo;
    private String customerName;
    private double totalAmount;
    private Date billDate;
    private String paymentMode;
    private double paidAmount;
    private double pendingAmount;
    private double customerTotalPending;
    private double cashAmount;
    private double gpayAmount;
    private List<BillItem> items;
    private Customer customerObj;
    private String status = "Printed";
    private boolean showBalanceOnBill = true;
    private double discount = 0.0;
    public Bill() {}

    public Bill(int id, String customerName, double totalAmount, Date billDate, String paymentMode, double paidAmount, double pendingAmount) {
        this.id = id;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.billDate = billDate;
        this.paymentMode = paymentMode;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDailyBillNo() { return dailyBillNo; }
    public void setDailyBillNo(int dailyBillNo) { this.dailyBillNo = dailyBillNo; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Date getBillDate() { return billDate; }
    public void setBillDate(Date billDate) { this.billDate = billDate; }
    
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    public double getCustomerTotalPending() { return customerTotalPending; }
    public void setCustomerTotalPending(double customerTotalPending) { this.customerTotalPending = customerTotalPending; }

    public double getCashAmount() { return cashAmount; }
    public void setCashAmount(double cashAmount) { this.cashAmount = cashAmount; }

    public double getGpayAmount() { return gpayAmount; }
    public void setGpayAmount(double gpayAmount) { this.gpayAmount = gpayAmount; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
    
    public Customer getCustomerObj() { return customerObj; }
    public void setCustomerObj(Customer customerObj) { this.customerObj = customerObj; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isShowBalanceOnBill() { return showBalanceOnBill; }
    public void setShowBalanceOnBill(boolean showBalanceOnBill) { this.showBalanceOnBill = showBalanceOnBill; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
