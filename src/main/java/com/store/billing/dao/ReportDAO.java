package com.store.billing.dao;

import com.store.billing.model.ReportItem;
import com.store.billing.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportDAO {
    
    public List<ReportItem> getSalesByCategory(Date fromDate, Date toDate) {
        List<ReportItem> report = new ArrayList<>();
        String query = 
            "SELECT " +
            "    COALESCE(p.category, 'Uncategorized') as category, " +
            "    COALESCE(p.name, bi.tamil_name) as name, " +
            "    SUM(bi.quantity) as total_qty, " +
            "    SUM(bi.subtotal) as total_sales, " +
            "    SUM(bi.subtotal - (bi.quantity * COALESCE(p.purchase_rate, 0))) as profit " +
            "FROM bills b " +
            "JOIN bill_items bi ON b.id = bi.bill_id " +
            "LEFT JOIN products p ON bi.product_id = p.id " +
            "WHERE b.bill_date >= ? AND b.bill_date <= ? AND b.status != 'Payment' " +
            "GROUP BY category, name " +
            "ORDER BY category ASC, name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            // SQLite handles date comparisons better as YYYY-MM-DD HH:MM:SS strings
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String startStr = sdf.format(fromDate) + " 00:00:00";
            String endStr = sdf.format(toDate) + " 23:59:59";

            pstmt.setString(1, startStr);
            pstmt.setString(2, endStr);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    if (category == null || category.trim().isEmpty()) {
                        category = "Uncategorized";
                    }
                    report.add(new ReportItem(
                        category,
                        rs.getString("name"),
                        rs.getInt("total_qty"),
                        rs.getDouble("total_sales"),
                        rs.getDouble("profit")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return report;
    }
}
