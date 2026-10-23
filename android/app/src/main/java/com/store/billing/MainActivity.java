package com.store.billing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.store.billing.db.AppDatabaseHelper;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private TextView tvShopName, tvShopAddress, tvTodaySales, tvPendingCredit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabaseHelper.getInstance(this);

        tvShopName = findViewById(R.id.tvShopName);
        tvShopAddress = findViewById(R.id.tvShopAddress);
        tvTodaySales = findViewById(R.id.tvTodaySales);
        tvPendingCredit = findViewById(R.id.tvPendingCredit);

        MaterialButton btnNewBill = findViewById(R.id.btnNewBill);
        MaterialCardView cardProducts = findViewById(R.id.cardProducts);
        MaterialCardView cardCustomers = findViewById(R.id.cardCustomers);
        MaterialCardView cardHistory = findViewById(R.id.cardHistory);
        MaterialCardView cardSettings = findViewById(R.id.cardSettings);

        btnNewBill.setOnClickListener(v -> startActivity(new Intent(this, BillingActivity.class)));
        cardProducts.setOnClickListener(v -> startActivity(new Intent(this, ProductsActivity.class)));
        cardCustomers.setOnClickListener(v -> startActivity(new Intent(this, CustomersActivity.class)));
        cardHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        cardSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    private void refreshDashboard() {
        tvShopName.setText(db.getSetting("shop_name", "MUSTAFA TRADERS & STORE"));
        tvShopAddress.setText(db.getSetting("shop_address", "Main Bazaar, Tamil Nadu"));

        double sales = db.getTodaySales();
        double pending = db.getTotalPendingCredit();

        tvTodaySales.setText(String.format(Locale.getDefault(), "₹ %.2f", sales));
        tvPendingCredit.setText(String.format(Locale.getDefault(), "₹ %.2f", pending));
    }
}
