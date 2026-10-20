package com.store.billing.ui;

import com.store.billing.dao.PurchaseDAO;
import com.store.billing.model.Purchase;
import com.store.billing.model.SupplierPayment;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PurchaseAccountsPanel extends JPanel {

    private PurchaseDAO purchaseDAO;
    private JTable purchaseTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private JSpinner startDateSpinner, endDateSpinner;
    private JComboBox<String> cmbFilterSupplier;
    private JTextField txtSearch;

    private JLabel lblTotalPurchases;
    private JLabel lblTotalPaid;
    private JLabel lblTotalPending;
    private JLabel lblSupplierPending;
    private JLabel lblSupplierPendingTitle;
    private JLabel lblSupplierPendingSub;
    private JLabel lblEntryStats;

    private List<Purchase> currentPurchasesList = new ArrayList<>();

    public PurchaseAccountsPanel() {
        purchaseDAO = new PurchaseDAO();
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(245, 247, 250));

        // 1. Top Section: Dashboard Summary + Filter Bar
        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);

        // Dashboard Summary Cards (4 Cards Grid)
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setOpaque(false);

        JPanel cardPurchases = createSummaryBox("Total Purchases", "மொத்த கொள்முதல்", "\u20B90.00", new Color(0, 123, 255));
        lblTotalPurchases = (JLabel) cardPurchases.getClientProperty("valLabel");
        summaryPanel.add(cardPurchases);

        JPanel cardPaid = createSummaryBox("Total Paid", "வழங்கிய தொகை", "\u20B90.00", new Color(40, 167, 69));
        lblTotalPaid = (JLabel) cardPaid.getClientProperty("valLabel");
        summaryPanel.add(cardPaid);

        JPanel cardPending = createSummaryBox("Total Pending", "மீதி பாக்கி", "\u20B90.00", new Color(220, 53, 69));
        lblTotalPending = (JLabel) cardPending.getClientProperty("valLabel");
        summaryPanel.add(cardPending);

        JPanel cardSupplierPending = createSummaryBox("Supplier Pending", "சப்ளையர் பாக்கி", "\u20B90.00", new Color(142, 68, 173));
        lblSupplierPending = (JLabel) cardSupplierPending.getClientProperty("valLabel");
        lblSupplierPendingTitle = (JLabel) cardSupplierPending.getClientProperty("titleLabel");
        lblSupplierPendingSub = (JLabel) cardSupplierPending.getClientProperty("subLabel");
        summaryPanel.add(cardSupplierPending);

        topSection.add(summaryPanel, BorderLayout.NORTH);

        // Filter Bar
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 13);
        Font fieldFont = new Font("Nirmala UI", Font.PLAIN, 14);

        // Date spinners
        Calendar calendar = Calendar.getInstance();
        Date initEnd = calendar.getTime();
        calendar.add(Calendar.MONTH, -1);
        Date initStart = calendar.getTime();

        JLabel lblFrom = new JLabel("From Date:");
        lblFrom.setFont(labelFont);
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(lblFrom, gbc);

        startDateSpinner = new JSpinner(new SpinnerDateModel(initStart, null, null, Calendar.DAY_OF_MONTH));
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd-MM-yyyy"));
        startDateSpinner.setFont(fieldFont);
        gbc.gridx = 1; gbc.gridy = 0;
        filterPanel.add(startDateSpinner, gbc);

        JLabel lblTo = new JLabel("To Date:");
        lblTo.setFont(labelFont);
        gbc.gridx = 2; gbc.gridy = 0;
        filterPanel.add(lblTo, gbc);

        endDateSpinner = new JSpinner(new SpinnerDateModel(initEnd, null, null, Calendar.DAY_OF_MONTH));
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "dd-MM-yyyy"));
        endDateSpinner.setFont(fieldFont);
        gbc.gridx = 3; gbc.gridy = 0;
        filterPanel.add(endDateSpinner, gbc);

        // Supplier combo
        JLabel lblSupp = new JLabel("Supplier:");
        lblSupp.setFont(labelFont);
        gbc.gridx = 4; gbc.gridy = 0;
        filterPanel.add(lblSupp, gbc);

        cmbFilterSupplier = new JComboBox<>();
        cmbFilterSupplier.setFont(fieldFont);
        cmbFilterSupplier.addItem("All Suppliers");
        cmbFilterSupplier.addActionListener(e -> applyFilters());
        gbc.gridx = 5; gbc.gridy = 0;
        gbc.weightx = 0.5;
        filterPanel.add(cmbFilterSupplier, gbc);

        // Search text
        JLabel lblSearch = new JLabel("Search (F2):");
        lblSearch.setFont(labelFont);
        gbc.gridx = 6; gbc.gridy = 0;
        gbc.weightx = 0;
        filterPanel.add(lblSearch, gbc);

        txtSearch = new JTextField(12);
        txtSearch.setFont(fieldFont);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });
        gbc.gridx = 7; gbc.gridy = 0;
        gbc.weightx = 0.5;
        filterPanel.add(txtSearch, gbc);

        // Action Buttons
        JButton btnSearch = new JButton("Filter");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearch.addActionListener(e -> loadPurchases());
        gbc.gridx = 8; gbc.gridy = 0;
        gbc.weightx = 0;
        filterPanel.add(btnSearch, gbc);

        JButton btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnReset.setFocusPainted(false);
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbFilterSupplier.setSelectedIndex(0);
            Calendar c = Calendar.getInstance();
            endDateSpinner.setValue(c.getTime());
            c.add(Calendar.MONTH, -1);
            startDateSpinner.setValue(c.getTime());
            loadPurchases();
        });
        gbc.gridx = 9; gbc.gridy = 0;
        filterPanel.add(btnReset, gbc);

        JButton btnNewPurchase = new JButton("+ New Purchase Entry (F3)");
        btnNewPurchase.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNewPurchase.setBackground(new Color(40, 167, 69));
        btnNewPurchase.setForeground(Color.WHITE);
        btnNewPurchase.setFocusPainted(false);
        btnNewPurchase.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNewPurchase.addActionListener(e -> openPurchaseDialog(null));
        gbc.gridx = 10; gbc.gridy = 0;
        filterPanel.add(btnNewPurchase, gbc);

        topSection.add(filterPanel, BorderLayout.CENTER);
        add(topSection, BorderLayout.NORTH);


        // 2. Center Section: Purchases Table
        JPanel tableContainer = new JPanel(new BorderLayout(0, 6));
        tableContainer.setOpaque(false);

        String[] columns = {
                "ID", "Purchase Date", "Supplier / Dealer Name",
                "Total (\u20B9)", "Pending (\u20B9)", "Paid (\u20B9)",
                "Mode", "Status", "+ Add Cash", "Action",
                "Mobile", "Location / Address", "Invoice No", "Items / Description"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        purchaseTable = new JTable(tableModel) {
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

        rowSorter = new TableRowSorter<>(tableModel);
        purchaseTable.setRowSorter(rowSorter);

        purchaseTable.setRowHeight(38);
        purchaseTable.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        purchaseTable.setShowGrid(false);
        purchaseTable.setShowHorizontalLines(true);
        purchaseTable.setGridColor(new Color(230, 235, 240));

        JTableHeader header = purchaseTable.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(new Color(235, 240, 245));
        header.setForeground(new Color(33, 47, 61));
        header.setReorderingAllowed(true);

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        purchaseTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);  // ID
        purchaseTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);  // Purchase Date
        purchaseTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);  // Mode
        purchaseTable.getColumnModel().getColumn(10).setCellRenderer(centerRenderer); // Mobile
        purchaseTable.getColumnModel().getColumn(12).setCellRenderer(centerRenderer); // Invoice No

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        purchaseTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);   // Total
        purchaseTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);   // Paid

        // Pending column renderer (bold red if pending > 0)
        purchaseTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.RIGHT);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                double pend = 0;
                try { pend = Double.parseDouble(value != null ? value.toString() : "0"); } catch (Exception ignore) {}
                if (pend > 0.009) {
                    lbl.setForeground(new Color(220, 53, 69));
                } else {
                    lbl.setForeground(new Color(40, 167, 69));
                }
                return lbl;
            }
        });

        // Status chip renderer (Col 7)
        purchaseTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
                String st = value != null ? value.toString() : "";
                if ("Paid".equalsIgnoreCase(st)) {
                    lbl.setForeground(new Color(40, 167, 69));
                } else if ("Partial".equalsIgnoreCase(st)) {
                    lbl.setForeground(new Color(230, 126, 34));
                } else {
                    lbl.setForeground(new Color(220, 53, 69));
                }
                return lbl;
            }
        });

        // "+ Add Cash" column renderer (Col 8)
        purchaseTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String val = value != null ? value.toString() : "";
                if (val.contains("Add Cash")) {
                    lbl.setForeground(new Color(40, 167, 69));
                    lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    lbl.setForeground(new Color(130, 140, 150));
                }
                return lbl;
            }
        });

        // Action column renderer (Col 9)
        purchaseTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
                lbl.setForeground(new Color(0, 123, 255));
                lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return lbl;
            }
        });

        // Column widths
        purchaseTable.getColumnModel().getColumn(0).setPreferredWidth(45);   // ID
        purchaseTable.getColumnModel().getColumn(1).setPreferredWidth(130);  // Date
        purchaseTable.getColumnModel().getColumn(2).setPreferredWidth(150);  // Supplier
        purchaseTable.getColumnModel().getColumn(3).setPreferredWidth(90);   // Total
        purchaseTable.getColumnModel().getColumn(4).setPreferredWidth(90);   // Pending
        purchaseTable.getColumnModel().getColumn(5).setPreferredWidth(90);   // Paid
        purchaseTable.getColumnModel().getColumn(6).setPreferredWidth(70);   // Mode
        purchaseTable.getColumnModel().getColumn(7).setPreferredWidth(70);   // Status
        purchaseTable.getColumnModel().getColumn(8).setPreferredWidth(105);  // + Add Cash
        purchaseTable.getColumnModel().getColumn(9).setPreferredWidth(85);   // Action
        purchaseTable.getColumnModel().getColumn(10).setPreferredWidth(100); // Mobile
        purchaseTable.getColumnModel().getColumn(11).setPreferredWidth(130); // Address
        purchaseTable.getColumnModel().getColumn(12).setPreferredWidth(85);  // Invoice
        purchaseTable.getColumnModel().getColumn(13).setPreferredWidth(150); // Items/Desc

        // Mouse click listener
        purchaseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = purchaseTable.rowAtPoint(e.getPoint());
                int viewCol = purchaseTable.columnAtPoint(e.getPoint());
                if (viewRow < 0) return;

                int modelRow = purchaseTable.convertRowIndexToModel(viewRow);
                int purchaseId = (int) tableModel.getValueAt(modelRow, 0);
                Purchase p = purchaseDAO.getPurchaseById(purchaseId);
                if (p == null) return;

                if (viewCol == 8) {
                    // "+ Add Cash" column clicked -> Open payment dialog directly to add cash
                    openPaymentDialog(p);
                } else if (viewCol == 9) {
                    // Action column clicked -> Show context menu
                    showActionMenu(e.getComponent(), e.getX(), e.getY(), purchaseId);
                } else if (e.getClickCount() == 2) {
                    // Double click -> Edit/View Purchase
                    openPurchaseDialog(p);
                }
            }
        });

        purchaseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSupplierPendingSummary();
            }
        });

        JScrollPane scrollPane = new JScrollPane(purchaseTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 230), 1));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // Table Bottom Status Bar
        JPanel tableBottom = new JPanel(new BorderLayout());
        tableBottom.setOpaque(false);
        lblEntryStats = new JLabel("Showing: 0 purchases");
        lblEntryStats.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblEntryStats.setForeground(new Color(100, 110, 120));
        tableBottom.add(lblEntryStats, BorderLayout.WEST);

        JLabel lblHelp = new JLabel("Shortcuts: [F2] Search | [F3] New | [Enter] Edit | [P] Pay | [Del] Delete");
        lblHelp.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblHelp.setForeground(new Color(120, 130, 140));
        tableBottom.add(lblHelp, BorderLayout.EAST);

        tableContainer.add(tableBottom, BorderLayout.SOUTH);

        add(tableContainer, BorderLayout.CENTER);

        loadPurchases();
        setupKeyboardShortcuts();
        
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { ((JSpinner.DefaultEditor)startDateSpinner.getEditor()).getTextField(), ((JSpinner.DefaultEditor)endDateSpinner.getEditor()).getTextField(), cmbFilterSupplier, txtSearch, btnSearch, btnReset, btnNewPurchase }
        });
    }

    private void setupKeyboardShortcuts() {
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                Runnable doFocus = () -> {
                    if (txtSearch != null) {
                        txtSearch.requestFocusInWindow();
                        txtSearch.selectAll();
                    }
                };
                SwingUtilities.invokeLater(doFocus);
                javax.swing.Timer timer = new javax.swing.Timer(150, e -> doFocus.run());
                timer.setRepeats(false);
                timer.start();
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusSearch");
        this.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtSearch != null) {
                    txtSearch.requestFocusInWindow();
                    txtSearch.selectAll();
                }
            }
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "newPurchase");
        this.getActionMap().put("newPurchase", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                openPurchaseDialog(null);
            }
        });

        purchaseTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editPurchase");
        purchaseTable.getActionMap().put("editPurchase", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int viewRow = purchaseTable.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = purchaseTable.convertRowIndexToModel(viewRow);
                int purchaseId = (int) tableModel.getValueAt(modelRow, 0);
                Purchase p = purchaseDAO.getPurchaseById(purchaseId);
                if (p != null) openPurchaseDialog(p);
            }
        });

        purchaseTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "payPurchase");
        purchaseTable.getActionMap().put("payPurchase", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int viewRow = purchaseTable.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = purchaseTable.convertRowIndexToModel(viewRow);
                int purchaseId = (int) tableModel.getValueAt(modelRow, 0);
                Purchase p = purchaseDAO.getPurchaseById(purchaseId);
                if (p != null) openPaymentDialog(p);
            }
        });

        purchaseTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deletePurchase");
        purchaseTable.getActionMap().put("deletePurchase", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int viewRow = purchaseTable.getSelectedRow();
                if (viewRow < 0) return;
                int modelRow = purchaseTable.convertRowIndexToModel(viewRow);
                int purchaseId = (int) tableModel.getValueAt(modelRow, 0);
                Purchase p = purchaseDAO.getPurchaseById(purchaseId);
                if (p != null) {
                    int confirm = JOptionPane.showConfirmDialog(PurchaseAccountsPanel.this,
                            "Are you sure you want to delete Purchase #" + p.getId() + "?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION && purchaseDAO.deletePurchase(p.getId())) {
                        loadPurchases();
                    }
                }
            }
        });
    }

    private JPanel createSummaryBox(String title, String subtitle, String initialValue, Color accentColor) {
        JPanel box = new JPanel(new BorderLayout(5, 4));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel topPnl = new JPanel(new BorderLayout(2, 2));
        topPnl.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(new Color(50, 60, 70));
        topPnl.add(titleLabel, BorderLayout.NORTH);

        JLabel subLabel = null;
        if (subtitle != null && !subtitle.isEmpty()) {
            subLabel = new JLabel(subtitle);
            subLabel.setFont(new Font("Nirmala UI", Font.PLAIN, 12));
            subLabel.setForeground(new Color(110, 120, 130));
            topPnl.add(subLabel, BorderLayout.SOUTH);
        }

        box.add(topPnl, BorderLayout.NORTH);

        JLabel valLabel = new JLabel(initialValue);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valLabel.setForeground(accentColor);
        box.add(valLabel, BorderLayout.SOUTH);

        box.putClientProperty("valLabel", valLabel);
        box.putClientProperty("titleLabel", titleLabel);
        box.putClientProperty("subLabel", subLabel);
        return box;
    }

    public void loadPurchases() {
        tableModel.setRowCount(0);
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();

        currentPurchasesList = purchaseDAO.getPurchasesByDateRange(startDate, endDate);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        double totalPurchasesSum = 0;
        double totalPaidSum = 0;
        double totalPendingSum = 0;

        for (Purchase p : currentPurchasesList) {
            String addCashText = p.getPendingAmount() > 0.009 ? "+ Add Cash" : "Paid \u2713";
            tableModel.addRow(new Object[]{
                    p.getId(),
                    sdf.format(p.getPurchaseDate()),
                    p.getSupplierName(),
                    String.format("%.2f", p.getTotalAmount()),
                    String.format("%.2f", p.getPendingAmount()),
                    String.format("%.2f", p.getPaidAmount()),
                    p.getPaymentMode(),
                    p.getStatus(),
                    addCashText,
                    "Manage \u25BE",
                    p.getSupplierMobile(),
                    p.getSupplierAddress(),
                    p.getInvoiceNumber(),
                    p.getDescription()
            });

            totalPurchasesSum += p.getTotalAmount();
            totalPaidSum += p.getPaidAmount();
            totalPendingSum += p.getPendingAmount();
        }

        lblTotalPurchases.setText(String.format("\u20B9%.2f", totalPurchasesSum));
        lblTotalPaid.setText(String.format("\u20B9%.2f", totalPaidSum));
        lblTotalPending.setText(String.format("\u20B9%.2f", totalPendingSum));

        // Refresh Supplier Dropdown
        refreshSupplierDropdown();
        applyFilters();
    }

    private void refreshSupplierDropdown() {
        String currentSel = cmbFilterSupplier.getSelectedItem() != null ? cmbFilterSupplier.getSelectedItem().toString() : "All Suppliers";
        cmbFilterSupplier.removeAllItems();
        cmbFilterSupplier.addItem("All Suppliers");
        List<String> suppliers = purchaseDAO.getAllSupplierNames();
        for (String s : suppliers) {
            cmbFilterSupplier.addItem(s);
        }
        if (currentSel != null && !currentSel.isEmpty()) {
            cmbFilterSupplier.setSelectedItem(currentSel);
        }
    }

    private void applyFilters() {
        String selectedSupplier = (String) cmbFilterSupplier.getSelectedItem();
        String search = txtSearch.getText().trim().toLowerCase();

        boolean filterSupplier = selectedSupplier != null && !selectedSupplier.isEmpty() && !"All Suppliers".equalsIgnoreCase(selectedSupplier);

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (filterSupplier) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    Object val = entry.getValue(2); // Supplier column
                    return val != null && selectedSupplier.equalsIgnoreCase(val.toString().trim());
                }
            });
        }

        if (!search.isEmpty()) {
            String[] tokens = search.split("\\s+");
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        Object val = entry.getValue(i);
                        if (val != null) sb.append(val.toString().toLowerCase()).append(" ");
                    }
                    String combined = sb.toString();
                    for (String t : tokens) {
                        if (!combined.contains(t)) return false;
                    }
                    return true;
                }
            });
        }

        if (filters.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.andFilter(filters));
        }

        int visible = purchaseTable.getRowCount();
        int total = currentPurchasesList.size();
        lblEntryStats.setText("Showing: " + visible + " / " + total + " purchases");

        updateSupplierPendingSummary();
    }

    private void updateSupplierPendingSummary() {
        String selectedSupplier = (String) cmbFilterSupplier.getSelectedItem();
        boolean isSpecificSupplier = selectedSupplier != null && !selectedSupplier.isEmpty() && !"All Suppliers".equalsIgnoreCase(selectedSupplier);

        if (isSpecificSupplier) {
            double suppPending = purchaseDAO.getTotalPendingAmountBySupplier(selectedSupplier);
            if (lblSupplierPendingTitle != null) {
                lblSupplierPendingTitle.setText(selectedSupplier + " Pending");
            }
            if (lblSupplierPendingSub != null) {
                lblSupplierPendingSub.setText("சப்ளையர் மொத்த பாக்கி");
            }
            if (lblSupplierPending != null) {
                lblSupplierPending.setText(String.format("\u20B9%.2f", suppPending));
            }
        } else {
            // If table has a selected row, show that row's supplier pending
            int viewRow = purchaseTable.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = purchaseTable.convertRowIndexToModel(viewRow);
                String rowSupp = (String) tableModel.getValueAt(modelRow, 2);
                if (rowSupp != null && !rowSupp.trim().isEmpty()) {
                    double suppPending = purchaseDAO.getTotalPendingAmountBySupplier(rowSupp);
                    if (lblSupplierPendingTitle != null) {
                        lblSupplierPendingTitle.setText(rowSupp + " Pending");
                    }
                    if (lblSupplierPendingSub != null) {
                        lblSupplierPendingSub.setText("தேர்ந்தெடுக்கப்பட்ட சப்ளையர்");
                    }
                    if (lblSupplierPending != null) {
                        lblSupplierPending.setText(String.format("\u20B9%.2f", suppPending));
                    }
                    return;
                }
            }

            double totalPendingAll = purchaseDAO.getTotalPendingAmountBySupplier(null);
            if (lblSupplierPendingTitle != null) {
                lblSupplierPendingTitle.setText("Supplier Pending");
            }
            if (lblSupplierPendingSub != null) {
                lblSupplierPendingSub.setText("அனைத்து சப்ளையர் பாக்கி");
            }
            if (lblSupplierPending != null) {
                lblSupplierPending.setText(String.format("\u20B9%.2f", totalPendingAll));
            }
        }
    }

    private void showActionMenu(Component invoker, int x, int y, int purchaseId) {
        Purchase p = purchaseDAO.getPurchaseById(purchaseId);
        if (p == null) return;

        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemPay = new JMenuItem("Record Payment (பணம் செலுத்துதல்)");
        itemPay.setFont(new Font("Nirmala UI", Font.BOLD, 13));
        itemPay.setForeground(new Color(40, 167, 69));
        itemPay.addActionListener(e -> openPaymentDialog(p));
        menu.add(itemPay);

        JMenuItem itemHistory = new JMenuItem("Payment History (செலுத்திய விவரம்)");
        itemHistory.setFont(new Font("Nirmala UI", Font.PLAIN, 13));
        itemHistory.addActionListener(e -> openPaymentHistoryDialog(p));
        menu.add(itemHistory);

        menu.addSeparator();

        JMenuItem itemEdit = new JMenuItem("Edit Purchase (திருத்துதல்)");
        itemEdit.setFont(new Font("Nirmala UI", Font.PLAIN, 13));
        itemEdit.addActionListener(e -> openPurchaseDialog(p));
        menu.add(itemEdit);

        JMenuItem itemDelete = new JMenuItem("Delete Purchase (நீக்குதல்)");
        itemDelete.setFont(new Font("Nirmala UI", Font.PLAIN, 13));
        itemDelete.setForeground(new Color(220, 53, 69));
        itemDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(PurchaseAccountsPanel.this,
                    "Are you sure you want to delete Purchase #" + p.getId() + " from '" + p.getSupplierName() + "'?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                if (purchaseDAO.deletePurchase(p.getId())) {
                    JOptionPane.showMessageDialog(PurchaseAccountsPanel.this, "Purchase deleted successfully.");
                    loadPurchases();
                } else {
                    JOptionPane.showMessageDialog(PurchaseAccountsPanel.this, "Failed to delete purchase.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        menu.add(itemDelete);

        menu.show(invoker, x, y);
    }

    private void openPurchaseDialog(Purchase existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Purchase #" + existing.getId() : "New Supplier Purchase Entry", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(620, 560);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        Font lblF = new Font("Segoe UI", Font.BOLD, 13);
        Font txtF = new Font("Nirmala UI", Font.PLAIN, 14);

        // Supplier Name (AutoComplete / Dropdown)
        g.gridx = 0; g.gridy = 0;
        formPanel.add(new JLabel("Supplier / Dealer Name *:"), g);

        JComboBox<String> cmbSupplierName = new JComboBox<>();
        cmbSupplierName.setEditable(true);
        cmbSupplierName.setFont(txtF);
        for (String s : purchaseDAO.getAllSupplierNames()) {
            cmbSupplierName.addItem(s);
        }
        if (isEdit) cmbSupplierName.setSelectedItem(existing.getSupplierName());
        g.gridx = 1; g.gridy = 0;
        g.gridwidth = 2;
        formPanel.add(cmbSupplierName, g);
        g.gridwidth = 1;

        // Mobile
        g.gridx = 0; g.gridy = 1;
        formPanel.add(new JLabel("Supplier Mobile:"), g);
        JTextField txtMobile = new JTextField(isEdit ? existing.getSupplierMobile() : "", 15);
        txtMobile.setFont(txtF);
        g.gridx = 1; g.gridy = 1;
        g.gridwidth = 2;
        formPanel.add(txtMobile, g);
        g.gridwidth = 1;

        // Address / Location
        g.gridx = 0; g.gridy = 2;
        formPanel.add(new JLabel("Location / Address:"), g);
        JTextField txtAddress = new JTextField(isEdit ? existing.getSupplierAddress() : "", 15);
        txtAddress.setFont(txtF);
        g.gridx = 1; g.gridy = 2;
        g.gridwidth = 2;
        formPanel.add(txtAddress, g);
        g.gridwidth = 1;

        // Invoice Number
        g.gridx = 0; g.gridy = 3;
        formPanel.add(new JLabel("Invoice / Bill No:"), g);
        JTextField txtInvoice = new JTextField(isEdit ? existing.getInvoiceNumber() : "", 15);
        txtInvoice.setFont(txtF);
        g.gridx = 1; g.gridy = 3;
        g.gridwidth = 2;
        formPanel.add(txtInvoice, g);
        g.gridwidth = 1;

        // Purchase Date
        g.gridx = 0; g.gridy = 4;
        formPanel.add(new JLabel("Purchase Date:"), g);
        Date d = isEdit ? existing.getPurchaseDate() : new Date();
        JSpinner dateSp = new JSpinner(new SpinnerDateModel(d, null, null, Calendar.DAY_OF_MONTH));
        dateSp.setEditor(new JSpinner.DateEditor(dateSp, "dd-MM-yyyy HH:mm"));
        dateSp.setFont(txtF);
        g.gridx = 1; g.gridy = 4;
        g.gridwidth = 2;
        formPanel.add(dateSp, g);
        g.gridwidth = 1;

        // Total Amount (₹)
        g.gridx = 0; g.gridy = 5;
        formPanel.add(new JLabel("Total Bill Amount (\u20B9) *:"), g);
        JTextField txtTotal = new JTextField(isEdit ? String.format("%.2f", existing.getTotalAmount()) : "", 15);
        txtTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.gridx = 1; g.gridy = 5;
        g.gridwidth = 2;
        formPanel.add(txtTotal, g);
        g.gridwidth = 1;

        // Paid Amount (₹)
        g.gridx = 0; g.gridy = 6;
        formPanel.add(new JLabel("Paid Amount (\u20B9):"), g);
        JTextField txtPaid = new JTextField(isEdit ? String.format("%.2f", existing.getPaidAmount()) : "", 15);
        txtPaid.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.gridx = 1; g.gridy = 6;
        g.gridwidth = 2;
        formPanel.add(txtPaid, g);
        g.gridwidth = 1;

        // Pending Amount (Auto Calculated)
        g.gridx = 0; g.gridy = 7;
        formPanel.add(new JLabel("Pending Balance (\u20B9):"), g);
        JLabel lblPendingCalc = new JLabel(isEdit ? String.format("\u20B9%.2f", existing.getPendingAmount()) : "\u20B90.00");
        lblPendingCalc.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPendingCalc.setForeground(new Color(220, 53, 69));
        g.gridx = 1; g.gridy = 7;
        g.gridwidth = 2;
        formPanel.add(lblPendingCalc, g);
        g.gridwidth = 1;

        // Auto calculate pending balance on typing total or paid
        Runnable calcPending = () -> {
            double tot = 0, pd = 0;
            try { tot = Double.parseDouble(txtTotal.getText().trim()); } catch (Exception ignored) {}
            try { pd = Double.parseDouble(txtPaid.getText().trim()); } catch (Exception ignored) {}
            double pnd = Math.max(0, tot - pd);
            lblPendingCalc.setText(String.format("\u20B9%.2f", pnd));
        };

        txtTotal.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { calcPending.run(); } });
        txtPaid.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { calcPending.run(); } });

        // Payment Mode
        g.gridx = 0; g.gridy = 8;
        formPanel.add(new JLabel("Payment Mode:"), g);
        JComboBox<String> cmbMode = new JComboBox<>(new String[]{"Cash", "GPay / UPI", "Bank Transfer", "Cheque", "Credit (Unpaid)"});
        cmbMode.setFont(txtF);
        if (isEdit) cmbMode.setSelectedItem(existing.getPaymentMode());
        g.gridx = 1; g.gridy = 8;
        g.gridwidth = 2;
        formPanel.add(cmbMode, g);
        g.gridwidth = 1;

        // Items / Description
        g.gridx = 0; g.gridy = 9;
        formPanel.add(new JLabel("Items / Notes:"), g);
        JTextField txtDesc = new JTextField(isEdit ? existing.getDescription() : "", 15);
        txtDesc.setFont(txtF);
        g.gridx = 1; g.gridy = 9;
        g.gridwidth = 2;
        formPanel.add(txtDesc, g);
        g.gridwidth = 1;

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnSave = new JButton(isEdit ? "Update Purchase" : "Save Purchase");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSave.addActionListener(e -> {
            Object suppObj = cmbSupplierName.getSelectedItem();
            String suppName = suppObj != null ? suppObj.toString().trim() : "";
            if (suppName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter Supplier / Dealer Name.", "Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String totStr = txtTotal.getText().trim();
            if (totStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter Total Bill Amount.", "Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double totalAmt = 0;
            try {
                totalAmt = Double.parseDouble(totStr);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid Total Amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double paidAmt = 0;
            String paidStr = txtPaid.getText().trim();
            if (!paidStr.isEmpty()) {
                try { paidAmt = Double.parseDouble(paidStr); } catch (Exception ignored) {}
            }
            double pendingAmt = Math.max(0, totalAmt - paidAmt);

            Purchase p = isEdit ? existing : new Purchase();
            p.setSupplierName(suppName);
            p.setSupplierMobile(txtMobile.getText().trim());
            p.setSupplierAddress(txtAddress.getText().trim());
            p.setInvoiceNumber(txtInvoice.getText().trim());
            p.setPurchaseDate((Date) dateSp.getValue());
            p.setTotalAmount(totalAmt);
            p.setPaidAmount(paidAmt);
            p.setPendingAmount(pendingAmt);
            p.setPaymentMode(cmbMode.getSelectedItem().toString());
            p.setDescription(txtDesc.getText().trim());

            boolean success = isEdit ? purchaseDAO.updatePurchase(p) : purchaseDAO.addPurchase(p);
            if (success) {
                JOptionPane.showMessageDialog(dialog, "Purchase saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadPurchases();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to save purchase into database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnCancel.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { cmbSupplierName.getEditor().getEditorComponent() },
            { txtMobile },
            { txtAddress },
            { txtInvoice },
            { ((JSpinner.DefaultEditor)dateSp.getEditor()).getTextField() },
            { txtTotal },
            { txtPaid },
            { cmbMode },
            { txtDesc },
            { btnSave, btnCancel }
        });

        dialog.setVisible(true);
    }

    private void openPaymentDialog(Purchase p) {
        if (p.getPendingAmount() <= 0.009) {
            JOptionPane.showMessageDialog(this, "This purchase is already fully paid (No pending balance).", "Fully Paid", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Record Supplier Payment - " + p.getSupplierName(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(480, 360);
        dialog.setLocationRelativeTo(this);

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        Font lblF = new Font("Segoe UI", Font.BOLD, 13);
        Font txtF = new Font("Nirmala UI", Font.PLAIN, 14);

        g.gridx = 0; g.gridy = 0;
        pnl.add(new JLabel("Supplier:"), g);
        JLabel lblSupp = new JLabel(p.getSupplierName());
        lblSupp.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g.gridx = 1; g.gridy = 0;
        pnl.add(lblSupp, g);

        g.gridx = 0; g.gridy = 1;
        pnl.add(new JLabel("Invoice No:"), g);
        JLabel lblInv = new JLabel(p.getInvoiceNumber().isEmpty() ? "Purchase #" + p.getId() : p.getInvoiceNumber());
        lblInv.setFont(txtF);
        g.gridx = 1; g.gridy = 1;
        pnl.add(lblInv, g);

        g.gridx = 0; g.gridy = 2;
        pnl.add(new JLabel("Current Pending:"), g);
        JLabel lblPending = new JLabel(String.format("\u20B9%.2f", p.getPendingAmount()));
        lblPending.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPending.setForeground(new Color(220, 53, 69));
        g.gridx = 1; g.gridy = 2;
        pnl.add(lblPending, g);

        g.gridx = 0; g.gridy = 3;
        pnl.add(new JLabel("Amount Paying Now (\u20B9) *:"), g);
        JTextField txtPayNow = new JTextField(String.format("%.2f", p.getPendingAmount()), 12);
        txtPayNow.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g.gridx = 1; g.gridy = 3;
        pnl.add(txtPayNow, g);

        g.gridx = 0; g.gridy = 4;
        pnl.add(new JLabel("Payment Date:"), g);
        JSpinner payDateSp = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
        payDateSp.setEditor(new JSpinner.DateEditor(payDateSp, "dd-MM-yyyy HH:mm"));
        payDateSp.setFont(txtF);
        g.gridx = 1; g.gridy = 4;
        pnl.add(payDateSp, g);

        g.gridx = 0; g.gridy = 5;
        pnl.add(new JLabel("Payment Mode:"), g);
        JComboBox<String> cmbPayMode = new JComboBox<>(new String[]{"Cash", "GPay / UPI", "Bank Transfer", "Cheque"});
        cmbPayMode.setFont(txtF);
        g.gridx = 1; g.gridy = 5;
        pnl.add(cmbPayMode, g);

        g.gridx = 0; g.gridy = 6;
        pnl.add(new JLabel("Notes / Remarks:"), g);
        JTextField txtPayNotes = new JTextField("Daily cash installment", 12);
        txtPayNotes.setFont(txtF);
        g.gridx = 1; g.gridy = 6;
        pnl.add(txtPayNotes, g);

        dialog.add(pnl, BorderLayout.CENTER);

        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSubmit = new JButton("Save Payment");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.setBackground(new Color(40, 167, 69));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnSubmit.addActionListener(e -> {
            String payStr = txtPayNow.getText().trim();
            if (payStr.isEmpty()) return;
            double payAmt = 0;
            try {
                payAmt = Double.parseDouble(payStr);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid payment amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (payAmt <= 0) {
                JOptionPane.showMessageDialog(dialog, "Payment amount must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Date payDate = (Date) payDateSp.getValue();
            boolean ok = purchaseDAO.addSupplierPayment(p.getId(), p.getSupplierName(), payAmt,
                    cmbPayMode.getSelectedItem().toString(), txtPayNotes.getText().trim(), payDate);
            if (ok) {
                JOptionPane.showMessageDialog(dialog, "Payment of \u20B9" + String.format("%.2f", payAmt) + " recorded successfully!");
                dialog.dispose();
                loadPurchases();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to record payment in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnClose = new JButton("Cancel");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnClose.addActionListener(e -> dialog.dispose());

        btnP.add(btnSubmit);
        btnP.add(btnClose);
        dialog.add(btnP, BorderLayout.SOUTH);

        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { txtPayNow },
            { ((JSpinner.DefaultEditor)payDateSp.getEditor()).getTextField() },
            { cmbPayMode },
            { txtPayNotes },
            { btnSubmit, btnClose }
        });

        dialog.setVisible(true);
    }

    private void openPaymentHistoryDialog(Purchase p) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment History - " + p.getSupplierName(), true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(550, 400);
        dialog.setLocationRelativeTo(this);

        List<SupplierPayment> payments = purchaseDAO.getPaymentsForPurchase(p.getId());

        String[] cols = {"Payment Date", "Amount Paid (\u20B9)", "Mode", "Notes"};
        DefaultTableModel mod = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        for (SupplierPayment sp : payments) {
            mod.addRow(new Object[]{
                    sp.getPaymentDate() != null ? sdf.format(sp.getPaymentDate()) : "",
                    String.format("\u20B9%.2f", sp.getAmountPaid()),
                    sp.getPaymentMode(),
                    sp.getNotes()
            });
        }

        JTable table = new JTable(mod);
        table.setRowHeight(32);
        table.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        top.add(new JLabel("Purchase Total: \u20B9" + String.format("%.2f", p.getTotalAmount())));
        top.add(new JLabel("Paid: \u20B9" + String.format("%.2f", p.getPaidAmount())));
        JLabel lblBal = new JLabel("Balance: \u20B9" + String.format("%.2f", p.getPendingAmount()));
        lblBal.setForeground(new Color(220, 53, 69));
        lblBal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        top.add(lblBal);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        btm.add(btnClose);
        dialog.add(btm, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
}
