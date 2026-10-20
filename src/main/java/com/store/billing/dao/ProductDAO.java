package com.store.billing.dao;

import com.store.billing.model.Product;
import com.store.billing.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY id ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE category IS NOT NULL AND TRIM(category) != '' ORDER BY category ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString(1).trim());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public List<Product> getProductsByCategory(String category) {
        List<Product> products = new ArrayList<>();
        String sql;
        boolean hasCategory = category != null && !category.trim().isEmpty() && !"All Categories".equalsIgnoreCase(category.trim());
        if (hasCategory) {
            sql = "SELECT * FROM products WHERE category = ? ORDER BY id ASC";
        } else {
            sql = "SELECT * FROM products ORDER BY id ASC";
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasCategory) {
                pstmt.setString(1, category.trim());
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(extractProductFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
    
    private Product extractProductFromResultSet(ResultSet rs) throws SQLException {
        int wholeQty = 0;
        int retailQty = 0;
        String category = "";
        String displayName = "";
        String englishName = "";

        try { wholeQty = rs.getInt("whole_qty_threshold"); } catch (SQLException ignore) {}
        try { retailQty = rs.getInt("retail_qty_threshold"); } catch (SQLException ignore) {}
        try { category = rs.getString("category"); } catch (SQLException ignore) {}
        try { displayName = rs.getString("display_name"); } catch (SQLException ignore) {}
        try { englishName = rs.getString("english_name"); } catch (SQLException ignore) {}

        if (category == null) category = "";
        if (displayName == null) displayName = "";
        if (englishName == null) englishName = "";
        
        return new Product(
                rs.getInt("id"),
                rs.getString("barcode"),
                rs.getString("name"),
                rs.getInt("stock_quantity"),
                rs.getString("unit_type"),
                rs.getString("tamil_name"),
                rs.getDouble("purchase_rate"),
                rs.getDouble("line_rate"),
                rs.getDouble("whole_price"),
                rs.getDouble("whole_dis"),
                rs.getDouble("retail_price"),
                rs.getDouble("retail_dis"),
                wholeQty,
                retailQty,
                category,
                displayName,
                englishName
        );
    }

    public Product getProductByBarcode(String barcode) {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, barcode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return extractProductFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products(barcode, name, stock_quantity, unit_type, tamil_name, " +
                "purchase_rate, line_rate, whole_price, whole_dis, retail_price, retail_dis, " +
                "whole_qty_threshold, retail_qty_threshold, category, display_name, english_name) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setInt(3, product.getStockQuantity());
            pstmt.setString(4, product.getUnitType());
            pstmt.setString(5, product.getTamilName());
            pstmt.setDouble(6, product.getPurchaseRate());
            pstmt.setDouble(7, product.getLineRate());
            pstmt.setDouble(8, product.getWholePrice());
            pstmt.setDouble(9, product.getWholeDis());
            pstmt.setDouble(10, product.getRetailPrice());
            pstmt.setDouble(11, product.getRetailDis());
            pstmt.setInt(12, product.getWholeQtyThreshold());
            pstmt.setInt(13, product.getRetailQtyThreshold());
            pstmt.setString(14, product.getCategory());
            pstmt.setString(15, product.getDisplayName());
            pstmt.setString(16, product.getEnglishName());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET barcode = ?, name = ?, stock_quantity = ?, unit_type = ?, tamil_name = ?, " +
                "purchase_rate = ?, line_rate = ?, whole_price = ?, whole_dis = ?, retail_price = ?, retail_dis = ?, " +
                "whole_qty_threshold = ?, retail_qty_threshold = ?, category = ?, display_name = ?, english_name = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setString(1, product.getBarcode());
            pstmt.setString(2, product.getName());
            pstmt.setInt(3, product.getStockQuantity());
            pstmt.setString(4, product.getUnitType());
            pstmt.setString(5, product.getTamilName());
            pstmt.setDouble(6, product.getPurchaseRate());
            pstmt.setDouble(7, product.getLineRate());
            pstmt.setDouble(8, product.getWholePrice());
            pstmt.setDouble(9, product.getWholeDis());
            pstmt.setDouble(10, product.getRetailPrice());
            pstmt.setDouble(11, product.getRetailDis());
            pstmt.setInt(12, product.getWholeQtyThreshold());
            pstmt.setInt(13, product.getRetailQtyThreshold());
            pstmt.setString(14, product.getCategory());
            pstmt.setString(15, product.getDisplayName());
            pstmt.setString(16, product.getEnglishName());
            pstmt.setInt(17, product.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateStock(int productId, int newQuantity) {
        String sql = "UPDATE products SET stock_quantity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, newQuantity);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean decreaseStock(int productId, int quantityToDecrease) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, quantityToDecrease);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
