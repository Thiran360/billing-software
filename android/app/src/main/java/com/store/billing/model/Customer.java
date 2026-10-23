package com.store.billing.model;

import java.io.Serializable;

public class Customer implements Serializable {
    private int id;
    private String name;
    private String mobile;
    private String address;
    private double pendingAmount;

    public Customer() {}

    public Customer(int id, String name, String mobile, String address, double pendingAmount) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.pendingAmount = pendingAmount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile != null ? mobile : ""; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address; }

    public double getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(double pendingAmount) { this.pendingAmount = pendingAmount; }

    @Override
    public String toString() {
        return name + (mobile != null && !mobile.isEmpty() ? " - " + mobile : "");
    }
}
