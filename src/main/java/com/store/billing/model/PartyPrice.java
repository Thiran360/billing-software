package com.store.billing.model;

public class PartyPrice {
    private int id;
    private int customerId;
    private int productId;
    private double customPrice;
    
    // For joining with product table to display product name
    private String productName;

    public PartyPrice() {}

    public PartyPrice(int id, int customerId, int productId, double customPrice) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.customPrice = customPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public double getCustomPrice() { return customPrice; }
    public void setCustomPrice(double customPrice) { this.customPrice = customPrice; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
