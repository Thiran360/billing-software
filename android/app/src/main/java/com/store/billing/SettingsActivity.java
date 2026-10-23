package com.store.billing;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.store.billing.db.AppDatabaseHelper;

public class SettingsActivity extends AppCompatActivity {

    private AppDatabaseHelper db;
    private EditText etShopName, etShopAddress, etShopPhone, etShopGst, etShopUpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Store Settings");
        }

        db = AppDatabaseHelper.getInstance(this);

        etShopName = findViewById(R.id.etSettingShopName);
        etShopAddress = findViewById(R.id.etSettingShopAddress);
        etShopPhone = findViewById(R.id.etSettingShopPhone);
        etShopGst = findViewById(R.id.etSettingShopGst);
        etShopUpi = findViewById(R.id.etSettingShopUpi);
        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);

        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        etShopName.setText(db.getSetting("shop_name", "MUSTAFA TRADERS & STORE"));
        etShopAddress.setText(db.getSetting("shop_address", "Main Bazaar, Tamil Nadu"));
        etShopPhone.setText(db.getSetting("shop_phone", "+91 9876543210"));
        etShopGst.setText(db.getSetting("shop_gst", "33AAAAA0000A1Z5"));
        etShopUpi.setText(db.getSetting("shop_upi", "mustafastore@upi"));
    }

    private void saveSettings() {
        db.setSetting("shop_name", etShopName.getText().toString().trim());
        db.setSetting("shop_address", etShopAddress.getText().toString().trim());
        db.setSetting("shop_phone", etShopPhone.getText().toString().trim());
        db.setSetting("shop_gst", etShopGst.getText().toString().trim());
        db.setSetting("shop_upi", etShopUpi.getText().toString().trim());

        Toast.makeText(this, "Settings updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
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
