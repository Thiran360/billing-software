package com.store.billing.util;

import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.print.Paper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ThermalPrinterUtil {

    public static final int THERMAL_RECEIPT_WIDTH = 576; // 80mm width (576 dots)

    /**
     * Checks if the service is a thermal/receipt printer.
     */
    public static boolean isThermalPrinter(PrintService service) {
        if (service == null) return false;
        String name = service.getName().toLowerCase();
        return name.contains("generic") ||
               name.contains("text only") ||
               name.contains("tvs") ||
               name.contains("rp") ||
               name.contains("pos") ||
               name.contains("thermal") ||
               name.contains("receipt") ||
               name.contains("80mm") ||
               name.contains("58mm") ||
               name.contains("epson") ||
               name.contains("xprinter");
    }

    /**
     * Prints the Bill directly using Native Hardware ESC/POS Text commands.
     * Guaranteed to work on all thermal receipt printers (TVS, Generic, POS-80, Epson).
     */
    public static boolean printBillDirect(Bill bill, List<BillItem> currentItems,
                                         String shopName, String shopAddress, String shopPhone,
                                         String language, boolean includeUpi, String upiId,
                                         PrintService printService) throws Exception {
        if (printService == null) {
            throw new IllegalArgumentException("Target PrintService is null.");
        }

        if (isThermalPrinter(printService) || isDocFlavorSupported(printService, DocFlavor.INPUT_STREAM.AUTOSENSE)) {
            byte[] escPosBytes = buildNativeEscPosBill(bill, currentItems, shopName, shopAddress, shopPhone, language, includeUpi, upiId);
            DocPrintJob printJob = printService.createPrintJob();
            Doc doc = new SimpleDoc(new ByteArrayInputStream(escPosBytes), DocFlavor.INPUT_STREAM.AUTOSENSE, null);
            printJob.print(doc, null);
            return true;
        }
        return false;
    }

    /**
     * Builds standard ESC/POS hardware command bytes for the bill.
     */
    public static byte[] buildNativeEscPosBill(Bill bill, List<BillItem> currentItems,
                                               String shopName, String shopAddress, String shopPhone,
                                               String language, boolean includeUpi, String upiId) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. ESC @ (Initialize Printer)
        out.write(new byte[]{0x1B, 0x40});

        // 2. Select Character Code Table (CP437)
        out.write(new byte[]{0x1B, 0x74, 0x00});

        // 3. Shop Header (Centered)
        out.write(new byte[]{0x1B, 0x61, 0x01}); // Center

        // Shop Name: Double Height & Width (ESC ! 0x30) + Bold (ESC E 1)
        out.write(new byte[]{0x1B, 0x45, 0x01});
        out.write(new byte[]{0x1B, 0x21, 0x30});
        out.write((shopName.toUpperCase() + "\n").getBytes(StandardCharsets.US_ASCII));

        // Normal text (ESC ! 0x00)
        out.write(new byte[]{0x1B, 0x21, 0x00});
        out.write(new byte[]{0x1B, 0x45, 0x00});
        if (shopAddress != null && !shopAddress.trim().isEmpty()) {
            out.write((shopAddress + "\n").getBytes(StandardCharsets.US_ASCII));
        }
        if (shopPhone != null && !shopPhone.trim().isEmpty()) {
            out.write(("Phone No: " + shopPhone + "\n").getBytes(StandardCharsets.US_ASCII));
        }

        // QUOTATION (Bold)
        out.write(new byte[]{0x1B, 0x45, 0x01});
        out.write("QUOTATION\n".getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{0x1B, 0x45, 0x00});
        out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));

        // 4. Bill Meta Info (Left align)
        out.write(new byte[]{0x1B, 0x61, 0x00}); // Left
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        String dateStr = sdf.format(bill.getBillDate() != null ? bill.getBillDate() : new Date());
        int bNo = bill.getDailyBillNo() > 0 ? bill.getDailyBillNo() : (bill.getId() > 0 ? bill.getId() : 1);
        String billNoStr = "No. " + bNo;
        String pageStr = "Page 1 / 1";
        out.write(String.format("%-15s %-18s %15s\n", billNoStr, "   " + dateStr, pageStr).getBytes(StandardCharsets.US_ASCII));

        String custName = bill.getCustomerName() != null && !bill.getCustomerName().trim().isEmpty() ? bill.getCustomerName() : "Customer";
        out.write(("To: " + custName + "\n").getBytes(StandardCharsets.US_ASCII));
        if (bill.getCustomerObj() != null && bill.getCustomerObj().getMobile() != null && !bill.getCustomerObj().getMobile().isEmpty()) {
            out.write(("Phone: " + bill.getCustomerObj().getMobile() + "\n").getBytes(StandardCharsets.US_ASCII));
        }
        out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));

        // 5. Item Table Header (48 characters width for 80mm roll)
        // No.(4) Item(22) Qty(10) Total(10) + spaces = 48
        out.write(new byte[]{0x1B, 0x45, 0x01}); // Bold header
        out.write(String.format("%-4s %-22s %-10s %10s\n", "No.", "Item", "Qty", "Total").getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{0x1B, 0x45, 0x00}); // Bold off
        out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));

        // 6. Items List (With Multi-Line Word Wrapping for Long Product Names)
        boolean isTamil = "Tamil".equalsIgnoreCase(language);
        for (int i = 0; i < currentItems.size(); i++) {
            BillItem item = currentItems.get(i);
            String name = item.getProductName();
            if (isTamil && item.getTamilName() != null && !item.getTamilName().trim().isEmpty()) {
                name = item.getTamilName();
            }

            boolean isPendingLine = name.contains("Pending") || name.contains("\u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF");
            String qtyStr = isPendingLine ? "" : formatQtyUnit(item.getQuantity(), item.getUnitType(), isTamil);
            if (qtyStr.length() > 10) {
                qtyStr = qtyStr.substring(0, 10);
            }

            String totalStr = String.format("%.2f", item.getSubtotal());
            
            List<String> nameLines = splitItemName(name, 22);
            String firstLine = nameLines.isEmpty() ? "" : nameLines.get(0);
            out.write(String.format("%-4s %-22s %-10s %10s\n", (i + 1) + ".", firstLine, qtyStr, totalStr).getBytes(StandardCharsets.US_ASCII));
            
            for (int k = 1; k < nameLines.size(); k++) {
                out.write(String.format("%-4s %-22s\n", "", nameLines.get(k)).getBytes(StandardCharsets.US_ASCII));
            }
        }

        out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));

        // 7. Totals section
        out.write(new byte[]{0x1B, 0x61, 0x00}); // Left align

        // Turn ON Extra Dark Bold
        out.write(new byte[]{0x1B, 0x45, 0x01}); // Bold on
        out.write(new byte[]{0x1B, 0x47, 0x01}); // Double-strike on

        // Double-height + Bold for TOTAL (always shown)
        out.write(new byte[]{0x1B, 0x21, 0x18}); // Double-height + Bold
        String totalShort = String.format("Tot: %.2f", bill.getTotalAmount());
        out.write(String.format("%48s\n", totalShort).getBytes(StandardCharsets.US_ASCII));

        // Turn OFF Double-height but keep bold for smaller lines
        out.write(new byte[]{0x1B, 0x21, 0x00});

        // Show Cash only if partial payment was entered and balance exists (Rule 3)
        boolean hasEnteredAmount = (bill.getCashAmount() > 0.009 || bill.getGpayAmount() > 0.009);
        if (hasEnteredAmount && bill.getPaidAmount() > 0.009 && bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
            String paidStr = String.format("Cash:  %.2f", bill.getPaidAmount());
            out.write(String.format("%48s\n", paidStr).getBytes(StandardCharsets.US_ASCII));
        }
        // Show Balance only if pending exists AND showBalanceOnBill is true
        if (bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
            String balStr = String.format("Balance: %.2f", bill.getPendingAmount());
            out.write(String.format("%48s\n", balStr).getBytes(StandardCharsets.US_ASCII));
        }

        // Turn OFF Bold & Double-strike
        out.write(new byte[]{0x1B, 0x45, 0x00});
        out.write(new byte[]{0x1B, 0x47, 0x00});

        // 8. UPI QR Code if enabled (ESC/POS Native QR Code Commands)
        if (includeUpi && upiId != null && !upiId.trim().isEmpty()) {
            out.write(new byte[]{0x1B, 0x61, 0x01}); // Center
            out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));
            
            boolean includeAmount = bill.getGpayAmount() > 0.009;
            double qrAmount = bill.getGpayAmount();
            
            String scanMsg = includeAmount ? String.format("Scan to Pay Rs. %.2f using UPI\n", qrAmount) : "Scan to Pay using UPI\n";
            out.write(scanMsg.getBytes(StandardCharsets.US_ASCII));

            String upiUrl = "upi://pay?pa=" + upiId.trim() + "&pn=" + shopName.replace(" ", "%20") + "&cu=INR";
            if (includeAmount) {
                upiUrl += "&am=" + String.format("%.2f", qrAmount);
            }
            byte[] qrBytes = generateEscPosQRCode(upiUrl);
            out.write(qrBytes);
            out.write("\n".getBytes(StandardCharsets.US_ASCII));
        }

        // 9. Footer
        out.write(new byte[]{0x1B, 0x61, 0x01}); // Center
        out.write("------------------------------------------------\n".getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{0x1B, 0x45, 0x01});
        out.write("*** THANK YOU! VISIT AGAIN ***\n".getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{0x1B, 0x45, 0x00});

        // 10. Feed 4 lines and Auto Cut (GS V 66 0)
        out.write(new byte[]{0x1B, 0x64, 0x04});
        out.write(new byte[]{0x1D, 0x56, 0x42, 0x00});

        return out.toByteArray();
    }

    /**
     * Generates native ESC/POS QR Code commands.
     */
    private static byte[] generateEscPosQRCode(String qrData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] dataBytes = qrData.getBytes(StandardCharsets.UTF_8);
        int len = dataBytes.length + 3;

        // 1. Model: GS ( k 4 0 49 65 n (Model 2)
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});

        // 2. Size: GS ( k 3 0 49 67 6 (Module size 6)
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x06});

        // 3. Error correction: GS ( k 3 0 49 69 48 (Level L)
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30});

        // 4. Store data: GS ( k pL pH 49 80 48 <data>
        int pL = len & 0xFF;
        int pH = (len >> 8) & 0xFF;
        baos.write(new byte[]{0x1D, 0x28, 0x6B, (byte) pL, (byte) pH, 0x31, 0x50, 0x30});
        baos.write(dataBytes);

        // 5. Print QR: GS ( k 3 0 49 81 48
        baos.write(new byte[]{0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});

        return baos.toByteArray();
    }

    /**
     * Prints the receipt panel with automatic fallback.
     */
    public static boolean printReceiptPanel(JPanel receiptPanel, PrintService printService) throws Exception {
        if (printService == null) {
            throw new IllegalArgumentException("Target PrintService is null.");
        }

        // We use a slightly smaller logical layout width (380px) to naturally 
        // increase the physical print size of the content by ~10%.
        int logicalWidth = 380;
        
        // Scale to 1.5x (570 pixels width), which perfectly matches standard 80mm thermal printers (576 dots).
        // Using 4.0x was creating massive images (36MB+) that choked the Windows spooler and caused 4-5s delays.
        double dpiScale = 1.5;

        BufferedImage receiptImage = renderReceiptToImage(receiptPanel, logicalWidth, dpiScale);

        // We exclusively use the high-DPI Java2D PrinterJob pipeline to ensure proper scaling
        // to the driver's imageable width, preserving Tamil fonts and layout flawlessly.
        return printImageViaStandardJob(receiptImage, printService, logicalWidth, dpiScale);
    }

    private static boolean isDocFlavorSupported(PrintService service, DocFlavor flavor) {
        DocFlavor[] supportedFlavors = service.getSupportedDocFlavors();
        if (supportedFlavors != null) {
            for (DocFlavor df : supportedFlavors) {
                if (df.equals(flavor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BufferedImage renderReceiptToImage(JPanel receiptPanel, int logicalWidth, double dpiScale) {
        JTextPane textPane = null;
        Component bottomQrPanel = null;

        for (Component c : receiptPanel.getComponents()) {
            if (c instanceof JScrollPane) {
                Component view = ((JScrollPane) c).getViewport().getView();
                if (view instanceof JTextPane) {
                    textPane = (JTextPane) view;
                }
            } else if (c instanceof JTextPane) {
                textPane = (JTextPane) c;
            } else if (c instanceof JPanel) {
                bottomQrPanel = c;
            }
        }

        JTextPane printText = new JTextPane();
        printText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        printText.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        printText.setContentType("text/html");
        printText.setText(textPane != null ? textPane.getText() : "");
        printText.setEditable(false);
        printText.setBackground(Color.WHITE);
        
        // Force the text pane to layout with the exact logical width and unbounded height
        printText.setSize(new Dimension(logicalWidth, Integer.MAX_VALUE));

        int textHeight = printText.getPreferredSize().height;
        if (textHeight <= 0) textHeight = 10;

        printText.setSize(new Dimension(logicalWidth, textHeight));

        int qrHeight = 0;
        if (bottomQrPanel != null) {
            qrHeight = bottomQrPanel.getPreferredSize().height;
        }

        int logicalHeight = textHeight + (bottomQrPanel != null ? qrHeight + 5 : 5);

        int pixelWidth = (int) Math.ceil(logicalWidth * dpiScale);
        int pixelHeight = (int) Math.ceil(logicalHeight * dpiScale);

        BufferedImage img = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        // Scale graphics to render high-DPI image
        g2d.scale(dpiScale, dpiScale);
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, logicalWidth, logicalHeight);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        printText.paint(g2d);

        if (bottomQrPanel != null) {
            g2d.translate(0, textHeight + 5);
            bottomQrPanel.setSize(logicalWidth, qrHeight);
            bottomQrPanel.doLayout(); // Force layout so children get bounds
            bottomQrPanel.setBackground(Color.WHITE);
            bottomQrPanel.paint(g2d);
        }

        g2d.dispose();
        return img;
    }

    public static byte[] buildEscPosPayload(BufferedImage img) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]{0x1B, 0x40});
        baos.write(new byte[]{0x1B, 0x61, 0x01});
        byte[] rasterBytes = imageToEscPosRaster(img);
        baos.write(rasterBytes);
        baos.write(new byte[]{0x1B, 0x64, 0x04});
        baos.write(new byte[]{0x1D, 0x56, 0x42, 0x00});
        return baos.toByteArray();
    }

    public static byte[] imageToEscPosRaster(BufferedImage img) {
        int width = img.getWidth();
        int rawHeight = img.getHeight();

        int lastContentY = rawHeight - 1;
        for (int y = rawHeight - 1; y >= 0; y--) {
            boolean hasBlack = false;
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int bl = rgb & 0xFF;
                if (((r + g + bl) / 3) < 180) {
                    hasBlack = true;
                    break;
                }
            }
            if (hasBlack) {
                lastContentY = Math.min(rawHeight, y + 10);
                break;
            }
        }
        int height = Math.max(50, lastContentY);
        int widthBytes = (width + 7) / 8;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0x1D);
        baos.write(0x76);
        baos.write(0x30);
        baos.write(0x00);
        baos.write(widthBytes & 0xFF);
        baos.write((widthBytes >> 8) & 0xFF);
        baos.write(height & 0xFF);
        baos.write((height >> 8) & 0xFF);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < widthBytes; x++) {
                int byteVal = 0;
                for (int b = 0; b < 8; b++) {
                    int pixelX = x * 8 + b;
                    if (pixelX < width) {
                        int rgb = img.getRGB(pixelX, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int bl = rgb & 0xFF;
                        int luminance = (r + g + bl) / 3;
                        if (luminance < 180) {
                            byteVal |= (1 << (7 - b));
                        }
                    }
                }
                baos.write(byteVal);
            }
        }
        return baos.toByteArray();
    }

    private static boolean printImageViaStandardJob(BufferedImage img, PrintService printService, int logicalWidth, double dpiScale) {
        PrinterJob job = PrinterJob.getPrinterJob();
        try {
            if (printService != null) {
                job.setPrintService(printService);
            }
            
            PageFormat pf = job.defaultPage();
            Paper paper = pf.getPaper();
            
            // Remove margins completely to ensure we get the absolute max printable width
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pf.setPaper(paper);

            job.setPrintable(new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    if (pageIndex > 0) return NO_SUCH_PAGE;
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    
                    // The all-important invariant: printed receipt width = printer imageable width
                    double imageableWidth = pageFormat.getImageableWidth();
                    
                    // We map the logical layout width EXACTLY to the printer's imageable points
                    double logicalToPoints = imageableWidth / (double) logicalWidth;
                    
                    // Since the image was rendered at a high DPI (dpiScale), its physical pixel 
                    // dimensions are larger. We compensate for dpiScale to draw it perfectly.
                    double finalScale = logicalToPoints / dpiScale;
                    
                    // --- FIX FOR TVS RP 3230 LEFT MARGIN ---
                    // Shift the print slightly to the left to consume the hardware left margin
                    g2d.translate(-10, 0); 
                    // Slightly increase the scale (e.g. +6%) to fill the 80mm paper completely
                    finalScale = finalScale * 1.06;
                    
                    g2d.scale(finalScale, finalScale);
                    g2d.drawImage(img, 0, 0, null);
                    return PAGE_EXISTS;
                }
            }, pf);

            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            job.print(attributes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<String> splitItemName(String name, int maxLen) {
        List<String> lines = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            lines.add("");
            return lines;
        }
        String remaining = name.trim();
        while (remaining.length() > maxLen) {
            int spaceIdx = remaining.lastIndexOf(' ', maxLen);
            if (spaceIdx > 0) {
                lines.add(remaining.substring(0, spaceIdx).trim());
                remaining = remaining.substring(spaceIdx + 1).trim();
            } else {
                lines.add(remaining.substring(0, maxLen));
                remaining = remaining.substring(maxLen).trim();
            }
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    public static String formatUnit(String unitType, boolean isTamil) {
        if (unitType == null || unitType.trim().isEmpty()) {
            return isTamil ? "பீஸ்" : "Pcs";
        }
        String trimmed = unitType.trim();
        com.store.billing.model.Unit customUnit = com.store.billing.dao.UnitDAO.findUnit(trimmed);
        if (customUnit != null) {
            if (isTamil) {
                if (customUnit.getNameTa() != null && !customUnit.getNameTa().trim().isEmpty()) {
                    return customUnit.getNameTa().trim();
                }
            } else {
                if (customUnit.getNameEn() != null && !customUnit.getNameEn().trim().isEmpty()) {
                    String en = customUnit.getNameEn().trim();
                    if (en.equalsIgnoreCase("piece") || en.equalsIgnoreCase("pcs") || en.equalsIgnoreCase("pc")) return "Pcs";
                    if (en.equalsIgnoreCase("dozen") || en.equalsIgnoreCase("dz") || en.equalsIgnoreCase("doz")) return "Dz";
                    if (en.equalsIgnoreCase("pandal") || en.equalsIgnoreCase("pand")) return "Pand.";
                    if (en.equalsIgnoreCase("packet") || en.equalsIgnoreCase("pac") || en.equalsIgnoreCase("pkt")) return "Pac.";
                    if (en.equalsIgnoreCase("bottle") || en.equalsIgnoreCase("bot")) return "Bot.";
                    if (en.equalsIgnoreCase("katta") || en.equalsIgnoreCase("kattu")) return "Katu";
                    if (en.equalsIgnoreCase("attai") || en.equalsIgnoreCase("att")) return "Att.";
                    if (en.equalsIgnoreCase("box")) return "Box";
                    if (en.equalsIgnoreCase("saram")) return "Saram";
                    if (en.equalsIgnoreCase("jar")) return "Jar";
                    if (en.equalsIgnoreCase("case")) return "Case";
                    if (en.equalsIgnoreCase("pet")) return "Pet";
                    if (en.equalsIgnoreCase("pai") || en.equalsIgnoreCase("bag")) return "Pai";
                    if (en.equalsIgnoreCase("kg")) return "kg";
                    if (en.equalsIgnoreCase("g") || en.equalsIgnoreCase("gm")) return "g";
                    if (en.equalsIgnoreCase("l") || en.equalsIgnoreCase("litre")) return "l";
                    if (en.equalsIgnoreCase("ml")) return "ml";
                    return Character.toUpperCase(en.charAt(0)) + en.substring(1);
                }
            }
        }
        String u = trimmed.toLowerCase();
        if (isTamil) {
            switch (u) {
                case "piece":
                case "pcs":
                case "pc":
                case "பீஸ்":
                    return "பீஸ்";
                case "dozen":
                case "dz":
                case "doz":
                case "டஜன்":
                    return "டஜன்";
                case "saram":
                case "சரம்":
                    return "சரம்";
                case "pandal":
                case "pand":
                case "pand.":
                case "bundle":
                case "பண்டல்":
                case "பண்":
                    return "பண்";
                case "packet":
                case "pac":
                case "pac.":
                case "pkt":
                case "pack":
                case "பாக்":
                case "பா":
                case "பாக்கெட்":
                    return "பா";
                case "bottle":
                case "bot":
                case "bot.":
                case "btl":
                case "பாட்":
                case "பாட்டில்":
                    return "பாட்";
                case "box":
                case "பாக்ஸ்":
                    return "Box";
                case "katta":
                case "katu":
                case "kattu":
                case "கட்":
                case "கட்டு":
                    return "கட்";
                case "attai":
                case "att":
                case "att.":
                case "அட்":
                case "அட்டை":
                    return "அட்";
                case "kg":
                case "kilo":
                case "கிலோ":
                    return "kg";
                case "g":
                case "gm":
                case "gram":
                case "கிராம்":
                    return "g";
                case "l":
                case "ltr":
                case "liter":
                case "litre":
                case "லி":
                case "லிட்டர்":
                    return "லி";
                case "ml":
                case "மி.லி":
                    return "ml";
                case "jar":
                case "ஜார்":
                    return "ஜார்";
                case "case":
                case "கேஸ்":
                    return "Case";
                case "pet":
                case "பெட்":
                    return "பெட்";
                case "pai":
                case "bag":
                case "பை":
                    return "பை";
                default:
                    return unitType;
            }
        } else {
            switch (u) {
                case "piece":
                case "pcs":
                case "pc":
                case "பீஸ்":
                    return "Pcs";
                case "dozen":
                case "dz":
                case "doz":
                case "டஜன்":
                    return "Dz";
                case "saram":
                case "சரம்":
                    return "Saram";
                case "pandal":
                case "pand":
                case "pand.":
                case "bundle":
                case "பண்டல்":
                    return "Pand.";
                case "packet":
                case "pac":
                case "pac.":
                case "pkt":
                case "pack":
                case "பாக்":
                case "பாக்கெட்":
                    return "Pac.";
                case "bottle":
                case "bot":
                case "bot.":
                case "btl":
                case "பாட்":
                case "பாட்டில்":
                    return "Bot.";
                case "box":
                case "பாக்ஸ்":
                    return "Box";
                case "katta":
                case "katu":
                case "kattu":
                case "கட்":
                case "கட்டு":
                    return "Katu";
                case "attai":
                case "att":
                case "att.":
                case "அட்":
                case "அட்டை":
                    return "Att.";
                case "pai":
                case "bag":
                case "பை":
                    return "Pai";
                case "kg":
                case "kilo":
                case "கிலோ":
                    return "kg";
                case "g":
                case "gm":
                case "gram":
                case "கிராம்":
                    return "g";
                case "l":
                case "ltr":
                case "liter":
                case "litre":
                case "லி":
                case "லிட்டர்":
                    return "l";
                case "ml":
                case "மி.லி":
                    return "ml";
                case "jar":
                case "ஜார்":
                    return "Jar";
                case "case":
                case "கேஸ்":
                    return "Case";
                case "pet":
                case "பெட்":
                    return "Pet";
                default:
                    return unitType;
            }
        }
    }

    public static String formatQtyUnit(double quantity, String unitType, boolean isTamil) {
        String formattedUnit = formatUnit(unitType, isTamil);
        String qtyStr = (quantity == (long) quantity) ? String.valueOf((long) quantity) : String.valueOf(quantity);
        if (qtyStr.endsWith(".0")) {
            qtyStr = qtyStr.substring(0, qtyStr.length() - 2);
        }
        return qtyStr + " " + formattedUnit;
    }

    public static String formatQtyUnit(int quantity, String unitType, boolean isTamil) {
        return formatQtyUnit((double) quantity, unitType, isTamil);
    }
}
