package com.store.billing.dao;

import com.store.billing.model.Purchase;
import com.store.billing.model.SupplierPayment;
import com.store.billing.util.DatabaseConnection;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PurchaseDAO {

    public boolean addPurchase(Purchase p) {
        String sql = "INSERT INTO purchases (supplier_name, supplier_mobile, supplier_address, invoice_number, " +
                     "purchase_date, total_amount, paid_amount, pending_amount, payment_mode, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int generatedId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, p.getSupplierName());
                pstmt.setString(2, p.getSupplierMobile());
                pstmt.setString(3, p.getSupplierAddress());
                pstmt.setString(4, p.getInvoiceNumber());
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                pstmt.setString(5, sdf.format(p.getPurchaseDate() != null ? p.getPurchaseDate() : new Date()));
                
                pstmt.setDouble(6, p.getTotalAmount());
                pstmt.setDouble(7, p.getPaidAmount());
                pstmt.setDouble(8, p.getPendingAmount());
                pstmt.setString(9, p.getPaymentMode());
                pstmt.setString(10, p.getDescription());
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                        p.setId(generatedId);
                    }
                }
            }

            // If initial paid amount > 0, log an initial payment record
            if (generatedId > 0 && p.getPaidAmount() > 0.009) {
                String paymentSql = "INSERT INTO supplier_payments (purchase_id, supplier_name, amount_paid, payment_mode, notes) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmtPay = conn.prepareStatement(paymentSql)) {
                    pstmtPay.setInt(1, generatedId);
                    pstmtPay.setString(2, p.getSupplierName());
                    pstmtPay.setDouble(3, p.getPaidAmount());
                    pstmtPay.setString(4, p.getPaymentMode());
                    pstmtPay.setString(5, "Initial payment on purchase");
                    pstmtPay.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public boolean updatePurchase(Purchase p) {
        String sql = "UPDATE purchases SET supplier_name=?, supplier_mobile=?, supplier_address=?, invoice_number=?, " +
                     "purchase_date=?, total_amount=?, paid_amount=?, pending_amount=?, payment_mode=?, description=? " +
                     "WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getSupplierName());
            pstmt.setString(2, p.getSupplierMobile());
            pstmt.setString(3, p.getSupplierAddress());
            pstmt.setString(4, p.getInvoiceNumber());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            pstmt.setString(5, sdf.format(p.getPurchaseDate() != null ? p.getPurchaseDate() : new Date()));
            
            pstmt.setDouble(6, p.getTotalAmount());
            pstmt.setDouble(7, p.getPaidAmount());
            pstmt.setDouble(8, p.getPendingAmount());
            pstmt.setString(9, p.getPaymentMode());
            pstmt.setString(10, p.getDescription());
            pstmt.setInt(11, p.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePurchase(int id) {
        String deletePaymentsSql = "DELETE FROM supplier_payments WHERE purchase_id=?";
        String deletePurchaseSql = "DELETE FROM purchases WHERE id=?";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtPay = conn.prepareStatement(deletePaymentsSql)) {
                pstmtPay.setInt(1, id);
                pstmtPay.executeUpdate();
            }
            
            try (PreparedStatement pstmtPur = conn.prepareStatement(deletePurchaseSql)) {
                pstmtPur.setInt(1, id);
                pstmtPur.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public Purchase getPurchaseById(int id) {
        String sql = "SELECT * FROM purchases WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPurchase(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Purchase> getAllPurchases() {
        List<Purchase> list = new ArrayList<>();
        String sql = "SELECT * FROM purchases ORDER BY purchase_date DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPurchase(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Purchase> getPurchasesByDateRange(Date start, Date end) {
        List<Purchase> list = new ArrayList<>();
        String sql = "SELECT * FROM purchases WHERE purchase_date >= ? AND purchase_date <= ? ORDER BY purchase_date DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(1, sdf.format(start) + " 00:00:00");
            pstmt.setString(2, sdf.format(end) + " 23:59:59");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPurchase(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getAllSupplierNames() {
        List<String> suppliers = new ArrayList<>();
        String sql = "SELECT DISTINCT supplier_name FROM purchases WHERE supplier_name IS NOT NULL AND supplier_name != '' ORDER BY supplier_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(rs.getString("supplier_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return suppliers;
    }

    public boolean addSupplierPayment(int purchaseId, String supplierName, double amountPaid, String paymentMode, String notes) {
        return addSupplierPayment(purchaseId, supplierName, amountPaid, paymentMode, notes, new Date());
    }

    public boolean addSupplierPayment(int purchaseId, String supplierName, double amountPaid, String paymentMode, String notes, Date paymentDate) {
        String insertPaySql = "INSERT INTO supplier_payments (purchase_id, supplier_name, amount_paid, payment_mode, notes, payment_date) VALUES (?, ?, ?, ?, ?, ?)";
        String updatePurchaseSql = "UPDATE purchases SET paid_amount = paid_amount + ?, pending_amount = MAX(0, pending_amount - ?) WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtPay = conn.prepareStatement(insertPaySql)) {
                pstmtPay.setInt(1, purchaseId);
                pstmtPay.setString(2, supplierName);
                pstmtPay.setDouble(3, amountPaid);
                pstmtPay.setString(4, paymentMode);
                pstmtPay.setString(5, notes);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                pstmtPay.setString(6, sdf.format(paymentDate != null ? paymentDate : new Date()));
                pstmtPay.executeUpdate();
            }
            
            try (PreparedStatement pstmtPur = conn.prepareStatement(updatePurchaseSql)) {
                pstmtPur.setDouble(1, amountPaid);
                pstmtPur.setDouble(2, amountPaid);
                pstmtPur.setInt(3, purchaseId);
                pstmtPur.executeUpdate();
            }
            
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public double getTotalPendingAmountBySupplier(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty() || "All Suppliers".equalsIgnoreCase(supplierName.trim())) {
            String sql = "SELECT COALESCE(SUM(pending_amount), 0) FROM purchases";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0.0;
        }

        String sql = "SELECT COALESCE(SUM(pending_amount), 0) FROM purchases WHERE LOWER(TRIM(supplier_name)) = LOWER(TRIM(?))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, supplierName.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<SupplierPayment> getPaymentsForPurchase(int purchaseId) {
        List<SupplierPayment> list = new ArrayList<>();
        String sql = "SELECT * FROM supplier_payments WHERE purchase_id = ? ORDER BY payment_date DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, purchaseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SupplierPayment sp = new SupplierPayment();
                    sp.setId(rs.getInt("id"));
                    sp.setPurchaseId(rs.getInt("purchase_id"));
                    sp.setSupplierName(rs.getString("supplier_name"));
                    sp.setAmountPaid(rs.getDouble("amount_paid"));
                    sp.setPaymentMode(rs.getString("payment_mode"));
                    sp.setPaymentDate(rs.getTimestamp("payment_date"));
                    sp.setNotes(rs.getString("notes"));
                    list.add(sp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Purchase mapResultSetToPurchase(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setSupplierName(rs.getString("supplier_name"));
        p.setSupplierMobile(rs.getString("supplier_mobile"));
        p.setSupplierAddress(rs.getString("supplier_address"));
        p.setInvoiceNumber(rs.getString("invoice_number"));
        p.setPurchaseDate(rs.getTimestamp("purchase_date"));
        p.setTotalAmount(rs.getDouble("total_amount"));
        p.setPaidAmount(rs.getDouble("paid_amount"));
        p.setPendingAmount(rs.getDouble("pending_amount"));
        p.setPaymentMode(rs.getString("payment_mode"));
        p.setDescription(rs.getString("description"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
