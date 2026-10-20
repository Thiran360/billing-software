package com.store.billing.dao;

import com.store.billing.model.PartyPrice;
import com.store.billing.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PartyPriceDAO {

    public void saveOrUpdatePartyPrice(int customerId, int productId, double customPrice) {
        String sql = "INSERT INTO party_prices (customer_id, product_id, custom_price) VALUES (?, ?, ?) " +
                     "ON CONFLICT(customer_id, product_id) DO UPDATE SET custom_price = excluded.custom_price";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, productId);
            pstmt.setDouble(3, customPrice);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Double getPartyPrice(int customerId, int productId) {
        String sql = "SELECT custom_price FROM party_prices WHERE customer_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("custom_price");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<PartyPrice> getPartyPricesByCustomer(int customerId) {
        List<PartyPrice> prices = new ArrayList<>();
        String sql = "SELECT pp.id, pp.customer_id, pp.product_id, pp.custom_price, p.name as product_name " +
                     "FROM party_prices pp " +
                     "JOIN products p ON pp.product_id = p.id " +
                     "WHERE pp.customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PartyPrice pp = new PartyPrice(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getInt("product_id"),
                        rs.getDouble("custom_price")
                );
                pp.setProductName(rs.getString("product_name"));
                prices.add(pp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prices;
    }
    
    public void deletePartyPrice(int id) {
        String sql = "DELETE FROM party_prices WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
