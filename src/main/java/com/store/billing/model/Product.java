package com.store.billing.model;

public class Product {
    private int id;
    private String barcode;          // Code (Column A)
    private String name;             // Saving Name (Column B)
    private String unitType;         // Unit (Column C)
    private double lineRate;         // Line rate (Column D)
    private double wholePrice;       // Whole (Column E)
    private double wholeDis;         // WHOLE(Dis) (Column F)
    private int wholeQtyThreshold;   // No. of Pic for Whole (Column G)
    private double retailPrice;      // Retail (Column H)
    private double retailDis;        // Retail(Dis) (Column I)
    private int retailQtyThreshold;  // No. of Pic for Retail (Column J)
    private String category;         // Category (Column K)
    private String displayName;      // Display name (Column L)
    private String tamilName;        // Print name[tamil] (Column M)
    private String englishName;      // Print Name[english] (Column N)
    private double purchaseRate;     // Purchase rate (Column O)
    private int stockQuantity;       // Stock Quantity

    // Default constructor for DB mapping
    public Product() {}

    // Full constructor with all 17 fields
    public Product(int id, String barcode, String name, int stockQuantity, String unitType, 
                   String tamilName, double purchaseRate, double lineRate, double wholePrice, 
                   double wholeDis, double retailPrice, double retailDis, int wholeQtyThreshold, 
                   int retailQtyThreshold, String category, String displayName, String englishName) {
        this.id = id;
        this.barcode = barcode != null ? barcode.trim() : "";
        this.name = name != null ? name.trim() : "";
        this.stockQuantity = stockQuantity;
        this.unitType = unitType != null ? unitType.trim() : "";
        this.tamilName = tamilName != null ? tamilName.trim() : "";
        this.purchaseRate = purchaseRate;
        this.lineRate = lineRate;
        this.wholePrice = wholePrice;
        this.wholeDis = wholeDis;
        this.retailPrice = retailPrice;
        this.retailDis = retailDis;
        this.wholeQtyThreshold = wholeQtyThreshold;
        this.retailQtyThreshold = retailQtyThreshold;
        this.category = category != null ? category.trim() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
        this.englishName = englishName != null ? englishName.trim() : "";
    }

    // Backwards-compatible constructor
    public Product(int id, String barcode, String name, int stockQuantity, String unitType, 
                   String tamilName, double purchaseRate, double lineRate, double wholePrice, 
                   double wholeDis, double retailPrice, double retailDis, int wholeQtyThreshold, 
                   int retailQtyThreshold) {
        this(id, barcode, name, stockQuantity, unitType, tamilName, purchaseRate, lineRate, 
             wholePrice, wholeDis, retailPrice, retailDis, wholeQtyThreshold, retailQtyThreshold, 
             "", "", name);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBarcode() { return barcode != null ? barcode : ""; }
    public void setBarcode(String barcode) { this.barcode = barcode != null ? barcode.trim() : ""; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name != null ? name.trim() : ""; }

    public String getTamilName() { return tamilName != null ? tamilName : ""; }
    public void setTamilName(String tamilName) { this.tamilName = tamilName != null ? tamilName.trim() : ""; }

    public String getUnitType() { return unitType != null ? unitType : ""; }
    public void setUnitType(String unitType) { this.unitType = unitType != null ? unitType.trim() : ""; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public double getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(double purchaseRate) { this.purchaseRate = purchaseRate; }

    public double getLineRate() { return lineRate; }
    public void setLineRate(double lineRate) { this.lineRate = lineRate; }

    public double getWholePrice() { return wholePrice; }
    public void setWholePrice(double wholePrice) { this.wholePrice = wholePrice; }

    public double getWholeDis() { return wholeDis; }
    public void setWholeDis(double wholeDis) { this.wholeDis = wholeDis; }

    public double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(double retailPrice) { this.retailPrice = retailPrice; }

    public double getRetailDis() { return retailDis; }
    public void setRetailDis(double retailDis) { this.retailDis = retailDis; }

    public int getWholeQtyThreshold() { return wholeQtyThreshold; }
    public void setWholeQtyThreshold(int wholeQtyThreshold) { this.wholeQtyThreshold = wholeQtyThreshold; }

    public int getRetailQtyThreshold() { return retailQtyThreshold; }
    public void setRetailQtyThreshold(int retailQtyThreshold) { this.retailQtyThreshold = retailQtyThreshold; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category != null ? category.trim() : ""; }

    public String getDisplayName() { return displayName != null ? displayName : ""; }
    public void setDisplayName(String displayName) { this.displayName = displayName != null ? displayName.trim() : ""; }

    public String getEnglishName() { return englishName != null ? englishName : ""; }
    public void setEnglishName(String englishName) { this.englishName = englishName != null ? englishName.trim() : ""; }

    public double getPrice(String priceType) {
        if (priceType == null) return wholePrice;
        switch (priceType) {
            case "Purchase rate": return purchaseRate;
            case "Line rate": return lineRate;
            case "WHOLE(Dis)": return wholeDis;
            case "Retail": return retailPrice;
            case "Retail(Dis)": return retailDis;
            case "Whole":
            default: return wholePrice;
        }
    }

    @Override
    public String toString() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        if (tamilName != null && !tamilName.trim().isEmpty()) {
            return tamilName;
        }
        if (englishName != null && !englishName.trim().isEmpty()) {
            return englishName;
        }
        return name;
    }
}
