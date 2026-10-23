package com.store.billing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.store.billing.db.AppDatabaseHelper;
import com.store.billing.model.Bill;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private final List<Bill> billList = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Bill History");
        }

        db = AppDatabaseHelper.getInstance(this);

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        adapter = new HistoryAdapter(billList, bill -> {
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra("bill_id", bill.getId());
            startActivity(intent);
        });

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        loadBills();
    }

    private void loadBills() {
        billList.clear();
        billList.addAll(db.getAllBills());
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    interface OnBillClickListener {
        void onBillClick(Bill bill);
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<Bill> list;
        private final OnBillClickListener listener;

        public HistoryAdapter(List<Bill> list, OnBillClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bill b = list.get(position);
            holder.tvBillNo.setText("Bill #" + b.getId());
            holder.tvCustomer.setText("Customer: " + b.getCustomerName());
            holder.tvDate.setText(b.getBillDate());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "₹ %.2f", b.getTotalAmount()));
            holder.tvMode.setText(b.getPaymentMode());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBillClick(b);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvBillNo, tvCustomer, tvDate, tvAmount, tvMode;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBillNo = itemView.findViewById(R.id.tvHistoryBillNo);
                tvCustomer = itemView.findViewById(R.id.tvHistoryCustomer);
                tvDate = itemView.findViewById(R.id.tvHistoryDate);
                tvAmount = itemView.findViewById(R.id.tvHistoryAmount);
                tvMode = itemView.findViewById(R.id.tvHistoryMode);
            }
        }
    }
}
