package com.store.billing;

import com.store.billing.dao.BillDAO;
import com.store.billing.dao.ProductDAO;
import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;
import com.store.billing.model.Product;
import com.store.billing.util.DatabaseConnection;

import java.util.ArrayList;

public class TestBilling {
    public static void main(String[] args) {
        try {
            DatabaseConnection.getConnection(); // initialize
            
            ProductDAO pDao = new ProductDAO();
            Product p = new Product(0, "123", "TestProduct", 100, "Piece", "Test", 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0, 0);
            pDao.addProduct(p);
            
            p = pDao.getProductByBarcode("123");
            System.out.println("Product ID: " + p.getId());
            
            Bill bill = new Bill();
            bill.setCustomerName("Test Customer");
            bill.setTotalAmount(10.0);
            bill.setPaymentMode("Cash");
            
            ArrayList<BillItem> items = new ArrayList<>();
            items.add(new BillItem(0, 0, p.getId(), 1, 10.0, 10.0, "Piece"));
            bill.setItems(items);
            
            BillDAO bDao = new BillDAO();
            boolean success = bDao.createBill(bill);
            System.out.println("Create Bill Success: " + success);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
