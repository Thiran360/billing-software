package com.store.billing.model;

public class ReportItem {
    private String category;
    private String productName;
    private int quantitySold;
    private double totalSales;
    private double totalProfit;

    public ReportItem(String category, String productName, int quantitySold, double totalSales, double totalProfit) {
        this.category = category;
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.totalSales = totalSales;
        this.totalProfit = totalProfit;
    }

    public String getCategory() { return category; }
    public String getProductName() { return productName; }
    public int getQuantitySold() { return quantitySold; }
    public double getTotalSales() { return totalSales; }
    public double getTotalProfit() { return totalProfit; }
}
