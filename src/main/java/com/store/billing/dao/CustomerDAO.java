package com.store.billing.dao;

import com.store.billing.model.Bill;
import com.store.billing.model.Customer;
import com.store.billing.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                double pendingAmount = 0.0;
                try {
                    pendingAmount = rs.getDouble("pending_amount");
                } catch (SQLException ignore) {}
                String tamilName = "";
                try {
                    tamilName = rs.getString("tamil_name");
                    if (tamilName == null) tamilName = "";
                } catch (SQLException ignore) {}
                customers.add(new Customer(
                    rs.getInt("id"),
                    rs.getString("name"),
                    tamilName,
                    rs.getString("mobile"),
                    rs.getString("address"),
                    pendingAmount
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public boolean isCustomerIdExists(int id, int excludeId) {
        String sql = "SELECT COUNT(*) FROM customers WHERE id = ? AND id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getNextCustomerId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM customers";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public boolean addCustomer(Customer customer) {
        String sql = customer.getId() > 0 ?
                "INSERT INTO customers (id, name, tamil_name, mobile, address, pending_amount) VALUES (?, ?, ?, ?, ?, ?)" :
                "INSERT INTO customers (name, tamil_name, mobile, address, pending_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int idx = 1;
            if (customer.getId() > 0) {
                pstmt.setInt(idx++, customer.getId());
            }
            pstmt.setString(idx++, customer.getName());
            pstmt.setString(idx++, customer.getTamilName() != null ? customer.getTamilName().trim() : "");
            if (customer.getMobile() != null && !customer.getMobile().trim().isEmpty()) {
                pstmt.setString(idx++, customer.getMobile().trim());
            } else {
                pstmt.setNull(idx++, java.sql.Types.VARCHAR);
            }
            pstmt.setString(idx++, customer.getAddress() != null ? customer.getAddress().trim() : "");
            pstmt.setDouble(idx++, customer.getPendingAmount());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int addCustomerAndGetId(Customer customer) {
        String sql = "INSERT INTO customers (name, tamil_name, mobile, address, pending_amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getTamilName() != null ? customer.getTamilName().trim() : "");
            if (customer.getMobile() != null && !customer.getMobile().trim().isEmpty()) {
                pstmt.setString(3, customer.getMobile().trim());
            } else {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            }
            pstmt.setString(4, customer.getAddress() != null ? customer.getAddress().trim() : "");
            pstmt.setDouble(5, customer.getPendingAmount());
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateCustomer(Customer customer) {
        return updateCustomer(customer, customer.getId());
    }

    public boolean updateCustomer(Customer customer, int oldId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. If Customer ID changed, update all linked bills first
            if (customer.getId() != oldId && oldId > 0) {
                String updateBillsSql = "UPDATE bills SET customer_id = ? WHERE customer_id = ?";
                try (PreparedStatement pstmtBills = conn.prepareStatement(updateBillsSql)) {
                    pstmtBills.setInt(1, customer.getId());
                    pstmtBills.setInt(2, oldId);
                    pstmtBills.executeUpdate();
                }
            }

            // 2. Update customer record
            String sql = "UPDATE customers SET id=?, name=?, tamil_name=?, mobile=?, address=?, pending_amount=? WHERE id=?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, customer.getId());
                pstmt.setString(2, customer.getName());
                pstmt.setString(3, customer.getTamilName() != null ? customer.getTamilName().trim() : "");
                if (customer.getMobile() != null && !customer.getMobile().trim().isEmpty()) {
                    pstmt.setString(4, customer.getMobile().trim());
                } else {
                    pstmt.setNull(4, java.sql.Types.VARCHAR);
                }
                pstmt.setString(5, customer.getAddress() != null ? customer.getAddress().trim() : "");
                pstmt.setDouble(6, customer.getPendingAmount());
                pstmt.setInt(7, oldId);
                pstmt.executeUpdate();
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

    public boolean deleteCustomer(int id) {
        String sql = "DELETE FROM customers WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePendingAmount(int customerId, double amountToAdd) {
        String sql = "UPDATE customers SET pending_amount = MAX(0.0, COALESCE(pending_amount, 0) + ?) WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amountToAdd);
            pstmt.setInt(2, customerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setPendingAmount(int customerId, double exactPendingAmount) {
        String sql = "UPDATE customers SET pending_amount = ? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, Math.max(0.0, exactPendingAmount));
            pstmt.setInt(2, customerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void syncCustomerPendingBalance(int customerId) {
        if (customerId <= 0) return;
        String sql = "SELECT MAX(0.0, COALESCE(SUM(CASE WHEN pending_amount > 0 THEN pending_amount ELSE 0 END), 0)) FROM bills WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double actualPending = Math.max(0.0, rs.getDouble(1));
                    setPendingAmount(customerId, actualPending);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Bill> getBillsByCustomerId(int customerId) {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills WHERE customer_id = ? ORDER BY bill_date DESC, id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bill b = new Bill();
                    b.setId(rs.getInt("id"));
                    b.setCustomerName(rs.getString("customer_name"));
                    b.setTotalAmount(rs.getDouble("total_amount"));
                    b.setPaymentMode(rs.getString("payment_mode"));
                    b.setBillDate(rs.getTimestamp("bill_date"));
                    double paid = 0, pending = 0;
                    try { paid = rs.getDouble("paid_amount"); } catch (SQLException ignore) {}
                    try { pending = rs.getDouble("pending_amount"); } catch (SQLException ignore) {}
                    b.setPaidAmount(paid);
                    b.setPendingAmount(pending);
                    try { b.setDailyBillNo(rs.getInt("daily_bill_no")); } catch (SQLException ignore) {}
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean recordCustomerPayment(int customerId, String customerName, double amountPaid, String paymentMode, String notes) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Get current pending
            double currentPending = 0.0;
            String getPendSql = "SELECT pending_amount FROM customers WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(getPendSql)) {
                pstmt.setInt(1, customerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentPending = rs.getDouble("pending_amount");
                    }
                }
            }

            double newPending = Math.max(0.0, currentPending - amountPaid);

            // 2. Update customer pending_amount
            String updateCustSql = "UPDATE customers SET pending_amount = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateCustSql)) {
                pstmt.setDouble(1, newPending);
                pstmt.setInt(2, customerId);
                pstmt.executeUpdate();
            }

            // 3. Insert payment transaction into bills table
            String insertBillSql = "INSERT INTO bills (customer_id, customer_name, total_amount, paid_amount, pending_amount, payment_mode, bill_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            int newBillId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertBillSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, customerId);
                pstmt.setString(2, customerName != null ? customerName : "Customer #" + customerId);
                pstmt.setDouble(3, amountPaid);
                pstmt.setDouble(4, amountPaid);
                pstmt.setDouble(5, 0.0);
                pstmt.setString(6, paymentMode != null ? paymentMode : "Cash");
                java.text.SimpleDateFormat sdfSql = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                pstmt.setString(7, sdfSql.format(new java.util.Date()));
                pstmt.setString(8, "Payment");
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newBillId = rs.getInt(1);
                    }
                }
            }

            // 4. Insert bill_item for payment received
            if (newBillId > 0) {
                String itemSql = "INSERT INTO bill_items (bill_id, product_id, quantity, unit_price, subtotal, tamil_name) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(itemSql)) {
                    pstmt.setInt(1, newBillId);
                    pstmt.setInt(2, 0);
                    pstmt.setDouble(3, 1.0);
                    pstmt.setDouble(4, amountPaid);
                    pstmt.setDouble(5, amountPaid);
                    String itemDesc = (notes != null && !notes.trim().isEmpty()) ? "Cash Received: " + notes.trim() : "Cash Received / \u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF \u0BB5\u0BB0\u0BB5\u0BC1";
                    pstmt.setString(6, itemDesc);
                    pstmt.executeUpdate();
                }
            }

            // 5. Reduce pending amount on customer's unpaid bills (FIFO)
            double remainingPayment = amountPaid;
            String fetchUnpaidSql = "SELECT id, paid_amount, pending_amount FROM bills WHERE customer_id = ? AND pending_amount > 0.001 AND id != ? ORDER BY bill_date ASC, id ASC";
            try (PreparedStatement pstmt = conn.prepareStatement(fetchUnpaidSql)) {
                pstmt.setInt(1, customerId);
                pstmt.setInt(2, newBillId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    List<int[]> billIds = new ArrayList<>();
                    List<double[]> billVals = new ArrayList<>();
                    while (rs.next() && remainingPayment > 0.001) {
                        int bId = rs.getInt("id");
                        double bPaid = rs.getDouble("paid_amount");
                        double bPend = rs.getDouble("pending_amount");
                        double payTowardsBill = Math.min(remainingPayment, bPend);
                        billIds.add(new int[]{bId});
                        billVals.add(new double[]{bPaid + payTowardsBill, Math.max(0.0, bPend - payTowardsBill)});
                        remainingPayment -= payTowardsBill;
                    }
                    for (int i = 0; i < billIds.size(); i++) {
                        String updateBillSql = "UPDATE bills SET paid_amount = ?, pending_amount = ? WHERE id = ?";
                        try (PreparedStatement uPstmt = conn.prepareStatement(updateBillSql)) {
                            uPstmt.setDouble(1, billVals.get(i)[0]);
                            uPstmt.setDouble(2, billVals.get(i)[1]);
                            uPstmt.setInt(3, billIds.get(i)[0]);
                            uPstmt.executeUpdate();
                        }
                    }
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
}
