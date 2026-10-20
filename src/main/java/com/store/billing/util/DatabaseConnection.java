package com.store.billing.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static String URL;

    static {
        try {
            // Find the directory where this application/jar is actually located
            String path = new java.io.File(DatabaseConnection.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            URL = "jdbc:sqlite:" + path + java.io.File.separator + "store_billing.db";
        } catch (Exception e) {
            URL = "jdbc:sqlite:store_billing.db"; // Fallback to current working directory
            e.printStackTrace();
        }
        initializeDatabase();
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // Create Settings table
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_settings (" +
                    "setting_key VARCHAR(50) PRIMARY KEY, " +
                    "setting_value TEXT NOT NULL)");
            
            // Create Products table
            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "barcode VARCHAR(50), " +
                    "name VARCHAR(100) NOT NULL, " +
                    "stock_quantity INT NOT NULL DEFAULT 0, " +
                    "unit_type VARCHAR(20), " +
                    "tamil_name VARCHAR(100), " +
                    "purchase_rate DECIMAL(10,2) DEFAULT 0, " +
                    "line_rate DECIMAL(10,2) DEFAULT 0, " +
                    "whole_price DECIMAL(10,2) DEFAULT 0, " +
                    "whole_dis DECIMAL(10,2) DEFAULT 0, " +
                    "retail_price DECIMAL(10,2) DEFAULT 0, " +
                    "retail_dis DECIMAL(10,2) DEFAULT 0, " +
                    "whole_qty_threshold INT DEFAULT 0, " +
                    "retail_qty_threshold INT DEFAULT 0, " +
                    "category VARCHAR(100) DEFAULT '', " +
                    "display_name VARCHAR(100) DEFAULT '', " +
                    "english_name VARCHAR(100) DEFAULT '')");
                    
            // Add columns if they don't exist in older databases
            try { stmt.execute("ALTER TABLE products ADD COLUMN purchase_rate DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN line_rate DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN whole_price DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN whole_dis DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN retail_price DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN retail_dis DECIMAL(10,2) DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN whole_qty_threshold INT DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN retail_qty_threshold INT DEFAULT 0"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN category VARCHAR(100) DEFAULT ''"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN display_name VARCHAR(100) DEFAULT ''"); } catch (Exception ignore) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN english_name VARCHAR(100) DEFAULT ''"); } catch (Exception ignore) {}
                    
            // Create Customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "mobile VARCHAR(20) UNIQUE, " +
                    "address TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create Bills table
            stmt.execute("CREATE TABLE IF NOT EXISTS bills (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_name VARCHAR(100), " +
                    "customer_id INT NULL, " +
                    "total_amount DECIMAL(10,2) NOT NULL, " +
                    "payment_mode VARCHAR(50), " +
                    "bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id))");

            // Create Bill Items table
            stmt.execute("CREATE TABLE IF NOT EXISTS bill_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "bill_id INT NOT NULL, " +
                    "product_id INT NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "unit_price DECIMAL(10,2) NOT NULL, " +
                    "subtotal DECIMAL(10,2) NOT NULL, " +
                    "tamil_name VARCHAR(100), " +
                    "FOREIGN KEY (bill_id) REFERENCES bills(id), " +
                    "FOREIGN KEY (product_id) REFERENCES products(id))");
            
            // Try to add tamil_name to existing bill_items table if it was created without it
            try {
                stmt.execute("ALTER TABLE bill_items ADD COLUMN tamil_name VARCHAR(100)");
            } catch (SQLException ignore) {
                // Column already exists
            }
            
            // Try to add unit_type to bill_items table
            try {
                stmt.execute("ALTER TABLE bill_items ADD COLUMN unit_type VARCHAR(50)");
            } catch (SQLException ignore) {}

            // Create Units table (supporting English and Tamil)
            stmt.execute("CREATE TABLE IF NOT EXISTS units (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name_en VARCHAR(50) NOT NULL UNIQUE COLLATE NOCASE, " +
                    "name_ta VARCHAR(50) NOT NULL)");

            // Seed default units if empty
            try (ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM units")) {
                if (rsCount.next() && rsCount.getInt(1) == 0) {
                    String[][] defaults = {
                        {"piece", "பீஸ்"},
                        {"dozen", "டஜன்"},
                        {"pandal", "பண்டல்"},
                        {"packet", "பாக்கெட்"},
                        {"box", "பாக்ஸ்"},
                        {"bottle", "பாட்டில்"},
                        {"saram", "சரம்"},
                        {"kattu", "கட்டு"},
                        {"attai", "அட்டை"},
                        {"pai", "பை"},
                        {"kg", "கிலோ"},
                        {"g", "கிராம்"},
                        {"litre", "லிட்டர்"},
                        {"ml", "மி.லி"},
                        {"jar", "ஜார்"},
                        {"case", "கேஸ்"},
                        {"pet", "பெட்"},
                        {"strip", "ஸ்ட்ரிப்"},
                        {"set", "செட்"},
                        {"roll", "ரோல்"},
                        {"meter", "மீட்டர்"},
                        {"tin", "டின்"}
                    };
                    try (PreparedStatement insPstmt = conn.prepareStatement("INSERT OR IGNORE INTO units (name_en, name_ta) VALUES (?, ?)")) {
                        for (String[] def : defaults) {
                            insPstmt.setString(1, def[0]);
                            insPstmt.setString(2, def[1]);
                            insPstmt.executeUpdate();
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Units seed error: " + ex.getMessage());
            }

            // Sync any existing units from products table into units table
            try {
                stmt.execute("INSERT OR IGNORE INTO units (name_en, name_ta) " +
                             "SELECT DISTINCT LOWER(TRIM(unit_type)), LOWER(TRIM(unit_type)) FROM products " +
                             "WHERE unit_type IS NOT NULL AND TRIM(unit_type) != ''");
            } catch (Exception ignore) {}
            
            // Try to add pending_amount to customers table
            try {
                stmt.execute("ALTER TABLE customers ADD COLUMN pending_amount DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException ignore) {
                // Column already exists
            }
            
            // Clean up empty string mobile numbers to NULL to avoid UNIQUE constraint collisions on empty strings
            try {
                stmt.execute("UPDATE customers SET mobile = NULL WHERE mobile = '' OR TRIM(mobile) = ''");
            } catch (Exception ignore) {}
            
            // Try to add paid_amount and pending_amount to bills table
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN paid_amount DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN pending_amount DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN status VARCHAR(20) DEFAULT 'Printed'");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN daily_bill_no INT DEFAULT NULL");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE customers ADD COLUMN tamil_name VARCHAR(100) DEFAULT ''");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN show_balance_on_bill INT DEFAULT 1");
            } catch (SQLException ignore) {}
            try {
                stmt.execute("ALTER TABLE bills ADD COLUMN discount DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException ignore) {}
            // One-time migration: Convert legacy UTC bill timestamps to local system time (IST / +05:30)
            try {
                boolean alreadyMigrated = false;
                try (PreparedStatement checkStmt = conn.prepareStatement("SELECT setting_value FROM shop_settings WHERE setting_key = 'utc_to_ist_migration_done'")) {
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && "true".equalsIgnoreCase(rs.getString("setting_value"))) {
                            alreadyMigrated = true;
                        }
                    }
                } catch (Exception ignore) {}

                if (!alreadyMigrated) {
                    stmt.execute("UPDATE bills SET bill_date = datetime(bill_date, 'localtime') WHERE bill_date IS NOT NULL AND bill_date LIKE '____-__-__ %'");
                    stmt.execute("UPDATE bills SET daily_bill_no = (" +
                                 "  SELECT COUNT(*) FROM bills b2 " +
                                 "  WHERE substr(b2.bill_date, 1, 10) = substr(bills.bill_date, 1, 10) AND b2.id <= bills.id" +
                                 ")");
                    try (PreparedStatement insStmt = conn.prepareStatement("INSERT OR REPLACE INTO shop_settings (setting_key, setting_value) VALUES ('utc_to_ist_migration_done', 'true')")) {
                        insStmt.executeUpdate();
                    }
                    System.out.println("UTC to local time migration completed successfully.");
                }
            } catch (Exception ex) {
                System.err.println("Migration UTC to local: " + ex.getMessage());
            }

            try {
                stmt.execute("UPDATE bills SET daily_bill_no = (" +
                             "  SELECT COUNT(*) FROM bills b2 " +
                             "  WHERE substr(b2.bill_date, 1, 10) = substr(bills.bill_date, 1, 10) AND b2.id <= bills.id" +
                             ") WHERE daily_bill_no IS NULL OR daily_bill_no = 0");
            } catch (Exception ignore) {}

            try {
                stmt.execute("UPDATE customers SET pending_amount = (" +
                             "  SELECT COALESCE(SUM(pending_amount), 0) FROM bills WHERE customer_id = customers.id" +
                             ") WHERE id IN (SELECT DISTINCT customer_id FROM bills WHERE customer_id > 0)");
            } catch (Exception ignore) {}

            // Create Purchases table (Supplier / Vendor Purchases & Expenses)
            stmt.execute("CREATE TABLE IF NOT EXISTS purchases (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "supplier_name VARCHAR(100) NOT NULL, " +
                    "supplier_mobile VARCHAR(20), " +
                    "supplier_address TEXT, " +
                    "invoice_number VARCHAR(50), " +
                    "purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "total_amount DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "pending_amount DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "payment_mode VARCHAR(50) DEFAULT 'Cash', " +
                    "description TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create Supplier Payments table (Installment payments towards supplier balances)
            stmt.execute("CREATE TABLE IF NOT EXISTS supplier_payments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "purchase_id INT, " +
                    "supplier_name VARCHAR(100) NOT NULL, " +
                    "amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "payment_mode VARCHAR(50) DEFAULT 'Cash', " +
                    "payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "notes TEXT, " +
                    "FOREIGN KEY (purchase_id) REFERENCES purchases(id))");

            // Create Party Prices table
            stmt.execute("CREATE TABLE IF NOT EXISTS party_prices (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_id INT NOT NULL, " +
                    "product_id INT NOT NULL, " +
                    "custom_price DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(customer_id, product_id), " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id), " +
                    "FOREIGN KEY (product_id) REFERENCES products(id))");

            // Reconcile and fix legacy Credit (Unpaid) bills and unpaid customer bills that had pending_amount = 0
            try {
                // 1. Link customer_id to existing customers if null but customer_name matches
                stmt.execute("UPDATE bills SET customer_id = (SELECT c.id FROM customers c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(bills.customer_name)) LIMIT 1) " +
                        "WHERE customer_id IS NULL AND customer_name IS NOT NULL AND TRIM(customer_name) != '' AND TRIM(LOWER(customer_name)) != 'customer'");

                // 2. Clean up any invalid negative pending_amount in bills and customers
                stmt.execute("UPDATE bills SET pending_amount = 0 WHERE pending_amount < 0 OR pending_amount IS NULL");
                stmt.execute("UPDATE customers SET pending_amount = 0 WHERE pending_amount < 0 OR pending_amount IS NULL");

                // 3. If a bill has unpaid amount (total_amount > paid_amount) and is Credit or has a registered customer,
                // ensure pending_amount is set to total_amount - paid_amount
                String fixCreditBillsSql = "UPDATE bills SET pending_amount = MAX(0.0, total_amount - COALESCE(paid_amount, 0)) " +
                        "WHERE (pending_amount = 0 OR pending_amount IS NULL) " +
                        "AND total_amount > COALESCE(paid_amount, 0) " +
                        "AND (payment_mode LIKE '%Credit%' OR customer_id IS NOT NULL OR (customer_name IS NOT NULL AND TRIM(customer_name) != '' AND TRIM(LOWER(customer_name)) != 'customer'))";
                stmt.execute(fixCreditBillsSql);

                // 4. Recalculate and synchronize customer pending_amount from their bills
                String syncCustSql = "UPDATE customers SET pending_amount = MAX(0.0, COALESCE(" +
                        "(SELECT SUM(CASE WHEN b.pending_amount > 0 THEN b.pending_amount ELSE 0 END) FROM bills b WHERE b.customer_id = customers.id), 0)) " +
                        "WHERE EXISTS (SELECT 1 FROM bills b WHERE b.customer_id = customers.id)";
                stmt.execute(syncCustSql);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database tables.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found. Add it to your library path.");
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL);
    }
}
