package com.store.billing;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.store.billing.adapter.CartAdapter;
import com.store.billing.db.AppDatabaseHelper;
import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;
import com.store.billing.model.Customer;
import com.store.billing.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BillingActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private AppDatabaseHelper db;
    private Spinner spinnerCustomer;
    private RadioGroup rgPriceType, rgPaymentMode;
    private AutoCompleteTextView actvProductSearch;
    private RecyclerView rvCartItems;
    private TextView tvItemsCount, tvGrandTotal;
    private MaterialButton btnCheckout;

    private final List<Product> allProducts = new ArrayList<>();
    private final List<Customer> allCustomers = new ArrayList<>();
    private final List<BillItem> cartItems = new ArrayList<>();
    private CartAdapter cartAdapter;

    private Product selectedProduct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("New Bill (POS)");
        }

        db = AppDatabaseHelper.getInstance(this);

        spinnerCustomer = findViewById(R.id.spinnerCustomer);
        rgPriceType = findViewById(R.id.rgPriceType);
        rgPaymentMode = findViewById(R.id.rgPaymentMode);
        actvProductSearch = findViewById(R.id.actvProductSearch);
        rvCartItems = findViewById(R.id.rvCartItems);
        tvItemsCount = findViewById(R.id.tvItemsCount);
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        Button btnAddProduct = findViewById(R.id.btnAddProduct);

        cartAdapter = new CartAdapter(cartItems, this);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvCartItems.setAdapter(cartAdapter);

        loadCustomers();
        loadProducts();

        btnAddProduct.setOnClickListener(v -> addProductToCart());
        btnCheckout.setOnClickListener(v -> checkoutBill());

        rgPriceType.setOnCheckedChangeListener((group, checkedId) -> {
            updateCartPrices();
        });
    }

    private void loadCustomers() {
        allCustomers.clear();
        allCustomers.addAll(db.getAllCustomers());
        if (allCustomers.isEmpty()) {
            allCustomers.add(new Customer(0, "Cash Customer (Walk-in)", "", "", 0));
        }
        ArrayAdapter<Customer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, allCustomers);
        spinnerCustomer.setAdapter(adapter);
    }

    private void loadProducts() {
        allProducts.clear();
        allProducts.addAll(db.getAllProducts());

        List<String> displayList = new ArrayList<>();
        for (Product p : allProducts) {
            displayList.add(p.getDisplayName());
        }

        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, displayList);
        actvProductSearch.setAdapter(searchAdapter);

        actvProductSearch.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            for (Product p : allProducts) {
                if (p.getDisplayName().equalsIgnoreCase(selected)) {
                    selectedProduct = p;
                    break;
                }
            }
        });
    }

    private void addProductToCart() {
        String input = actvProductSearch.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter or select a product", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedProduct == null) {
            for (Product p : allProducts) {
                if (p.getDisplayName().equalsIgnoreCase(input) || 
                    p.getName().equalsIgnoreCase(input) || 
                    p.getBarcode().equalsIgnoreCase(input)) {
                    selectedProduct = p;
                    break;
                }
            }
        }

        if (selectedProduct == null) {
            Toast.makeText(this, "Product not found: " + input, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already in cart
        for (BillItem item : cartItems) {
            if (item.getProductId() == selectedProduct.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                cartAdapter.notifyDataSetChanged();
                onCartChanged();
                actvProductSearch.setText("");
                selectedProduct = null;
                return;
            }
        }

        boolean isWholesale = rgPriceType.getCheckedRadioButtonId() == R.id.rbWholesale;
        double price = isWholesale ? selectedProduct.getWholePrice() : selectedProduct.getRetailPrice();
        if (price <= 0) {
            price = selectedProduct.getRetailPrice() > 0 ? selectedProduct.getRetailPrice() : selectedProduct.getWholePrice();
        }

        BillItem newItem = new BillItem(
                selectedProduct.getId(),
                selectedProduct.getName(),
                selectedProduct.getTamilName(),
                selectedProduct.getUnitType(),
                1,
                price,
                price
        );

        cartItems.add(newItem);
        cartAdapter.notifyItemInserted(cartItems.size() - 1);
        onCartChanged();

        actvProductSearch.setText("");
        selectedProduct = null;
    }

    private void updateCartPrices() {
        boolean isWholesale = rgPriceType.getCheckedRadioButtonId() == R.id.rbWholesale;
        for (BillItem item : cartItems) {
            for (Product p : allProducts) {
                if (p.getId() == item.getProductId()) {
                    double price = isWholesale ? p.getWholePrice() : p.getRetailPrice();
                    if (price > 0) {
                        item.setUnitPrice(price);
                    }
                    break;
                }
            }
        }
        cartAdapter.notifyDataSetChanged();
        onCartChanged();
    }

    @Override
    public void onCartChanged() {
        int totalQty = 0;
        double grandTotal = 0;
        for (BillItem item : cartItems) {
            totalQty += item.getQuantity();
            grandTotal += item.getSubtotal();
        }

        tvItemsCount.setText("Items: " + cartItems.size() + " (" + totalQty + " pcs)");
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", grandTotal));
    }

    private void checkoutBill() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty. Add products to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        double grandTotal = 0;
        for (BillItem item : cartItems) {
            grandTotal += item.getSubtotal();
        }

        Customer selectedCustomer = (Customer) spinnerCustomer.getSelectedItem();
        String paymentMode = "CASH";
        int modeId = rgPaymentMode.getCheckedRadioButtonId();
        if (modeId == R.id.rbUPI) {
            paymentMode = "UPI";
        } else if (modeId == R.id.rbCredit) {
            paymentMode = "CREDIT";
        }

        double paidAmount = paymentMode.equals("CREDIT") ? 0 : grandTotal;
        double pendingAmount = paymentMode.equals("CREDIT") ? grandTotal : 0;

        Bill bill = new Bill();
        bill.setCustomerName(selectedCustomer != null ? selectedCustomer.getName() : "Cash Customer");
        bill.setCustomerId(selectedCustomer != null ? selectedCustomer.getId() : 0);
        bill.setTotalAmount(grandTotal);
        bill.setPaidAmount(paidAmount);
        bill.setPendingAmount(pendingAmount);
        bill.setPaymentMode(paymentMode);
        bill.setItems(new ArrayList<>(cartItems));

        long billId = db.saveBill(bill);
        if (billId > 0) {
            Toast.makeText(this, "Bill #" + billId + " created successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra("bill", bill);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to save bill", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
