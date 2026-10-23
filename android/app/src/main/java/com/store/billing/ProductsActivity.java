package com.store.billing;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.store.billing.db.AppDatabaseHelper;
import com.store.billing.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductsActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private EditText etSearchProduct;
    private RecyclerView rvProducts;
    private final List<Product> productList = new ArrayList<>();
    private ProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Products & Inventory");
        }

        db = AppDatabaseHelper.getInstance(this);

        etSearchProduct = findViewById(R.id.etSearchProduct);
        Button btnAddNewProduct = findViewById(R.id.btnAddNewProduct);
        rvProducts = findViewById(R.id.rvProducts);

        adapter = new ProductAdapter(productList, this::showProductDialog);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);

        loadProducts("");

        btnAddNewProduct.setOnClickListener(v -> showProductDialog(null));

        etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadProducts(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadProducts(String query) {
        productList.clear();
        productList.addAll(db.searchProducts(query));
        adapter.notifyDataSetChanged();
    }

    private void showProductDialog(Product existing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existing == null ? "Add New Product" : "Edit Product");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
        EditText etName = view.findViewById(R.id.etProdName);
        EditText etTamil = view.findViewById(R.id.etProdTamil);
        EditText etBarcode = view.findViewById(R.id.etProdBarcode);
        EditText etStock = view.findViewById(R.id.etProdStock);
        EditText etUnit = view.findViewById(R.id.etProdUnit);
        EditText etRetail = view.findViewById(R.id.etProdRetail);
        EditText etWhole = view.findViewById(R.id.etProdWhole);

        if (existing != null) {
            etName.setText(existing.getName());
            etTamil.setText(existing.getTamilName());
            etBarcode.setText(existing.getBarcode());
            etStock.setText(String.valueOf(existing.getStockQuantity()));
            etUnit.setText(existing.getUnitType());
            etRetail.setText(String.valueOf(existing.getRetailPrice()));
            etWhole.setText(String.valueOf(existing.getWholePrice()));
        }

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Product name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Product p = existing != null ? existing : new Product();
            p.setName(name);
            p.setTamilName(etTamil.getText().toString().trim());
            p.setBarcode(etBarcode.getText().toString().trim());
            try { p.setStockQuantity(Integer.parseInt(etStock.getText().toString().trim())); } catch (Exception e) { p.setStockQuantity(0); }
            p.setUnitType(etUnit.getText().toString().trim().isEmpty() ? "piece" : etUnit.getText().toString().trim());
            try { p.setRetailPrice(Double.parseDouble(etRetail.getText().toString().trim())); } catch (Exception e) { p.setRetailPrice(0); }
            try { p.setWholePrice(Double.parseDouble(etWhole.getText().toString().trim())); } catch (Exception e) { p.setWholePrice(0); }

            db.saveProduct(p);
            loadProducts(etSearchProduct.getText().toString());
            Toast.makeText(this, "Product saved!", Toast.LENGTH_SHORT).show();
        });

        if (existing != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                db.deleteProduct(existing.getId());
                loadProducts(etSearchProduct.getText().toString());
                Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    interface OnProductClickListener {
        void onProductClick(Product product);
    }

    static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private final List<Product> list;
        private final OnProductClickListener listener;

        public ProductAdapter(List<Product> list, OnProductClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product p = list.get(position);
            holder.tvName.setText(p.getName());
            holder.tvTamil.setText(p.getTamilName() != null && !p.getTamilName().isEmpty() ? p.getTamilName() : "-");
            holder.tvStock.setText("Stock: " + p.getStockQuantity() + " " + p.getUnitType());
            holder.tvRetail.setText(String.format(Locale.getDefault(), "Retail: ₹ %.2f", p.getRetailPrice()));
            holder.tvWhole.setText(String.format(Locale.getDefault(), "Whole: ₹ %.2f", p.getWholePrice()));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onProductClick(p);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTamil, tvStock, tvRetail, tvWhole;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvProductName);
                tvTamil = itemView.findViewById(R.id.tvProductTamil);
                tvStock = itemView.findViewById(R.id.tvProductStock);
                tvRetail = itemView.findViewById(R.id.tvRetailPrice);
                tvWhole = itemView.findViewById(R.id.tvWholePrice);
            }
        }
    }
}
