package com.store.billing.model;

public class Unit {
    private int id;
    private String nameEn;
    private String nameTa;

    public Unit() {}

    public Unit(int id, String nameEn, String nameTa) {
        this.id = id;
        this.nameEn = nameEn != null ? nameEn.trim() : "";
        this.nameTa = nameTa != null ? nameTa.trim() : "";
    }

    public Unit(String nameEn, String nameTa) {
        this(0, nameEn, nameTa);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn != null ? nameEn.trim() : ""; }

    public String getNameTa() { return nameTa; }
    public void setNameTa(String nameTa) { this.nameTa = nameTa != null ? nameTa.trim() : ""; }

    @Override
    public String toString() {
        if (nameTa != null && !nameTa.isEmpty() && !nameTa.equalsIgnoreCase(nameEn)) {
            return nameEn + " - " + nameTa;
        }
        return nameEn;
    }
}
