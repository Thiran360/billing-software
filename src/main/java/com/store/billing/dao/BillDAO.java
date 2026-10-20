package com.store.billing.dao;

import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;
import com.store.billing.util.DatabaseConnection;

import java.sql.*;
import java.util.List;

public class BillDAO {

    public boolean createBill(Bill bill) {
        if (bill.getBillDate() == null) {
            bill.setBillDate(new java.util.Date());
        }
        if (bill.getDailyBillNo() <= 0) {
            bill.setDailyBillNo(getNextDailyBillNo(bill.getBillDate()));
        }
        String insertBillSql = "INSERT INTO bills (customer_name, total_amount, payment_mode, paid_amount, pending_amount, customer_id, status, daily_bill_no, bill_date, show_balance_on_bill, discount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO bill_items (bill_id, product_id, quantity, unit_price, subtotal, tamil_name, unit_type) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Insert Bill
            int generatedBillId = -1;
            try (PreparedStatement pstmtBill = conn.prepareStatement(insertBillSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtBill.setString(1, bill.getCustomerName());
                pstmtBill.setDouble(2, bill.getTotalAmount());
                pstmtBill.setString(3, bill.getPaymentMode() != null ? bill.getPaymentMode() : "Cash");
                pstmtBill.setDouble(4, bill.getPaidAmount());
                pstmtBill.setDouble(5, bill.getPendingAmount());
                if (bill.getCustomerObj() != null) {
                    pstmtBill.setInt(6, bill.getCustomerObj().getId());
                } else {
                    pstmtBill.setNull(6, java.sql.Types.INTEGER);
                }
                pstmtBill.setString(7, bill.getStatus());
                pstmtBill.setInt(8, bill.getDailyBillNo());
                java.text.SimpleDateFormat sdfSql = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                pstmtBill.setString(9, sdfSql.format(bill.getBillDate()));
                pstmtBill.setInt(10, bill.isShowBalanceOnBill() ? 1 : 0);
                pstmtBill.setDouble(11, bill.getDiscount());
                pstmtBill.executeUpdate();
                
                try (ResultSet rs = pstmtBill.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedBillId = rs.getInt(1);
                        bill.setId(generatedBillId);
                    } else {
                        throw new SQLException("Creating bill failed, no ID obtained.");
                    }
                }
            }
            
            // 2. Insert Bill Items and update product stock
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
            
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {
                for (BillItem item : bill.getItems()) {
                    pstmtItem.setInt(1, generatedBillId);
                    pstmtItem.setInt(2, item.getProductId());
                    pstmtItem.setDouble(3, item.getQuantity());
                    pstmtItem.setDouble(4, item.getUnitPrice());
                    pstmtItem.setDouble(5, item.getSubtotal());
                    pstmtItem.setString(6, item.getTamilName());
                    pstmtItem.setString(7, item.getUnitType() != null ? item.getUnitType() : "piece");
                    pstmtItem.addBatch();
                    
                    // Decrease stock unconditionally without failing if stock is low or negative
                    if (item.getProductId() > 0 && item.getQuantity() > 0) {
                        pstmtStock.setInt(1, (int) Math.round(item.getQuantity()));
                        pstmtStock.setInt(2, item.getProductId());
                        pstmtStock.executeUpdate();
                    }
                }
                pstmtItem.executeBatch();
            }
            
            conn.commit(); // Commit transaction
            return true;
            
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Bill> getBillsByDateRange(java.util.Date startDate, java.util.Date endDate) {
        List<Bill> bills = new java.util.ArrayList<>();
        String sql = "SELECT b.*, c.pending_amount AS customer_pending FROM bills b LEFT JOIN customers c ON b.customer_id = c.id WHERE b.bill_date >= ? AND b.bill_date <= ? AND b.status != 'Payment' ORDER BY b.bill_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String startStr = sdf.format(startDate) + " 00:00:00";
            String endStr = sdf.format(endDate) + " 23:59:59";
            
            pstmt.setString(1, startStr);
            pstmt.setString(2, endStr);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Bill bill = new Bill();
                    bill.setId(rs.getInt("id"));
                    bill.setCustomerName(rs.getString("customer_name"));
                    bill.setTotalAmount(rs.getDouble("total_amount"));
                    bill.setBillDate(rs.getTimestamp("bill_date"));
                    bill.setPaymentMode(rs.getString("payment_mode"));
                    bill.setPaidAmount(rs.getDouble("paid_amount"));
                    bill.setPendingAmount(rs.getDouble("pending_amount"));
                    bill.setCustomerTotalPending(rs.getDouble("customer_pending"));
                    try { bill.setDiscount(rs.getDouble("discount")); } catch (Exception ignore) {}
                    try {
                        bill.setStatus(rs.getString("status"));
                    } catch (SQLException ignore) {}
                    try {
                        bill.setDailyBillNo(rs.getInt("daily_bill_no"));
                    } catch (SQLException ignore) {}
                    if (bill.getDailyBillNo() <= 0) {
                        bill.setDailyBillNo(bill.getId());
                    }
                    try {
                        bill.setShowBalanceOnBill(rs.getInt("show_balance_on_bill") == 1);
                    } catch (SQLException ignore) {}
                    
                    bills.add(bill);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bills;
    }

    public List<String> getAllCustomerNames() {
        List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT customer_name FROM bills WHERE customer_name IS NOT NULL AND customer_name != '' " +
                "UNION " +
                "SELECT DISTINCT name AS customer_name FROM customers WHERE name IS NOT NULL AND name != '' ORDER BY 1 ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null && !name.trim().isEmpty() && !"Customer".equalsIgnoreCase(name.trim())) {
                    list.add(name.trim());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getNextBillId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM bills";
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

    public int getNextDailyBillNo() {
        return getNextDailyBillNo(new java.util.Date());
    }

    public int getNextDailyBillNo(java.util.Date date) {
        String sql = "SELECT COALESCE(MAX(daily_bill_no), 0) + 1 FROM bills WHERE substr(bill_date, 1, 10) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date != null ? date : new java.util.Date());
            pstmt.setString(1, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public int getTotalBillsCount() {
        String sql = "SELECT COUNT(*) FROM bills";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Bill getBillById(int billId) {
        String sqlBill = "SELECT b.*, c.id as cust_id, c.name as cust_name, c.mobile as cust_mobile, c.address as cust_address, c.pending_amount as cust_pending " +
                         "FROM bills b LEFT JOIN customers c ON b.customer_id = c.id WHERE b.id = ?";
        String sqlItems = "SELECT bi.*, p.name as prod_name, p.unit_type as prod_unit, p.display_name, p.tamil_name as prod_tamil " +
                          "FROM bill_items bi LEFT JOIN products p ON bi.product_id = p.id WHERE bi.bill_id = ? ORDER BY bi.id ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtBill = conn.prepareStatement(sqlBill)) {
            pstmtBill.setInt(1, billId);
            try (ResultSet rs = pstmtBill.executeQuery()) {
                if (rs.next()) {
                    Bill bill = new Bill();
                    bill.setId(rs.getInt("id"));
                    try { bill.setDailyBillNo(rs.getInt("daily_bill_no")); } catch (SQLException ignore) {}
                    if (bill.getDailyBillNo() <= 0) {
                        bill.setDailyBillNo(bill.getId());
                    }
                    bill.setCustomerName(rs.getString("customer_name"));
                    bill.setTotalAmount(rs.getDouble("total_amount"));
                    bill.setBillDate(rs.getTimestamp("bill_date"));
                    bill.setPaymentMode(rs.getString("payment_mode"));
                    
                    double paidAmt = 0;
                    double pendingAmt = 0;
                    double custPending = 0;
                    try { paidAmt = rs.getDouble("paid_amount"); } catch (SQLException ignore) {}
                    try { pendingAmt = rs.getDouble("pending_amount"); } catch (SQLException ignore) {}
                    try { custPending = rs.getDouble("cust_pending"); } catch (SQLException ignore) {}
                    bill.setPaidAmount(paidAmt);
                    bill.setPendingAmount(pendingAmt);
                    bill.setCustomerTotalPending(custPending);
                    try { bill.setShowBalanceOnBill(rs.getInt("show_balance_on_bill") == 1); } catch (SQLException ignore) {}
                    try { bill.setDiscount(rs.getDouble("discount")); } catch (SQLException ignore) {}

                    int custId = rs.getInt("customer_id");
                    if (!rs.wasNull() && custId > 0) {
                        com.store.billing.model.Customer cust = new com.store.billing.model.Customer(
                                custId,
                                rs.getString("cust_name"),
                                rs.getString("cust_mobile"),
                                rs.getString("cust_address"),
                                custPending
                        );
                        bill.setCustomerObj(cust);
                    }

                    // Load items
                    List<BillItem> items = new java.util.ArrayList<>();
                    try (PreparedStatement pstmtItems = conn.prepareStatement(sqlItems)) {
                        pstmtItems.setInt(1, billId);
                        try (ResultSet rsItems = pstmtItems.executeQuery()) {
                            while (rsItems.next()) {
                                BillItem item = new BillItem();
                                item.setId(rsItems.getInt("id"));
                                item.setBillId(billId);
                                item.setProductId(rsItems.getInt("product_id"));
                                
                                String displayName = rsItems.getString("display_name");
                                String prodName = rsItems.getString("prod_name");
                                String tamilName = rsItems.getString("tamil_name");
                                if (tamilName == null || tamilName.isEmpty()) {
                                    tamilName = rsItems.getString("prod_tamil");
                                }
                                
                                String pName = (displayName != null && !displayName.isEmpty()) ? displayName :
                                               (tamilName != null && !tamilName.isEmpty()) ? tamilName : prodName;
                                item.setProductName(pName);
                                item.setTamilName(tamilName);
                                
                                String unit = null;
                                try { unit = rsItems.getString("unit_type"); } catch (Exception ignore) {}
                                if (unit == null || unit.trim().isEmpty()) {
                                    unit = rsItems.getString("prod_unit");
                                }
                                if (unit == null || unit.trim().isEmpty()) unit = "piece";
                                item.setUnitType(unit.trim());
                                
                                item.setQuantity(rsItems.getDouble("quantity"));
                                item.setUnitPrice(rsItems.getDouble("unit_price"));
                                item.setSubtotal(rsItems.getDouble("subtotal"));
                                items.add(item);
                            }
                        }
                    }
                    bill.setItems(items);
                    return bill;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateBill(Bill bill) {
        String selectOldItemsSql = "SELECT product_id, quantity FROM bill_items WHERE bill_id = ?";
        String restoreStockSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
        String deleteOldItemsSql = "DELETE FROM bill_items WHERE bill_id = ?";
        String updateBillSql = "UPDATE bills SET customer_name = ?, total_amount = ?, payment_mode = ?, paid_amount = ?, pending_amount = ?, customer_id = ?, show_balance_on_bill = ?, discount = ?, daily_bill_no = CASE WHEN daily_bill_no IS NULL OR daily_bill_no <= 0 THEN ? ELSE daily_bill_no END WHERE id = ?";
        String insertItemSql = "INSERT INTO bill_items (bill_id, product_id, quantity, unit_price, subtotal, tamil_name, unit_type) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Restore previous stock for items in this bill
            try (PreparedStatement pstmtOld = conn.prepareStatement(selectOldItemsSql);
                 PreparedStatement pstmtRestore = conn.prepareStatement(restoreStockSql)) {
                pstmtOld.setInt(1, bill.getId());
                try (ResultSet rs = pstmtOld.executeQuery()) {
                    while (rs.next()) {
                        int prodId = rs.getInt("product_id");
                        double qty = rs.getDouble("quantity");
                        if (prodId > 0 && qty > 0) {
                            pstmtRestore.setInt(1, (int) Math.round(qty));
                            pstmtRestore.setInt(2, prodId);
                            pstmtRestore.executeUpdate();
                        }
                    }
                }
            }

            // 2. Delete old bill items
            try (PreparedStatement pstmtDel = conn.prepareStatement(deleteOldItemsSql)) {
                pstmtDel.setInt(1, bill.getId());
                pstmtDel.executeUpdate();
            }

            // 3. Update Bill master record
            try (PreparedStatement pstmtBill = conn.prepareStatement(updateBillSql)) {
                pstmtBill.setString(1, bill.getCustomerName());
                pstmtBill.setDouble(2, bill.getTotalAmount());
                pstmtBill.setString(3, bill.getPaymentMode() != null ? bill.getPaymentMode() : "Cash");
                pstmtBill.setDouble(4, bill.getPaidAmount());
                pstmtBill.setDouble(5, bill.getPendingAmount());
                if (bill.getCustomerObj() != null) {
                    pstmtBill.setInt(6, bill.getCustomerObj().getId());
                } else {
                    pstmtBill.setNull(6, java.sql.Types.INTEGER);
                }
                pstmtBill.setInt(7, bill.isShowBalanceOnBill() ? 1 : 0);
                pstmtBill.setDouble(8, bill.getDiscount());
                pstmtBill.setInt(9, bill.getDailyBillNo() > 0 ? bill.getDailyBillNo() : bill.getId());
                pstmtBill.setInt(10, bill.getId());
                pstmtBill.executeUpdate();
            }

            // 4. Insert new bill items & deduct new stock
            try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {
                for (BillItem item : bill.getItems()) {
                    pstmtItem.setInt(1, bill.getId());
                    pstmtItem.setInt(2, item.getProductId());
                    pstmtItem.setDouble(3, item.getQuantity());
                    pstmtItem.setDouble(4, item.getUnitPrice());
                    pstmtItem.setDouble(5, item.getSubtotal());
                    pstmtItem.setString(6, item.getTamilName());
                    pstmtItem.setString(7, item.getUnitType() != null ? item.getUnitType() : "piece");
                    pstmtItem.addBatch();

                    if (item.getProductId() > 0 && item.getQuantity() > 0) {
                        pstmtStock.setInt(1, (int) Math.round(item.getQuantity()));
                        pstmtStock.setInt(2, item.getProductId());
                        pstmtStock.executeUpdate();
                    }
                }
                pstmtItem.executeBatch();
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

    public boolean deleteBill(int billId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Refund stock
            String selectOldItemsSql = "SELECT product_id, quantity FROM bill_items WHERE bill_id = ?";
            String refundStockSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            try (PreparedStatement pstmtSel = conn.prepareStatement(selectOldItemsSql);
                 PreparedStatement pstmtRef = conn.prepareStatement(refundStockSql)) {
                pstmtSel.setInt(1, billId);
                try (ResultSet rs = pstmtSel.executeQuery()) {
                    while (rs.next()) {
                        int pId = rs.getInt("product_id");
                        int qty = rs.getInt("quantity");
                        if (pId > 0 && qty > 0) {
                            pstmtRef.setInt(1, qty);
                            pstmtRef.setInt(2, pId);
                            pstmtRef.executeUpdate();
                        }
                    }
                }
            }

            // 2. Delete bill items and bill
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bill_items WHERE bill_id = ?")) {
                pstmt.setInt(1, billId);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bills WHERE id = ?")) {
                pstmt.setInt(1, billId);
                pstmt.executeUpdate();
            }

            // 3. Reset auto-increment sequence to reclaim the ID if the last bill(s) were deleted
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE sqlite_sequence SET seq = (SELECT COALESCE(MAX(id), 0) FROM bills) WHERE name = 'bills'");
            } catch (Exception ignored) {}

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

    public boolean deleteBillsByDateRange(java.util.Date fromDate, java.util.Date toDate) {
        String sqlSelect = "SELECT id FROM bills WHERE bill_date >= ? AND bill_date <= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            pstmt.setString(1, sdf.format(fromDate) + " 00:00:00");
            pstmt.setString(2, sdf.format(toDate) + " 23:59:59");
            
            java.util.List<Integer> billIds = new java.util.ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    billIds.add(rs.getInt("id"));
                }
            }
            
            for (int id : billIds) {
                deleteBill(id); // properly refunds stock and deletes items
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean clearAllBillingHistory() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM bill_items");
            stmt.execute("DELETE FROM bills");
            try {
                stmt.execute("DELETE FROM sqlite_sequence WHERE name='bills' OR name='bill_items'");
            } catch (Exception ignored) {}
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
