package com.store.billing.ui;

import com.store.billing.dao.CustomerDAO;
import com.store.billing.model.Bill;
import com.store.billing.model.Customer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CustomerPanel extends JPanel {
    private CustomerDAO customerDAO;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    private JTextField txtSearch;
    private JTextField txtId, txtName, txtTamilName, txtMobile, txtPending;
    private JTextArea txtAddress;
    private JLabel lblFormMode;

    private JButton btnAdd, btnUpdate, btnAddCash, btnDelete, btnHistory, btnClear;
    private int selectedCustomerId = -1;

    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FIELD_FONT = new Font("Nirmala UI", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);

    public CustomerPanel() {
        this.customerDAO = new CustomerDAO();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new Color(245, 247, 250));

        // 1. Top Section: Customer Entry Form & Search
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // Form Panel
        JPanel formPanel = createCustomerFormPanel();
        topContainer.add(formPanel, BorderLayout.CENTER);

        // Search Bar Panel
        JPanel searchPanel = createSearchPanel();
        topContainer.add(searchPanel, BorderLayout.SOUTH);

        add(topContainer, BorderLayout.NORTH);

        // 2. Center Section: Customers Table
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Action Listeners
        btnAdd.addActionListener(e -> addCustomer());
        btnUpdate.addActionListener(e -> updateCustomer());
        btnAddCash.addActionListener(e -> openAddCashDialog());
        btnDelete.addActionListener(e -> deleteCustomer());
        btnHistory.addActionListener(e -> openSelectedCustomerHistory());
        btnClear.addActionListener(e -> clearForm());

        // Keyboard & Focus setup
        setupEnterKeyNavigation();
        setupKeyboardShortcuts();

        // Auto focus when panel opens
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                loadCustomers();
                SwingUtilities.invokeLater(() -> {
                    if (txtSearch != null) txtSearch.requestFocusInWindow();
                });
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        loadCustomers();
    }

    private void setupKeyboardShortcuts() {
        // F2 -> Edit Form (Customer Name)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "focusEdit");
        this.getActionMap().put("focusEdit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtName != null) {
                    txtName.requestFocusInWindow();
                    txtName.selectAll();
                }
            }
        });

        // F3 -> Search Box
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "focusSearch");
        this.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtSearch != null) {
                    txtSearch.requestFocusInWindow();
                    txtSearch.selectAll();
                }
            }
        });

        // F5 -> Clear Form
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "clearForm");
        this.getActionMap().put("clearForm", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnClear != null) btnClear.doClick();
            }
        });

        // Mnemonic shortcuts for buttons
        btnAdd.setMnemonic(java.awt.event.KeyEvent.VK_A); // Alt + A
        btnUpdate.setMnemonic(java.awt.event.KeyEvent.VK_U); // Alt + U
        btnDelete.setMnemonic(java.awt.event.KeyEvent.VK_D); // Alt + D
        btnHistory.setMnemonic(java.awt.event.KeyEvent.VK_H); // Alt + H
        btnAddCash.setMnemonic(java.awt.event.KeyEvent.VK_R); // Alt + R (Receive Cash)
        btnClear.setMnemonic(java.awt.event.KeyEvent.VK_C); // Alt + C (Clear)
    }

    private JPanel createCustomerFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Header Status Label
        lblFormMode = new JLabel("Mode: Ready to Add New Customer");
        lblFormMode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFormMode.setForeground(new Color(40, 167, 69));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 4;
        formPanel.add(lblFormMode, gbc);
        gbc.gridwidth = 1;

        // ROW 1: Customer ID & Customer Name (English)
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = 0.0;
        JLabel lblId = new JLabel("Customer ID *:");
        lblId.setFont(LABEL_FONT);
        formPanel.add(lblId, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        txtId = new JTextField();
        txtId.setFont(FIELD_FONT);
        txtId.setToolTipText("Customer ID (Must be unique. Auto-generated or custom editable)");
        formPanel.add(txtId, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0;
        JLabel lblName = new JLabel("Customer Name (English) *:");
        lblName.setFont(LABEL_FONT);
        formPanel.add(lblName, gbc);

        gbc.gridx = 3; gbc.weightx = 0.7;
        txtName = new JTextField();
        txtName.setFont(FIELD_FONT);
        formPanel.add(txtName, gbc);

        // ROW 2: Customer Name (Tamil) & Total Pending + Receive Cash
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = 0.0;
        JLabel lblTamilName = new JLabel("Customer Name (Tamil):");
        lblTamilName.setFont(LABEL_FONT);
        formPanel.add(lblTamilName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        txtTamilName = new JTextField();
        txtTamilName.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        txtTamilName.setToolTipText("Customer Name in Tamil (e.g. \u0BB8\u0BCD\u0BB0\u0BC0)");
        formPanel.add(txtTamilName, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0;
        JLabel lblPend = new JLabel("Total Pending (\u20B9):");
        lblPend.setFont(LABEL_FONT);
        formPanel.add(lblPend, gbc);

        gbc.gridx = 3; gbc.weightx = 0.7;
        JPanel pendingPanel = new JPanel(new BorderLayout(8, 0));
        pendingPanel.setOpaque(false);
        txtPending = new JTextField("0.00");
        txtPending.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtPending.setForeground(new Color(220, 53, 69));
        txtPending.setToolTipText("Customer's overall pending balance history (Editable)");
        pendingPanel.add(txtPending, BorderLayout.CENTER);

        btnAddCash = new JButton("Receive / Add Cash");
        btnAddCash.setFont(LABEL_FONT);
        btnAddCash.setBackground(new Color(23, 162, 184));
        btnAddCash.setForeground(Color.WHITE);
        btnAddCash.setPreferredSize(new Dimension(165, 32));
        btnAddCash.setFocusPainted(false);
        btnAddCash.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddCash.setMnemonic(KeyEvent.VK_R);
        btnAddCash.setToolTipText("Shortcut: Alt + R");
        pendingPanel.add(btnAddCash, BorderLayout.EAST);
        formPanel.add(pendingPanel, gbc);

        // ROW 3: Mobile Number & Address
        gbc.gridy = 3;
        gbc.gridx = 0; gbc.weightx = 0.0;
        JLabel lblMob = new JLabel("Mobile Number:");
        lblMob.setFont(LABEL_FONT);
        formPanel.add(lblMob, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        txtMobile = new JTextField();
        txtMobile.setFont(FIELD_FONT);
        formPanel.add(txtMobile, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0;
        JLabel lblAddr = new JLabel("Address / Location:");
        lblAddr.setFont(LABEL_FONT);
        formPanel.add(lblAddr, gbc);

        gbc.gridx = 3; gbc.weightx = 0.7;
        txtAddress = new JTextArea(2, 20);
        txtAddress.setFont(FIELD_FONT);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        JScrollPane scrollAddr = new JScrollPane(txtAddress);
        scrollAddr.setPreferredSize(new Dimension(200, 48));
        formPanel.add(scrollAddr, gbc);

        // ROW 4: Action Buttons
        gbc.gridy = 4;
        gbc.gridx = 0; gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.EAST;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.setOpaque(false);

        btnClear = new JButton("Clear Form");
        btnClear.setFont(LABEL_FONT);
        btnClear.setPreferredSize(new Dimension(110, 32));
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(btnClear);

        btnAdd = new JButton("Add Customer");
        btnAdd.setFont(LABEL_FONT);
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setPreferredSize(new Dimension(135, 32));
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(btnAdd);

        btnUpdate = new JButton("Update Customer");
        btnUpdate.setFont(LABEL_FONT);
        btnUpdate.setBackground(new Color(0, 123, 255));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setPreferredSize(new Dimension(150, 32));
        btnUpdate.setFocusPainted(false);
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(btnUpdate);

        btnHistory = new JButton("Billing History");
        btnHistory.setFont(LABEL_FONT);
        btnHistory.setBackground(new Color(142, 68, 173));
        btnHistory.setForeground(Color.WHITE);
        btnHistory.setPreferredSize(new Dimension(140, 32));
        btnHistory.setFocusPainted(false);
        btnHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(btnHistory);

        btnDelete = new JButton("Delete Customer");
        btnDelete.setFont(LABEL_FONT);
        btnDelete.setBackground(new Color(220, 53, 69));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setPreferredSize(new Dimension(145, 32));
        btnDelete.setFocusPainted(false);
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        buttonPanel.add(btnDelete);

        formPanel.add(buttonPanel, gbc);

        return formPanel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblSearch = new JLabel("Search Customer (Name / ID / Mobile / Address):");
        lblSearch.setFont(LABEL_FONT);
        panel.add(lblSearch, BorderLayout.WEST);

        txtSearch = new JTextField();
        txtSearch.setFont(FIELD_FONT);
        setupCustomerSearchAutoComplete();
        panel.add(txtSearch, BorderLayout.CENTER);

        return panel;
    }

    private void setupCustomerSearchAutoComplete() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setFocusable(false);
        popupMenu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }

            private void updateSuggestions() {
                SwingUtilities.invokeLater(() -> {
                    String text = txtSearch.getText().trim();
                    popupMenu.removeAll();
                    if (text.isEmpty()) {
                        popupMenu.setVisible(false);
                        filterTable();
                        return;
                    }

                    filterTable();

                    List<Customer> allCust = customerDAO.getAllCustomers();
                    List<Customer> matched = com.store.billing.util.CustomerSearchUtil.filter(allCust, text);
                    int count = 0;
                    for (Customer c : matched) {
                        String display = com.store.billing.util.CustomerSearchUtil.getDisplayLabel(c);
                        JMenuItem item = new JMenuItem(display);
                        item.setFont(FIELD_FONT);
                        item.setBackground(Color.WHITE);
                        item.setOpaque(true);
                        item.addActionListener(e -> {
                            txtSearch.setText(c.getName());
                            popupMenu.setVisible(false);
                            for (int i = 0; i < tableModel.getRowCount(); i++) {
                                int cid = (int) tableModel.getValueAt(i, 0);
                                if (cid == c.getId()) {
                                    populateFormFromModelRow(i);
                                    try {
                                        int viewRow = table.convertRowIndexToView(i);
                                        if (viewRow >= 0) {
                                            table.setRowSelectionInterval(viewRow, viewRow);
                                        }
                                    } catch(Exception ignored) {}
                                    break;
                                }
                            }
                        });
                        popupMenu.add(item);
                        count++;
                        if (count >= 10) break;
                    }

                    if (count > 0 && txtSearch.hasFocus()) {
                        if (!popupMenu.isVisible()) {
                            popupMenu.show(txtSearch, 0, txtSearch.getHeight());
                        } else {
                            popupMenu.pack();
                        }
                        txtSearch.requestFocusInWindow();
                    } else {
                        popupMenu.setVisible(false);
                    }
                });
            }
        });

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN && popupMenu.isVisible()) {
                    MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{popupMenu, (JMenuItem)popupMenu.getComponent(0)});
                }
            }
        });
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);

        String[] columns = {"Customer ID", "Customer Name (English)", "Customer Name (Tamil)", "Total Pending (\u20B9)", "Billing History", "Mobile Number", "Address / Location"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    c.setBackground(new Color(225, 238, 255)); // Soft modern light blue highlight
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                }
                return c;
            }
        };
        table.setRowHeight(34);
        table.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(HEADER_FONT);
        table.getTableHeader().setBackground(new Color(235, 240, 245));
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(230, 235, 240));
        table.setSelectionBackground(new Color(225, 238, 255));
        table.setSelectionForeground(Color.BLACK);

        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        table.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Mobile

        // Pending Amount Renderer (bold red if > 0, green if 0, clear on selection)
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(JLabel.RIGHT);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
                double amt = 0;
                try { amt = Double.parseDouble(val != null ? val.toString() : "0"); } catch (Exception ignored) {}
                if (amt > 0.009) {
                    lbl.setForeground(new Color(200, 35, 51)); // Deep clear red
                } else {
                    lbl.setForeground(new Color(30, 140, 55)); // Deep clear green
                }
                if (isSelected) {
                    lbl.setBackground(new Color(225, 238, 255));
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                }
                return lbl;
            }
        });

        // Billing History Action Column Renderer
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setForeground(new Color(0, 102, 204));
                if (isSelected) {
                    lbl.setBackground(new Color(225, 238, 255));
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 252));
                }
                lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return lbl;
            }
        });

        // Column Widths
        table.getColumnModel().getColumn(0).setPreferredWidth(85);   // Customer ID
        table.getColumnModel().getColumn(1).setPreferredWidth(170);  // Name English
        table.getColumnModel().getColumn(2).setPreferredWidth(170);  // Name Tamil
        table.getColumnModel().getColumn(3).setPreferredWidth(130);  // Pending Amount
        table.getColumnModel().getColumn(4).setPreferredWidth(120);  // Action
        table.getColumnModel().getColumn(5).setPreferredWidth(120);  // Mobile
        table.getColumnModel().getColumn(6).setPreferredWidth(180);  // Address

        // Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int viewRow = table.getSelectedRow();
                int modelRow = table.convertRowIndexToModel(viewRow);
                populateFormFromModelRow(modelRow);
            }
        });

        // Mouse Click Listener
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0) return;

                int modelRow = table.convertRowIndexToModel(viewRow);
                int custId = (int) tableModel.getValueAt(modelRow, 0);
                String custName = (String) tableModel.getValueAt(modelRow, 1);

                if (viewCol == 4) {
                    // "View Bills ➔" column clicked
                    openCustomerBillingHistory(custId, custName);
                } else if (e.getClickCount() == 2) {
                    // Double click -> View Billing History
                    openCustomerBillingHistory(custId, custName);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 230), 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void filterTable() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    try {
                        int id = Integer.parseInt(entry.getStringValue(0));
                        String name = entry.getStringValue(1);
                        String tamilName = entry.getStringValue(2);
                        String mobile = entry.getStringValue(5);
                        String address = entry.getStringValue(6);
                        Customer temp = new Customer(id, name, tamilName, mobile, address, 0);
                        return com.store.billing.util.CustomerSearchUtil.matches(temp, query);
                    } catch (Exception e) {
                        return true;
                    }
                }
            });
        }
    }

    private void populateFormFromModelRow(int modelRow) {
        selectedCustomerId = (int) tableModel.getValueAt(modelRow, 0);
        txtId.setText(String.valueOf(selectedCustomerId));
        txtName.setText((String) tableModel.getValueAt(modelRow, 1));
        Object tam = tableModel.getValueAt(modelRow, 2);
        txtTamilName.setText(tam != null ? tam.toString() : "");
        
        Object pend = tableModel.getValueAt(modelRow, 3);
        double pVal = 0.0;
        if (pend != null) {
            try {
                pVal = Math.max(0.0, Double.parseDouble(pend.toString().replace("\u20B9", "").replace(",", "").trim()));
            } catch (Exception ignored) {}
        }
        txtPending.setText(String.format("%.2f", pVal));
        
        Object mob = tableModel.getValueAt(modelRow, 5);
        txtMobile.setText(mob != null ? mob.toString() : "");
        
        Object addr = tableModel.getValueAt(modelRow, 6);
        txtAddress.setText(addr != null ? addr.toString() : "");

        lblFormMode.setText("Mode: Editing Customer #" + selectedCustomerId + " - " + txtName.getText());
        lblFormMode.setForeground(new Color(0, 123, 255));
    }

    private void setupEnterKeyNavigation() {
        txtId.addActionListener(e -> txtName.requestFocusInWindow());
        txtName.addActionListener(e -> txtTamilName.requestFocusInWindow());
        txtTamilName.addActionListener(e -> txtMobile.requestFocusInWindow());
        txtMobile.addActionListener(e -> txtAddress.requestFocusInWindow());

        txtAddress.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                txtName.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        txtAddress.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                txtName.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));

        txtAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        txtAddress.append("\n");
                    } else {
                        e.consume();
                        txtPending.requestFocusInWindow();
                    }
                }
            }
        });

        txtPending.addActionListener(e -> {
            if (selectedCustomerId > 0) {
                btnUpdate.requestFocusInWindow();
            } else {
                btnAdd.requestFocusInWindow();
            }
        });

        // Arrow Key 2D Navigation (Left, Right, Up, Down)
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
                { txtId, txtId, txtName, txtName, txtName },
                { txtTamilName, txtTamilName, txtPending, btnAddCash, btnAddCash },
                { txtMobile, txtMobile, txtAddress, txtAddress, txtAddress },
                { btnClear, btnAdd, btnUpdate, btnHistory, btnDelete },
                { txtSearch, txtSearch, txtSearch, txtSearch, txtSearch }
        });
    }

    public void loadCustomers() {
        tableModel.setRowCount(0);
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            tableModel.addRow(new Object[]{
                    c.getId(),
                    c.getName(),
                    c.getTamilName() != null ? c.getTamilName() : "",
                    String.format("%.2f", Math.max(0.0, c.getPendingAmount())),
                    "View Bills",
                    c.getMobile() != null ? c.getMobile() : "",
                    c.getAddress() != null ? c.getAddress() : ""
            });
        }
        if (selectedCustomerId == -1) {
            txtId.setText(String.valueOf(customerDAO.getNextCustomerId()));
        }
    }

    private void addCustomer() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer Name is required!", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }

        String tamilName = txtTamilName.getText().trim();

        int customId = 0;
        String idStr = txtId.getText().trim();
        if (!idStr.isEmpty()) {
            try {
                customId = Integer.parseInt(idStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Customer ID must be a valid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                txtId.requestFocusInWindow();
                return;
            }
        }
        if (customId <= 0) {
            customId = customerDAO.getNextCustomerId();
        }

        // Validate uniqueness of Customer ID
        if (customerDAO.isCustomerIdExists(customId, -1)) {
            JOptionPane.showMessageDialog(this, "Customer ID #" + customId + " is already in use by another customer!\nPlease enter a unique Customer ID.", "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            txtId.requestFocusInWindow();
            txtId.selectAll();
            return;
        }

        double pendingAmt = 0.0;
        String pendStr = txtPending.getText().trim();
        if (!pendStr.isEmpty()) {
            try {
                pendingAmt = Math.max(0.0, Double.parseDouble(pendStr));
            } catch (NumberFormatException ignored) {}
        }

        String mobile = txtMobile.getText().trim();
        String address = txtAddress.getText().trim();

        Customer c = new Customer(customId, name, tamilName, mobile, address, pendingAmt);
        if (customerDAO.addCustomer(c)) {
            JOptionPane.showMessageDialog(this, "Customer '" + name + "' (ID: #" + customId + ") added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add customer. Please check database connection.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCustomer() {
        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table to update.", "No Customer Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }

        String tamilName = txtTamilName.getText().trim();

        int newId = selectedCustomerId;
        String idStr = txtId.getText().trim();
        if (!idStr.isEmpty()) {
            try {
                newId = Integer.parseInt(idStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Customer ID must be a valid number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                txtId.requestFocusInWindow();
                return;
            }
        }
        if (newId <= 0) newId = selectedCustomerId;

        // Check if new ID collides with another customer
        if (newId != selectedCustomerId && customerDAO.isCustomerIdExists(newId, selectedCustomerId)) {
            JOptionPane.showMessageDialog(this, "Customer ID #" + newId + " is already in use by another customer!\nPlease enter a unique Customer ID.", "Duplicate ID", JOptionPane.WARNING_MESSAGE);
            txtId.requestFocusInWindow();
            txtId.selectAll();
            return;
        }

        double pendingAmt = 0.0;
        String pendStr = txtPending.getText().trim();
        if (!pendStr.isEmpty()) {
            try {
                pendingAmt = Math.max(0.0, Double.parseDouble(pendStr));
            } catch (NumberFormatException ignored) {}
        }

        String mobile = txtMobile.getText().trim();
        String address = txtAddress.getText().trim();

        Customer c = new Customer(newId, name, tamilName, mobile, address, pendingAmt);
        if (customerDAO.updateCustomer(c, selectedCustomerId)) {
            JOptionPane.showMessageDialog(this, "Customer '" + name + "' (ID: #" + newId + ") updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadCustomers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update customer.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteCustomer() {
        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table to delete.", "No Customer Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = txtName.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this customer?\n\nID: #" + selectedCustomerId + "\nName: " + name,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (customerDAO.deleteCustomer(selectedCustomerId)) {
                JOptionPane.showMessageDialog(this, "Customer deleted successfully!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadCustomers();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete customer.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openAddCashDialog() {
        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table to receive / add cash payment.", "No Customer Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = txtName.getText().trim();
        double currentPending = 0.0;
        try {
            currentPending = Double.parseDouble(txtPending.getText().trim());
        } catch (Exception ignored) {}

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Receive Cash Payment - " + name, true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(440, 310);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Customer Info
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblCust = new JLabel("Customer:");
        lblCust.setFont(LABEL_FONT);
        content.add(lblCust, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        JLabel lblCustVal = new JLabel(name + " (ID: #" + selectedCustomerId + ")");
        lblCustVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        content.add(lblCustVal, gbc);

        // Current Pending
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblPend = new JLabel("Current Pending:");
        lblPend.setFont(LABEL_FONT);
        content.add(lblPend, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        JLabel lblPendVal = new JLabel(String.format("\u20B9%.2f", currentPending));
        lblPendVal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPendVal.setForeground(currentPending > 0.009 ? new Color(220, 53, 69) : new Color(40, 167, 69));
        content.add(lblPendVal, gbc);

        // Amount Received Field
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lblAmt = new JLabel("Amount Received \u20B9 *:");
        lblAmt.setFont(LABEL_FONT);
        content.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JTextField txtAmount = new JTextField();
        txtAmount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtAmount.setPreferredSize(new Dimension(180, 32));
        content.add(txtAmount, gbc);

        // Payment Mode
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lblMode = new JLabel("Payment Mode:");
        lblMode.setFont(LABEL_FONT);
        content.add(lblMode, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        JComboBox<String> cmbMode = new JComboBox<>(new String[]{"Cash", "GPay", "UPI", "Cheque"});
        cmbMode.setFont(FIELD_FONT);
        content.add(cmbMode, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel lblNotes = new JLabel("Notes (Optional):");
        lblNotes.setFont(LABEL_FONT);
        content.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        JTextField txtNotes = new JTextField();
        txtNotes.setFont(FIELD_FONT);
        content.add(txtNotes, gbc);

        dialog.add(content, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(LABEL_FONT);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton("Save Payment");
        btnSave.setFont(LABEL_FONT);
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));

        final double pendAmtFinal = currentPending;
        btnSave.addActionListener(e -> {
            String amtStr = txtAmount.getText().trim();
            if (amtStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter the payment amount received.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtAmount.requestFocusInWindow();
                return;
            }
            double paidAmt;
            try {
                paidAmt = Double.parseDouble(amtStr);
                if (paidAmt <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Payment amount must be greater than 0.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    txtAmount.requestFocusInWindow();
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid numeric amount.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                txtAmount.requestFocusInWindow();
                return;
            }

            String pMode = (String) cmbMode.getSelectedItem();
            String pNotes = txtNotes.getText().trim();

            if (customerDAO.recordCustomerPayment(selectedCustomerId, name, paidAmt, pMode, pNotes)) {
                double newPend = Math.max(0.0, pendAmtFinal - paidAmt);
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        String.format("Payment of \u20B9%.2f recorded successfully!\n\nCustomer: %s (ID: #%d)\nNew Pending Balance: \u20B9%.2f",
                                paidAmt, name, selectedCustomerId, newPend),
                        "Payment Recorded", JOptionPane.INFORMATION_MESSAGE);
                loadCustomers();
                txtPending.setText(String.format("%.2f", newPend));
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to record payment in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // Enter key navigation between dialog fields
        txtAmount.addActionListener(e -> cmbMode.requestFocusInWindow());
        cmbMode.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (cmbMode.isPopupVisible()) {
                        cmbMode.setPopupVisible(false);
                    }
                    txtNotes.requestFocusInWindow();
                    txtNotes.selectAll();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbMode.isPopupVisible()) {
                    cmbMode.showPopup();
                }
            }
        });
        txtNotes.addActionListener(e -> btnSave.doClick());

        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
                { txtAmount },
                { cmbMode },
                { txtNotes },
                { btnSave, btnCancel }
        });

        SwingUtilities.invokeLater(() -> {
            txtAmount.requestFocusInWindow();
            txtAmount.selectAll();
        });
        dialog.setVisible(true);
    }

    private void openSelectedCustomerHistory() {
        if (selectedCustomerId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table to view billing history.", "No Customer Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openCustomerBillingHistory(selectedCustomerId, txtName.getText().trim());
    }

    public interface OnEditBillListener {
        void onEditBill(int billId);
    }
    private OnEditBillListener editBillListener;
    public void setOnEditBillListener(OnEditBillListener listener) {
        this.editBillListener = listener;
    }

    private void openCustomerBillingHistory(int customerId, String customerName) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Customer Billing History - " + customerName + " (ID: #" + customerId + ")", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(740, 480);
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        customerDAO.syncCustomerPendingBalance(customerId);
        List<Bill> bills = customerDAO.getBillsByCustomerId(customerId);

        String[] cols = {"Bill No", "Date (DD-MM-YYYY HH:mm)", "Total (\u20B9)", "Paid (\u20B9)", "Pending (\u20B9)", "Payment Mode", "Action"};
        DefaultTableModel mod = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        double totSum = 0, paidSum = 0, pendSum = 0;

        for (Bill b : bills) {
            int displayNo = b.getDailyBillNo() > 0 ? b.getDailyBillNo() : b.getId();
            mod.addRow(new Object[]{
                    "#" + displayNo,
                    b.getBillDate() != null ? sdf.format(b.getBillDate()) : "",
                    String.format("%.2f", b.getTotalAmount()),
                    String.format("%.2f", b.getPaidAmount()),
                    String.format("%.2f", Math.max(0.0, b.getPendingAmount())),
                    b.getPaymentMode() != null ? b.getPaymentMode() : "Cash",
                    "Edit \u270E"
            });
            totSum += b.getTotalAmount();
            paidSum += b.getPaidAmount();
            pendSum += Math.max(0.0, b.getPendingAmount());
        }

        JTable billTable = new JTable(mod);
        billTable.setRowHeight(32);
        billTable.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        billTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        billTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        billTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        billTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        billTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        billTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        billTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer actionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setForeground(new Color(230, 126, 34)); // Amber / Orange
                lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
                return lbl;
            }
        };
        billTable.getColumnModel().getColumn(6).setCellRenderer(actionRenderer);
        billTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        billTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = billTable.rowAtPoint(e.getPoint());
                int col = billTable.columnAtPoint(e.getPoint());
                if (row >= 0 && (col == 6 || e.getClickCount() == 2)) {
                    int billId = bills.get(row).getId();
                    dialog.dispose();
                    if (editBillListener != null) {
                        editBillListener.onEditBill(billId);
                    }
                }
            }
        });

        // Header Summary Panel
        JPanel topPnl = new JPanel(new GridLayout(1, 4, 10, 0));
        topPnl.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));

        JLabel lblTotBills = new JLabel("Total Bills: " + bills.size());
        lblTotBills.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topPnl.add(lblTotBills);

        JLabel lblTotAmt = new JLabel(String.format("Total: \u20B9%.2f", totSum));
        lblTotAmt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotAmt.setForeground(new Color(0, 123, 255));
        topPnl.add(lblTotAmt);

        JLabel lblTotPaid = new JLabel(String.format("Paid: \u20B9%.2f", paidSum));
        lblTotPaid.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotPaid.setForeground(new Color(40, 167, 69));
        topPnl.add(lblTotPaid);

        JLabel lblTotPending = new JLabel(String.format("Pending: \u20B9%.2f", pendSum));
        lblTotPending.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotPending.setForeground(new Color(220, 53, 69));
        topPnl.add(lblTotPending);

        dialog.add(topPnl, BorderLayout.NORTH);
        dialog.add(new JScrollPane(billTable), BorderLayout.CENTER);

        JPanel btmPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton btnEditBill = new JButton("Edit Selected Bill");
        btnEditBill.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnEditBill.setBackground(new Color(230, 126, 34)); // Amber / Orange
        btnEditBill.setForeground(Color.WHITE);
        btnEditBill.setFocusPainted(false);
        btnEditBill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEditBill.addActionListener(ev -> {
            int sel = billTable.getSelectedRow();
            if (sel >= 0) {
                int billId = bills.get(sel).getId();
                dialog.dispose();
                if (editBillListener != null) {
                    editBillListener.onEditBill(billId);
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select a bill from the table to edit.", "No Bill Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btmPnl.add(btnEditBill);

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.addActionListener(ev -> dialog.dispose());
        btmPnl.add(btnClose);
        dialog.add(btmPnl, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void clearForm() {
        selectedCustomerId = -1;
        txtId.setText(String.valueOf(customerDAO.getNextCustomerId()));
        txtName.setText("");
        txtTamilName.setText("");
        txtMobile.setText("");
        txtAddress.setText("");
        txtPending.setText("0.00");
        table.clearSelection();
        lblFormMode.setText("Mode: Ready to Add New Customer");
        lblFormMode.setForeground(new Color(40, 167, 69));
        txtName.requestFocusInWindow();
    }
}
