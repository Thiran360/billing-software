package com.store.billing.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    public static ImageIcon generateUPIQRCode(String upiId, String payeeName, double amount, boolean includeAmount, int width, int height) {
        // Construct standard UPI URI
        // upi://pay?pa=UPIID&pn=PAYEENAME&am=AMOUNT&cu=INR
        String upiUrl = "upi://pay?pa=" + upiId + "&pn=" + payeeName.replace(" ", "%20") + "&cu=INR";
        if (includeAmount) {
            upiUrl += "&am=" + String.format("%.2f", amount);
        }

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(upiUrl, BarcodeFormat.QR_CODE, width, height, hints);
            
            // Create a TYPE_INT_RGB image explicitly to avoid Java2D scaling corruption on TYPE_BYTE_BINARY
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2 = image.createGraphics();
            g2.setColor(java.awt.Color.WHITE);
            g2.fillRect(0, 0, width, height);
            g2.setColor(java.awt.Color.BLACK);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    if (bitMatrix.get(x, y)) {
                        g2.fillRect(x, y, 1, 1);
                    }
                }
            }
            g2.dispose();
            
            return new ImageIcon(image);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
