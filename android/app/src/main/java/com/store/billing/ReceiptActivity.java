package com.store.billing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.store.billing.db.AppDatabaseHelper;
import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;

import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private Bill bill;
    private AppDatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Receipt");
        }

        db = AppDatabaseHelper.getInstance(this);

        bill = (Bill) getIntent().getSerializableExtra("bill");
        int billId = getIntent().getIntExtra("bill_id", -1);

        if (bill == null && billId > 0) {
            for (Bill b : db.getAllBills()) {
                if (b.getId() == billId) {
                    bill = b;
                    bill.setItems(db.getBillItems(billId));
                    break;
                }
            }
        }

        if (bill == null) {
            Toast.makeText(this, "Receipt not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvShopName = findViewById(R.id.tvReceiptShopName);
        TextView tvShopAddress = findViewById(R.id.tvReceiptShopAddress);
        TextView tvGst = findViewById(R.id.tvReceiptGst);
        TextView tvBillNo = findViewById(R.id.tvReceiptBillNo);
        TextView tvDate = findViewById(R.id.tvReceiptDate);
        TextView tvCustomer = findViewById(R.id.tvReceiptCustomer);
        TextView tvGrandTotal = findViewById(R.id.tvReceiptGrandTotal);
        TextView tvPaymentMode = findViewById(R.id.tvReceiptPaymentMode);
        TextView tvPaidAmount = findViewById(R.id.tvReceiptPaidAmount);
        LinearLayout llItems = findViewById(R.id.llReceiptItems);
        LinearLayout llUpiQr = findViewById(R.id.llUpiQr);
        ImageView ivUpiQr = findViewById(R.id.ivUpiQr);

        MaterialButton btnShareWhatsApp = findViewById(R.id.btnShareWhatsApp);
        MaterialButton btnDone = findViewById(R.id.btnDone);

        tvShopName.setText(db.getSetting("shop_name", "MUSTAFA TRADERS & STORE"));
        String addr = db.getSetting("shop_address", "Main Bazaar, Tamil Nadu") + "\nPh: " + db.getSetting("shop_phone", "+91 9876543210");
        tvShopAddress.setText(addr);
        tvGst.setText("GSTIN: " + db.getSetting("shop_gst", "33AAAAA0000A1Z5"));

        tvBillNo.setText("Bill No: #" + bill.getId());
        tvDate.setText(bill.getBillDate());
        tvCustomer.setText("Customer: " + bill.getCustomerName());
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %.2f", bill.getTotalAmount()));
        tvPaymentMode.setText("Mode: " + bill.getPaymentMode());
        tvPaidAmount.setText(String.format(Locale.getDefault(), "Paid: ₹ %.2f", bill.getPaidAmount()));

        // Populate items table
        llItems.removeAllViews();
        for (BillItem item : bill.getItems()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 4, 0, 4);

            TextView tvName = new TextView(this);
            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            String nameStr = item.getProductName();
            if (item.getTamilName() != null && !item.getTamilName().isEmpty()) {
                nameStr += "\n" + item.getTamilName();
            }
            tvName.setText(nameStr);
            tvName.setTextSize(12);
            tvName.setTextColor(Color.DKGRAY);

            TextView tvQty = new TextView(this);
            tvQty.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(45), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvQty.setGravity(Gravity.CENTER);
            tvQty.setText(String.valueOf(item.getQuantity()));
            tvQty.setTextSize(12);
            tvQty.setTextColor(Color.DKGRAY);

            TextView tvRate = new TextView(this);
            tvRate.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(60), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvRate.setGravity(Gravity.END);
            tvRate.setText(String.format(Locale.getDefault(), "%.2f", item.getUnitPrice()));
            tvRate.setTextSize(12);
            tvRate.setTextColor(Color.DKGRAY);

            TextView tvSub = new TextView(this);
            tvSub.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(70), LinearLayout.LayoutParams.WRAP_CONTENT));
            tvSub.setGravity(Gravity.END);
            tvSub.setText(String.format(Locale.getDefault(), "%.2f", item.getSubtotal()));
            tvSub.setTextSize(12);
            tvSub.setTextColor(Color.BLACK);

            row.addView(tvName);
            row.addView(tvQty);
            row.addView(tvRate);
            row.addView(tvSub);

            llItems.addView(row);
        }

        // Generate UPI QR Code if UPI mode or UPI setting present
        String upiId = db.getSetting("shop_upi", "");
        if (!upiId.trim().isEmpty() && bill.getTotalAmount() > 0) {
            String shopName = db.getSetting("shop_name", "MustafaStore");
            String upiUrl = String.format(Locale.US,
                    "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=Bill%d",
                    upiId.trim(), shopName.replaceAll("[^a-zA-Z0-9]", ""), bill.getTotalAmount(), bill.getId());
            Bitmap qr = generateQrBitmap(upiUrl, 400, 400);
            if (qr != null) {
                ivUpiQr.setImageBitmap(qr);
                llUpiQr.setVisibility(View.VISIBLE);
            }
        }

        btnShareWhatsApp.setOnClickListener(v -> shareViaWhatsApp());
        btnDone.setOnClickListener(v -> finish());
    }

    private Bitmap generateQrBitmap(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void shareViaWhatsApp() {
        StringBuilder sb = new StringBuilder();
        sb.append("🧾 *").append(db.getSetting("shop_name", "MUSTAFA TRADERS & STORE")).append("*\n");
        sb.append(db.getSetting("shop_address", "")).append("\n");
        sb.append("-----------------------------\n");
        sb.append("Bill No: #").append(bill.getId()).append("\n");
        sb.append("Date: ").append(bill.getBillDate()).append("\n");
        sb.append("Customer: ").append(bill.getCustomerName()).append("\n");
        sb.append("-----------------------------\n");
        for (BillItem item : bill.getItems()) {
            sb.append(item.getProductName()).append(" x ").append(item.getQuantity())
                    .append(" = ₹").append(String.format(Locale.getDefault(), "%.2f", item.getSubtotal())).append("\n");
        }
        sb.append("-----------------------------\n");
        sb.append("*Total: ₹").append(String.format(Locale.getDefault(), "%.2f", bill.getTotalAmount())).append("*\n");
        sb.append("Mode: ").append(bill.getPaymentMode()).append("\n");
        sb.append("Thank you! Visit again 🙏");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
