package com.store.billing.model;

import java.io.Serializable;

public class BillItem implements Serializable {
    private int id;
    private int billId;
    private int productId;
    private String productName;
    private String tamilName;
    private String unitType;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public BillItem() {}

    public BillItem(int productId, String productName, String tamilName, String unitType, int quantity, double unitPrice, double subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.tamilName = tamilName;
        this.unitType = unitType;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName != null ? productName : ""; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getTamilName() { return tamilName != null ? tamilName : ""; }
    public void setTamilName(String tamilName) { this.tamilName = tamilName; }

    public String getUnitType() { return unitType != null ? unitType : "piece"; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.quantity * this.unitPrice;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.subtotal = this.quantity * this.unitPrice;
    }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
}
