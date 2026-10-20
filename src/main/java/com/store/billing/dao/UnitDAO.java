package com.store.billing.dao;

import com.store.billing.model.Unit;
import com.store.billing.util.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UnitDAO {

    private static final Map<String, Unit> CACHE_BY_EN = new ConcurrentHashMap<>();
    private static final Map<String, Unit> CACHE_BY_TA = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;

    public static synchronized void ensureInitialized() {
        if (!initialized) {
            reloadCache();
            initialized = true;
        }
    }

    public static synchronized void reloadCache() {
        CACHE_BY_EN.clear();
        CACHE_BY_TA.clear();
        List<Unit> list = fetchAllFromDB();
        for (Unit u : list) {
            if (u.getNameEn() != null && !u.getNameEn().isEmpty()) {
                CACHE_BY_EN.put(u.getNameEn().toLowerCase().trim(), u);
            }
            if (u.getNameTa() != null && !u.getNameTa().isEmpty()) {
                CACHE_BY_TA.put(u.getNameTa().trim(), u);
            }
        }
    }

    private static List<Unit> fetchAllFromDB() {
        List<Unit> list = new ArrayList<>();
        String sql = "SELECT id, name_en, name_ta FROM units ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Unit(
                    rs.getInt("id"),
                    rs.getString("name_en"),
                    rs.getString("name_ta")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching units: " + e.getMessage());
        }
        return list;
    }

    public List<Unit> getAllUnits() {
        ensureInitialized();
        return fetchAllFromDB();
    }

    public boolean addUnit(String nameEn, String nameTa) {
        if (nameEn == null || nameEn.trim().isEmpty()) return false;
        String en = nameEn.trim();
        String ta = (nameTa != null && !nameTa.trim().isEmpty()) ? nameTa.trim() : en;

        String sql = "INSERT INTO units (name_en, name_ta) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, en);
            pstmt.setString(2, ta);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                reloadCache();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding unit: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUnit(int id, String nameEn, String nameTa) {
        if (id <= 0 || nameEn == null || nameEn.trim().isEmpty()) return false;
        String en = nameEn.trim();
        String ta = (nameTa != null && !nameTa.trim().isEmpty()) ? nameTa.trim() : en;

        String sql = "UPDATE units SET name_en = ?, name_ta = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, en);
            pstmt.setString(2, ta);
            pstmt.setInt(3, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                reloadCache();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating unit: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteUnit(int id) {
        if (id <= 0) return false;
        String sql = "DELETE FROM units WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                reloadCache();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting unit: " + e.getMessage());
        }
        return false;
    }

    public static Unit findUnit(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        ensureInitialized();
        String trimmed = name.trim();
        String lower = trimmed.toLowerCase();

        if (CACHE_BY_EN.containsKey(lower)) {
            return CACHE_BY_EN.get(lower);
        }
        if (CACHE_BY_TA.containsKey(trimmed)) {
            return CACHE_BY_TA.get(trimmed);
        }
        return null;
    }

    public static String getTamilName(String name) {
        Unit u = findUnit(name);
        if (u != null && u.getNameTa() != null && !u.getNameTa().isEmpty()) {
            return u.getNameTa();
        }
        return name;
    }

    public static String getEnglishName(String name) {
        Unit u = findUnit(name);
        if (u != null && u.getNameEn() != null && !u.getNameEn().isEmpty()) {
            return u.getNameEn();
        }
        return name;
    }
}
