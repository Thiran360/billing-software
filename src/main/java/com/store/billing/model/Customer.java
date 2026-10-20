package com.store.billing.model;

public class Customer {
    private int id;
    private String name;
    private String tamilName;
    private String mobile;
    private String address;
    private double pendingAmount;

    public Customer() {}

    public Customer(int id, String name, String mobile, String address) {
        this(id, name, "", mobile, address, 0.0);
    }
    
    public Customer(int id, String name, String mobile, String address, double pendingAmount) {
        this(id, name, "", mobile, address, pendingAmount);
    }

    public Customer(int id, String name, String tamilName, String mobile, String address, double pendingAmount) {
        this.id = id;
        this.name = name;
        this.tamilName = tamilName;
        this.mobile = mobile;
        this.address = address;
        this.pendingAmount = pendingAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTamilName() { return tamilName != null ? tamilName : ""; }
    public void setTamilName(String tamilName) { this.tamilName = tamilName; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    @Override
    public String toString() {
        if (tamilName != null && !tamilName.trim().isEmpty()) {
            return tamilName.trim();
        }
        return name != null ? name : ""; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof Customer) {
            Customer other = (Customer) o;
            if (this.id > 0 && other.id > 0) return this.id == other.id;
            return this.name != null && this.name.equalsIgnoreCase(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id > 0 ? Integer.hashCode(id) : (name != null ? name.toLowerCase().hashCode() : 0);
    }
}
