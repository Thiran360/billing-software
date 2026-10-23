package com.store.billing.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;
import com.store.billing.model.Customer;
import com.store.billing.model.Product;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mustafa_billing.db";
    private static final int DATABASE_VERSION = 1;

    private static AppDatabaseHelper instance;

    public static synchronized AppDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Shop Settings
        db.execSQL("CREATE TABLE IF NOT EXISTS shop_settings (" +
                "setting_key TEXT PRIMARY KEY, " +
                "setting_value TEXT NOT NULL)");

        // Products
        db.execSQL("CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "barcode TEXT, " +
                "name TEXT NOT NULL, " +
                "tamil_name TEXT, " +
                "stock_quantity INTEGER NOT NULL DEFAULT 0, " +
                "unit_type TEXT DEFAULT 'piece', " +
                "purchase_rate REAL DEFAULT 0, " +
                "whole_price REAL DEFAULT 0, " +
                "retail_price REAL DEFAULT 0)");

        // Customers
        db.execSQL("CREATE TABLE IF NOT EXISTS customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "mobile TEXT UNIQUE, " +
                "address TEXT, " +
                "pending_amount REAL DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // Bills
        db.execSQL("CREATE TABLE IF NOT EXISTS bills (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "customer_name TEXT, " +
                "customer_id INTEGER, " +
                "total_amount REAL NOT NULL, " +
                "paid_amount REAL DEFAULT 0, " +
                "pending_amount REAL DEFAULT 0, " +
                "payment_mode TEXT, " +
                "bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // Bill Items
        db.execSQL("CREATE TABLE IF NOT EXISTS bill_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bill_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "unit_price REAL NOT NULL, " +
                "subtotal REAL NOT NULL, " +
                "product_name TEXT, " +
                "tamil_name TEXT, " +
                "unit_type TEXT)");

        // Units
        db.execSQL("CREATE TABLE IF NOT EXISTS units (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name_en TEXT NOT NULL UNIQUE, " +
                "name_ta TEXT NOT NULL)");

        seedDefaults(db);
    }

    private void seedDefaults(SQLiteDatabase db) {
        // Default Shop Info
        db.execSQL("INSERT OR IGNORE INTO shop_settings VALUES ('shop_name', 'MUSTAFA TRADERS & STORE')");
        db.execSQL("INSERT OR IGNORE INTO shop_settings VALUES ('shop_address', 'Main Bazaar, Tamil Nadu')");
        db.execSQL("INSERT OR IGNORE INTO shop_settings VALUES ('shop_phone', '+91 9876543210')");
        db.execSQL("INSERT OR IGNORE INTO shop_settings VALUES ('shop_gst', '33AAAAA0000A1Z5')");
        db.execSQL("INSERT OR IGNORE INTO shop_settings VALUES ('shop_upi', 'mustafastore@upi')");

        // Seed initial units
        String[][] units = {
                {"piece", "பீஸ்"},
                {"packet", "பாக்கெட்"},
                {"box", "பாக்ஸ்"},
                {"kg", "கிலோ"},
                {"litre", "லிட்டர்"}
        };
        for (String[] u : units) {
            db.execSQL("INSERT OR IGNORE INTO units (name_en, name_ta) VALUES ('" + u[0] + "', '" + u[1] + "')");
        }

        // Seed initial products
        String[][] sampleProducts = {
                {"8901030000001", "Tea Powder", "டீ தூள்", "100", "packet", "30", "45", "50"},
                {"8901030000002", "Sugar", "சர்க்கரை", "250", "kg", "38", "42", "44"},
                {"8901030000003", "Cooking Oil", "சமையல் எண்ணெய்", "80", "litre", "120", "135", "145"},
                {"8901030000004", "Basmati Rice", "பாசுமதி அரிசி", "150", "kg", "75", "90", "95"},
                {"8901030000005", "Wheat Flour", "கோதுமை மாவு", "120", "packet", "40", "48", "52"},
                {"8901030000006", "Washing Soap", "சலவை சோப்", "200", "piece", "15", "18", "20"}
        };
        for (String[] p : sampleProducts) {
            db.execSQL("INSERT OR IGNORE INTO products (barcode, name, tamil_name, stock_quantity, unit_type, purchase_rate, whole_price, retail_price) " +
                    "VALUES ('" + p[0] + "', '" + p[1] + "', '" + p[2] + "', " + p[3] + ", '" + p[4] + "', " + p[5] + ", " + p[6] + ", " + p[7] + ")");
        }

        // Seed initial customer
        db.execSQL("INSERT OR IGNORE INTO customers (name, mobile, address, pending_amount) " +
                "VALUES ('Cash Customer (Walk-in)', '', 'Counter Sale', 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade logic if needed in future
    }

    // --- PRODUCTS ---
    public List<Product> getAllProducts() {
        return searchProducts(null);
    }

    public List<Product> searchProducts(String query) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;
        if (query == null || query.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM products ORDER BY name ASC", null);
        } else {
            String q = "%" + query.trim() + "%";
            cursor = db.rawQuery("SELECT * FROM products WHERE name LIKE ? OR tamil_name LIKE ? OR barcode LIKE ? ORDER BY name ASC",
                    new String[]{q, q, q});
        }
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Product p = new Product();
                p.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                p.setBarcode(cursor.getString(cursor.getColumnIndexOrThrow("barcode")));
                p.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                p.setTamilName(cursor.getString(cursor.getColumnIndexOrThrow("tamil_name")));
                p.setStockQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("stock_quantity")));
                p.setUnitType(cursor.getString(cursor.getColumnIndexOrThrow("unit_type")));
                p.setRetailPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("retail_price")));
                p.setWholePrice(cursor.getDouble(cursor.getColumnIndexOrThrow("whole_price")));
                list.add(p);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public long saveProduct(Product p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("barcode", p.getBarcode());
        cv.put("name", p.getName());
        cv.put("tamil_name", p.getTamilName());
        cv.put("stock_quantity", p.getStockQuantity());
        cv.put("unit_type", p.getUnitType());
        cv.put("retail_price", p.getRetailPrice());
        cv.put("whole_price", p.getWholePrice());

        if (p.getId() > 0) {
            return db.update("products", cv, "id = ?", new String[]{String.valueOf(p.getId())});
        } else {
            return db.insert("products", null, cv);
        }
    }

    public int deleteProduct(int id) {
        return getWritableDatabase().delete("products", "id = ?", new String[]{String.valueOf(id)});
    }

    // --- CUSTOMERS ---
    public List<Customer> getAllCustomers() {
        return searchCustomers(null);
    }

    public List<Customer> searchCustomers(String query) {
        List<Customer> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;
        if (query == null || query.trim().isEmpty()) {
            cursor = db.rawQuery("SELECT * FROM customers ORDER BY name ASC", null);
        } else {
            String q = "%" + query.trim() + "%";
            cursor = db.rawQuery("SELECT * FROM customers WHERE name LIKE ? OR mobile LIKE ? ORDER BY name ASC",
                    new String[]{q, q});
        }
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Customer c = new Customer();
                c.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                c.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                c.setMobile(cursor.getString(cursor.getColumnIndexOrThrow("mobile")));
                c.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
                c.setPendingAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("pending_amount")));
                list.add(c);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public long saveCustomer(Customer c) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", c.getName());
        cv.put("mobile", c.getMobile());
        cv.put("address", c.getAddress());
        cv.put("pending_amount", c.getPendingAmount());

        if (c.getId() > 0) {
            return db.update("customers", cv, "id = ?", new String[]{String.valueOf(c.getId())});
        } else {
            return db.insert("customers", null, cv);
        }
    }

    // --- BILLS ---
    public long saveBill(Bill bill) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long billId = -1;
        try {
            ContentValues cv = new ContentValues();
            cv.put("customer_name", bill.getCustomerName());
            cv.put("customer_id", bill.getCustomerId() > 0 ? bill.getCustomerId() : null);
            cv.put("total_amount", bill.getTotalAmount());
            cv.put("paid_amount", bill.getPaidAmount());
            cv.put("pending_amount", bill.getPendingAmount());
            cv.put("payment_mode", bill.getPaymentMode());
            
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            cv.put("bill_date", currentDate);
            bill.setBillDate(currentDate);

            billId = db.insert("bills", null, cv);

            // Insert items & decrease stock
            for (BillItem item : bill.getItems()) {
                ContentValues icv = new ContentValues();
                icv.put("bill_id", billId);
                icv.put("product_id", item.getProductId());
                icv.put("quantity", item.getQuantity());
                icv.put("unit_price", item.getUnitPrice());
                icv.put("subtotal", item.getSubtotal());
                icv.put("product_name", item.getProductName());
                icv.put("tamil_name", item.getTamilName());
                icv.put("unit_type", item.getUnitType());
                db.insert("bill_items", null, icv);

                // Reduce inventory stock
                db.execSQL("UPDATE products SET stock_quantity = MAX(0, stock_quantity - " + item.getQuantity() + ") WHERE id = " + item.getProductId());
            }

            // Update customer pending credit balance if any
            if (bill.getCustomerId() > 0 && bill.getPendingAmount() > 0) {
                db.execSQL("UPDATE customers SET pending_amount = pending_amount + " + bill.getPendingAmount() + " WHERE id = " + bill.getCustomerId());
            }

            db.setTransactionSuccessful();
            bill.setId((int) billId);
        } finally {
            db.endTransaction();
        }
        return billId;
    }

    public List<Bill> getAllBills() {
        List<Bill> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM bills ORDER BY id DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Bill b = new Bill();
                b.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                b.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow("customer_name")));
                b.setTotalAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount")));
                b.setPaidAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("paid_amount")));
                b.setPendingAmount(cursor.getDouble(cursor.getColumnIndexOrThrow("pending_amount")));
                b.setPaymentMode(cursor.getString(cursor.getColumnIndexOrThrow("payment_mode")));
                b.setBillDate(cursor.getString(cursor.getColumnIndexOrThrow("bill_date")));
                list.add(b);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<BillItem> getBillItems(int billId) {
        List<BillItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM bill_items WHERE bill_id = ?", new String[]{String.valueOf(billId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                BillItem item = new BillItem();
                item.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                item.setBillId(billId);
                item.setProductId(cursor.getInt(cursor.getColumnIndexOrThrow("product_id")));
                item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow("quantity")));
                item.setUnitPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("unit_price")));
                item.setSubtotal(cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal")));
                item.setProductName(cursor.getString(cursor.getColumnIndexOrThrow("product_name")));
                item.setTamilName(cursor.getString(cursor.getColumnIndexOrThrow("tamil_name")));
                item.setUnitType(cursor.getString(cursor.getColumnIndexOrThrow("unit_type")));
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    // --- DASHBOARD METRICS ---
    public double getTodaySales() {
        SQLiteDatabase db = getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + "%";
        Cursor c = db.rawQuery("SELECT SUM(total_amount) FROM bills WHERE bill_date LIKE ?", new String[]{today});
        double sum = 0;
        if (c != null && c.moveToFirst()) {
            sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    public int getTodayBillsCount() {
        SQLiteDatabase db = getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + "%";
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM bills WHERE bill_date LIKE ?", new String[]{today});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public double getTotalPendingCredit() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(pending_amount) FROM customers", null);
        double sum = 0;
        if (c != null && c.moveToFirst()) {
            sum = c.getDouble(0);
            c.close();
        }
        return sum;
    }

    // --- SETTINGS ---
    public String getSetting(String key, String fallback) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT setting_value FROM shop_settings WHERE setting_key = ?", new String[]{key});
        String val = fallback;
        if (c != null && c.moveToFirst()) {
            val = c.getString(0);
            c.close();
        }
        return val;
    }

    public void setSetting(String key, String val) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("setting_key", key);
        cv.put("setting_value", val);
        db.insertWithOnConflict("shop_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
