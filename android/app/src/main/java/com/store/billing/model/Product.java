package com.store.billing.model;

import java.io.Serializable;

public class Product implements Serializable {
    private int id;
    private String barcode;
    private String name;
    private String tamilName;
    private int stockQuantity;
    private String unitType;
    private double retailPrice;
    private double wholePrice;
    private double lineRate;

    public Product() {}

    public Product(int id, String barcode, String name, String tamilName, int stockQuantity, String unitType, double retailPrice, double wholePrice) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.tamilName = tamilName;
        this.stockQuantity = stockQuantity;
        this.unitType = unitType;
        this.retailPrice = retailPrice;
        this.wholePrice = wholePrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBarcode() { return barcode != null ? barcode : ""; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getTamilName() { return tamilName != null ? tamilName : ""; }
    public void setTamilName(String tamilName) { this.tamilName = tamilName; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getUnitType() { return unitType != null && !unitType.isEmpty() ? unitType : "piece"; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }

    public double getWholePrice() { return wholePrice; }
    public void setWholePrice(double wholePrice) { this.wholePrice = wholePrice; }

    public double getLineRate() { return lineRate; }
    public void setLineRate(double lineRate) { this.lineRate = lineRate; }

    public String getDisplayName() {
        if (tamilName != null && !tamilName.trim().isEmpty()) {
            return name + " (" + tamilName + ")";
        }
        return name;
    }
}
