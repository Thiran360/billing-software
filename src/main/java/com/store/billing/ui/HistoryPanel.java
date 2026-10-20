package com.store.billing.ui;

import com.store.billing.dao.BillDAO;
import com.store.billing.model.Bill;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class HistoryPanel extends JPanel {
    private BillDAO billDAO;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JSpinner startDateSpinner, endDateSpinner;
    private JComboBox<String> cmbCustomerSearch;
    private List<String> allCustomerNames;

    private JLabel lblTotalRevenue;
    private JLabel lblTotalBills;

    public HistoryPanel() {
        billDAO = new BillDAO();
        allCustomerNames = new ArrayList<>();
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));

        // 1. Top Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
                "Filter & Search Billing History",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 14)
        ));
        filterPanel.setBackground(Color.WHITE);

        Date today = new Date();

        Font filterFont = new Font("SansSerif", Font.PLAIN, 14);
        Font labelFont = new Font("Segoe UI", Font.BOLD, 13);

        // Date Filters - Default to Today so only current date's billing history is displayed
        JLabel lblFrom = new JLabel("From:");
        lblFrom.setFont(labelFont);
        filterPanel.add(lblFrom);

        SpinnerDateModel startModel = new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH);
        startDateSpinner = new JSpinner(startModel);
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startDateSpinner, "dd-MM-yyyy");
        startDateSpinner.setEditor(startEditor);
        startDateSpinner.setFont(filterFont);
        startDateSpinner.setPreferredSize(new Dimension(115, 30));
        filterPanel.add(startDateSpinner);

        JLabel lblTo = new JLabel("To:");
        lblTo.setFont(labelFont);
        filterPanel.add(lblTo);

        SpinnerDateModel endModel = new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH);
        endDateSpinner = new JSpinner(endModel);
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endDateSpinner, "dd-MM-yyyy");
        endDateSpinner.setEditor(endEditor);
        endDateSpinner.setFont(filterFont);
        endDateSpinner.setPreferredSize(new Dimension(115, 30));
        filterPanel.add(endDateSpinner);

        startDateSpinner.addChangeListener(e -> {
            Date s = (Date) startDateSpinner.getValue();
            Date en = (Date) endDateSpinner.getValue();
            if (s.after(en)) {
                endDateSpinner.setValue(s);
            }
            loadHistory();
        });
        endDateSpinner.addChangeListener(e -> {
            Date s = (Date) startDateSpinner.getValue();
            Date en = (Date) endDateSpinner.getValue();
            if (en.before(s)) {
                startDateSpinner.setValue(en);
            }
            loadHistory();
        });

        // Customer Name Autocomplete Search Field
        JLabel lblCust = new JLabel("Customer Name:");
        lblCust.setFont(labelFont);
        filterPanel.add(lblCust);

        cmbCustomerSearch = new JComboBox<>();
        cmbCustomerSearch.setEditable(true);
        cmbCustomerSearch.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        cmbCustomerSearch.setPreferredSize(new Dimension(180, 30));
        cmbCustomerSearch.setLightWeightPopupEnabled(false);
        filterPanel.add(cmbCustomerSearch);

        setupCustomerAutocomplete();

        // Search Button
        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setPreferredSize(new Dimension(85, 30));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> loadHistory());
        filterPanel.add(btnSearch);

        // Today Button (Resets to Current Date)
        JButton btnReset = new JButton("Today");
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnReset.setBackground(new Color(40, 167, 69));
        btnReset.setForeground(Color.WHITE);
        btnReset.setFocusPainted(false);
        btnReset.setPreferredSize(new Dimension(80, 30));
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(e -> {
            Date now = new Date();
            startDateSpinner.setValue(now);
            endDateSpinner.setValue(now);
            JTextField editor = (JTextField) cmbCustomerSearch.getEditor().getEditorComponent();
            editor.setText("");
            if (rowSorter != null) rowSorter.setRowFilter(null);
            loadHistory();
        });
        filterPanel.add(btnReset);

        // Show All Button (Shows all billing history across all dates)
        JButton btnShowAll = new JButton("Show All");
        btnShowAll.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnShowAll.setBackground(new Color(108, 117, 125));
        btnShowAll.setForeground(Color.WHITE);
        btnShowAll.setFocusPainted(false);
        btnShowAll.setPreferredSize(new Dimension(85, 30));
        btnShowAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnShowAll.addActionListener(e -> {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -5);
            startDateSpinner.setValue(cal.getTime());
            endDateSpinner.setValue(new Date());
            JTextField editor = (JTextField) cmbCustomerSearch.getEditor().getEditorComponent();
            editor.setText("");
            if (rowSorter != null) rowSorter.setRowFilter(null);
            loadHistory();
        });
        filterPanel.add(btnShowAll);

        // Edit Selected Bill Button
        JButton btnEditBill = new JButton("Edit Selected Bill");
        btnEditBill.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEditBill.setBackground(new Color(230, 126, 34)); // Amber / Orange
        btnEditBill.setForeground(Color.WHITE);
        btnEditBill.setFocusPainted(false);
        btnEditBill.setPreferredSize(new Dimension(140, 30));
        btnEditBill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditBill.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = historyTable.convertRowIndexToModel(row);
                int billId = (int) tableModel.getValueAt(modelRow, 9);
                triggerEditBill(billId);
            } else {
                JOptionPane.showMessageDialog(HistoryPanel.this,
                        "Please select a bill from the table to edit.",
                        "No Bill Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filterPanel.add(btnEditBill);

        // Print / Re-print Selected Bill Button
        JButton btnPrintBill = new JButton("Print / Re-print");
        btnPrintBill.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrintBill.setBackground(new Color(40, 167, 69)); // Green
        btnPrintBill.setForeground(Color.WHITE);
        btnPrintBill.setFocusPainted(false);
        btnPrintBill.setPreferredSize(new Dimension(135, 30));
        btnPrintBill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintBill.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = historyTable.convertRowIndexToModel(row);
                int billId = (int) tableModel.getValueAt(modelRow, 9);
                triggerPrintBill(billId);
            } else {
                JOptionPane.showMessageDialog(HistoryPanel.this,
                        "Please select a bill from the table to print.",
                        "No Bill Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filterPanel.add(btnPrintBill);

        // Delete Bill Button
        JButton btnDeleteBill = new JButton("Delete Bill");
        btnDeleteBill.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDeleteBill.setBackground(new Color(220, 53, 69));
        btnDeleteBill.setForeground(Color.WHITE);
        btnDeleteBill.setFocusPainted(false);
        btnDeleteBill.setPreferredSize(new Dimension(100, 30));
        btnDeleteBill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDeleteBill.addActionListener(e -> {
            int row = historyTable.getSelectedRow();
            if (row >= 0) {
                int modelRow = historyTable.convertRowIndexToModel(row);
                int billId = (int) tableModel.getValueAt(modelRow, 9);
                int confirm = JOptionPane.showConfirmDialog(HistoryPanel.this,
                        "Are you sure you want to delete Bill #" + billId + "?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (billDAO.deleteBill(billId)) {
                        JOptionPane.showMessageDialog(HistoryPanel.this, "Bill #" + billId + " deleted successfully!");
                        loadHistory();
                    } else {
                        JOptionPane.showMessageDialog(HistoryPanel.this, "Failed to delete bill.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(HistoryPanel.this, "Please select a bill to delete.");
            }
        });
        filterPanel.add(btnDeleteBill);

        // Clear All History Button
        JButton btnClearAll = new JButton("Clear All History");
        btnClearAll.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClearAll.setBackground(new Color(108, 117, 125));
        btnClearAll.setForeground(Color.WHITE);
        btnClearAll.setFocusPainted(false);
        btnClearAll.setPreferredSize(new Dimension(130, 30));
        btnClearAll.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearAll.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(HistoryPanel.this,
                    "Are you sure you want to delete ALL billing history? (This cannot be undone)",
                    "Clear All Billing History", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (billDAO.clearAllBillingHistory()) {
                    JOptionPane.showMessageDialog(HistoryPanel.this, "All billing history cleared successfully!");
                    loadHistory();
                } else {
                    JOptionPane.showMessageDialog(HistoryPanel.this, "Failed to clear billing history.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        filterPanel.add(btnClearAll);

        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            {
                ((JSpinner.DefaultEditor)startDateSpinner.getEditor()).getTextField(),
                ((JSpinner.DefaultEditor)endDateSpinner.getEditor()).getTextField(),
                cmbCustomerSearch,
                btnSearch,
                btnReset,
                btnEditBill,
                btnDeleteBill,
                btnClearAll
            }
        });

        add(filterPanel, BorderLayout.NORTH);

        // 2. Table Panel
        String[] columns = {"Bill No", "Customer Name", "Date & Time", "Total Amount", "Paid Amount", "Pending Amount", "Total Pending (Customer)", "Status", "Action", "DB_ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    c.setBackground(new Color(225, 238, 255)); // Soft modern light blue highlight
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                }
                return c;
            }
        };
        historyTable.setRowHeight(36);
        historyTable.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        historyTable.setShowGrid(false);
        historyTable.setShowHorizontalLines(true);
        historyTable.setGridColor(new Color(230, 235, 240));

        rowSorter = new TableRowSorter<>(tableModel);
        historyTable.setRowSorter(rowSorter);

        // Header Styling
        JTableHeader header = historyTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(235, 240, 245));
        header.setForeground(new Color(33, 47, 61));

        // Center align text columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // Right align Amount columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        historyTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        historyTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        historyTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        // Action column renderer (Edit button style)
        DefaultTableCellRenderer actionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setForeground(new Color(230, 126, 34));
                lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return lbl;
            }
        };
        historyTable.getColumnModel().getColumn(8).setCellRenderer(actionRenderer);

        // Column widths
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Bill ID
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(160); // Customer
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(160); // Date
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Total
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Paid
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(110); // Pending
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Total Pending (Customer)
        historyTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Status
        historyTable.getColumnModel().getColumn(8).setPreferredWidth(80);  // Action (Edit)
        if (historyTable.getColumnCount() > 9) {
            historyTable.removeColumn(historyTable.getColumnModel().getColumn(9)); // Hide DB_ID from view
        }

        // Mouse click listener: Click on Action column or Double-click on row to Edit
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = historyTable.rowAtPoint(e.getPoint());
                int col = historyTable.columnAtPoint(e.getPoint());
                if (row >= 0) {
                    if (col == 8 || e.getClickCount() == 2) {
                        int modelRow = historyTable.convertRowIndexToModel(row);
                        int billId = (int) tableModel.getValueAt(modelRow, 9);
                        triggerEditBill(billId);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 230)));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Summary Panel (Dashboard style)
        JPanel summaryPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel billsBox = createSummaryBox("Total Bills Generated", "0");
        lblTotalBills = (JLabel) billsBox.getClientProperty("valueLabel");
        summaryPanel.add(billsBox);

        JPanel revenueBox = createSummaryBox("Total Revenue", "\u20B90.00");
        lblTotalRevenue = (JLabel) revenueBox.getClientProperty("valueLabel");
        lblTotalRevenue.setForeground(new Color(40, 167, 69));
        summaryPanel.add(revenueBox);

        add(summaryPanel, BorderLayout.SOUTH);

        // Refresh customer list on panel shown
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                refreshCustomerNames();
                loadHistory();
            }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        refreshCustomerNames();
        loadHistory();
    }

    private void setupCustomerAutocomplete() {
        JTextField editor = (JTextField) cmbCustomerSearch.getEditor().getEditorComponent();
        editor.setFont(new Font("Nirmala UI", Font.PLAIN, 14));

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
                    code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE ||
                    code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
                    if (code == KeyEvent.VK_ENTER) {
                        applyCustomerSearchFilter();
                    }
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    Vector<String> items = new Vector<>();
                    boolean found = false;

                    if (text.trim().isEmpty()) {
                        for (String name : allCustomerNames) {
                            items.add(name);
                            if (items.size() >= 30) break;
                        }
                    } else {
                        String query = text.trim().toLowerCase();
                        for (String name : allCustomerNames) {
                            if (name.toLowerCase().contains(query)) {
                                items.add(name);
                                found = true;
                                if (items.size() >= 30) break;
                            }
                        }
                    }

                    if (cmbCustomerSearch.getModel().getSize() != items.size() || !items.isEmpty()) {
                        cmbCustomerSearch.setModel(new DefaultComboBoxModel<>(items));
                        cmbCustomerSearch.getEditor().setItem(text);
                    }

                    if (!text.trim().isEmpty() && found && editor.hasFocus() && cmbCustomerSearch.isShowing()) {
                        if (!cmbCustomerSearch.isPopupVisible()) {
                            try {
                                cmbCustomerSearch.showPopup();
                            } catch (Exception ignored) {}
                        }
                    } else {
                        cmbCustomerSearch.hidePopup();
                    }

                    // Live Filter the table as user types
                    applyCustomerSearchFilter();
                });
            }
        });

        cmbCustomerSearch.addActionListener(e -> {
            if ("comboBoxChanged".equals(e.getActionCommand())) {
                applyCustomerSearchFilter();
            }
        });
    }

    private void refreshCustomerNames() {
        try {
            allCustomerNames = billDAO.getAllCustomerNames();
            Vector<String> items = new Vector<>();
            for (String name : allCustomerNames) {
                items.add(name);
                if (items.size() >= 30) break;
            }
            String currentText = "";
            if (cmbCustomerSearch.getEditor() != null && cmbCustomerSearch.getEditor().getItem() != null) {
                currentText = cmbCustomerSearch.getEditor().getItem().toString();
            }
            cmbCustomerSearch.setModel(new DefaultComboBoxModel<>(items));
            cmbCustomerSearch.getEditor().setItem(currentText);
        } catch (Exception ignored) {}
    }

    private void applyCustomerSearchFilter() {
        JTextField editor = (JTextField) cmbCustomerSearch.getEditor().getEditorComponent();
        String query = editor.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            // Filter by Customer Name (column index 1)
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query), 1));
        }
        updateSummaryFromVisibleRows();
    }

    private void updateSummaryFromVisibleRows() {
        double revenue = 0;
        int count = historyTable.getRowCount();
        for (int i = 0; i < count; i++) {
            int modelRow = historyTable.convertRowIndexToModel(i);
            String totStr = (String) tableModel.getValueAt(modelRow, 3);
            if (totStr != null) {
                try {
                    String clean = totStr.replace("\u20B9", "").replace(",", "").trim();
                    revenue += Double.parseDouble(clean);
                } catch (Exception ignored) {}
            }
        }
        if (lblTotalRevenue != null) lblTotalRevenue.setText(String.format("\u20B9%.2f", revenue));
        if (lblTotalBills != null) {
            int allTimeTotal = billDAO.getTotalBillsCount();
            lblTotalBills.setText(String.valueOf(allTimeTotal));
        }
    }

    public interface OnEditBillListener {
        void onEditBill(int billId);
    }
    private OnEditBillListener editBillListener;

    public void setOnEditBillListener(OnEditBillListener listener) {
        this.editBillListener = listener;
    }

    private void triggerEditBill(int billId) {
        if (editBillListener != null) {
            editBillListener.onEditBill(billId);
        }
    }

    public interface OnPrintBillListener {
        void onPrintBill(int billId);
    }
    private OnPrintBillListener printBillListener;

    public void setOnPrintBillListener(OnPrintBillListener listener) {
        this.printBillListener = listener;
    }

    private void triggerPrintBill(int billId) {
        if (printBillListener != null) {
            printBillListener.onPrintBill(billId);
        }
    }

    private JPanel createSummaryBox(String title, String initialValue) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1, true),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        titleLabel.setForeground(new Color(100, 110, 120));
        box.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        box.add(valueLabel, BorderLayout.SOUTH);

        box.putClientProperty("valueLabel", valueLabel);
        return box;
    }

    public void loadHistory() {
        tableModel.setRowCount(0);
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();

        List<Bill> bills = billDAO.getBillsByDateRange(startDate, endDate);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        for (Bill b : bills) {
            int displayNo = b.getDailyBillNo() > 0 ? b.getDailyBillNo() : b.getId();
            double billPending = Math.max(0.0, b.getPendingAmount());
            double custPending = Math.max(0.0, b.getCustomerTotalPending());
            tableModel.addRow(new Object[]{
                    displayNo,
                    b.getCustomerName().isEmpty() ? "Customer" : b.getCustomerName(),
                    b.getBillDate() != null ? sdf.format(b.getBillDate()) : "",
                    String.format("\u20B9%.2f", b.getTotalAmount()),
                    String.format("\u20B9%.2f", b.getPaidAmount()),
                    String.format("\u20B9%.2f", billPending),
                    String.format("\u20B9%.2f", custPending),
                    b.getStatus() != null ? b.getStatus() : "Printed",
                    "Edit \u270E",
                    b.getId()
            });
        }

        applyCustomerSearchFilter();
    }
}
