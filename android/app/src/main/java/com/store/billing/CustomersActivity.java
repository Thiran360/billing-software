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
import com.store.billing.model.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomersActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private EditText etSearchCustomer;
    private RecyclerView rvCustomers;
    private final List<Customer> customerList = new ArrayList<>();
    private CustomerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Customer Ledger");
        }

        db = AppDatabaseHelper.getInstance(this);

        etSearchCustomer = findViewById(R.id.etSearchCustomer);
        Button btnAddCustomer = findViewById(R.id.btnAddCustomer);
        rvCustomers = findViewById(R.id.rvCustomers);

        adapter = new CustomerAdapter(customerList, this::showCustomerDialog);
        rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        rvCustomers.setAdapter(adapter);

        loadCustomers("");

        btnAddCustomer.setOnClickListener(v -> showCustomerDialog(null));

        etSearchCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadCustomers(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomers(String query) {
        customerList.clear();
        customerList.addAll(db.searchCustomers(query));
        adapter.notifyDataSetChanged();
    }

    private void showCustomerDialog(Customer existing) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existing == null ? "Add New Customer" : "Edit Customer");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        EditText etName = view.findViewById(R.id.etCustName);
        EditText etMobile = view.findViewById(R.id.etCustMobile);
        EditText etAddress = view.findViewById(R.id.etCustAddress);
        EditText etPending = view.findViewById(R.id.etCustPending);

        if (existing != null) {
            etName.setText(existing.getName());
            etMobile.setText(existing.getMobile());
            etAddress.setText(existing.getAddress());
            etPending.setText(String.valueOf(existing.getPendingAmount()));
        }

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Customer name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Customer c = existing != null ? existing : new Customer();
            c.setName(name);
            c.setMobile(etMobile.getText().toString().trim());
            c.setAddress(etAddress.getText().toString().trim());
            try {
                c.setPendingAmount(Double.parseDouble(etPending.getText().toString().trim()));
            } catch (Exception e) {
                c.setPendingAmount(0);
            }

            db.saveCustomer(c);
            loadCustomers(etSearchCustomer.getText().toString());
            Toast.makeText(this, "Customer saved!", Toast.LENGTH_SHORT).show();
        });

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

    interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    static class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {
        private final List<Customer> list;
        private final OnCustomerClickListener listener;

        public CustomerAdapter(List<Customer> list, OnCustomerClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Customer c = list.get(position);
            holder.tvName.setText(c.getName());
            holder.tvMobile.setText(c.getMobile() != null && !c.getMobile().isEmpty() ? "📱 " + c.getMobile() : "-");
            holder.tvAddress.setText(c.getAddress() != null && !c.getAddress().isEmpty() ? c.getAddress() : "");
            holder.tvPending.setText(String.format(Locale.getDefault(), "₹ %.2f", c.getPendingAmount()));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCustomerClick(c);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvMobile, tvAddress, tvPending;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvCustomerName);
                tvMobile = itemView.findViewById(R.id.tvCustomerMobile);
                tvAddress = itemView.findViewById(R.id.tvCustomerAddress);
                tvPending = itemView.findViewById(R.id.tvPendingDue);
            }
        }
    }
}
