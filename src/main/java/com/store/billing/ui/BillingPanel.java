package com.store.billing.ui;

import com.store.billing.dao.BillDAO;
import com.store.billing.dao.ProductDAO;
import com.store.billing.dao.SettingsDAO;
import com.store.billing.model.Bill;
import com.store.billing.model.BillItem;
import com.store.billing.model.Product;
import com.store.billing.model.Customer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import com.store.billing.util.ThermalPrinterUtil;

public class BillingPanel extends JPanel {
    private JComboBox<Object> cmbCustomer;
    private JCheckBox chkIncludeUpi;
    private JComboBox<String> cmbUpiList;
    private JTextField txtCash;
    private JTextField txtGPay;
    private JComboBox<String> cmbPrintLanguage;
    private JComboBox<String> cmbPriceType;
    private JComboBox<String> cmbItemsPerPage;
    private JLabel lblTotal;
    private JTextField txtDiscount;
    private JLabel lblRemaining;
    private JCheckBox chkAddAsPending;
    private JLabel lblPrevPending;
    private JCheckBox chkAddPrevPending;
    private JTextField txtPendingPaid;
    private double currentCustomerPending = 0.0;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JComboBox<String> nameCombo; 
    
    private ProductDAO productDAO;
    private BillDAO billDAO;
    private SettingsDAO settingsDAO;
    private com.store.billing.dao.CustomerDAO customerDAO;
    private com.store.billing.dao.PartyPriceDAO partyPriceDAO;
    
    private List<Product> allProducts = new ArrayList<>();
    private java.util.Map<String, Product> productMap = new java.util.HashMap<>();
    private double currentTotal = 0.0;
    private boolean isUpdatingTable = false;
    private boolean nameEditorStopInProgress = false; // true while stopCellEditing runs, blocks spurious enterAction

    private int editingBillId = -1;
    private int editingDailyBillNo = -1;
    private Date editingBillDate = null;
    private JPanel editBannerPanel;
    private JLabel lblEditBannerText;
    private JButton btnCancelEdit;
    private JButton btnCheckout;
    private JButton btnDirectPrint;
    private JButton btnSaveOnly;

    public BillingPanel() {
        productDAO = new ProductDAO();
        billDAO = new BillDAO();
        settingsDAO = new SettingsDAO();
        customerDAO = new com.store.billing.dao.CustomerDAO();
        partyPriceDAO = new com.store.billing.dao.PartyPriceDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Edit Mode Banner
        editBannerPanel = new JPanel(new BorderLayout(15, 0));
        editBannerPanel.setBackground(new Color(255, 243, 205)); // Soft amber
        editBannerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(255, 222, 142)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        lblEditBannerText = new JLabel("EDIT MODE: Modifying Bill #...");
        lblEditBannerText.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblEditBannerText.setForeground(new Color(133, 100, 4));
        editBannerPanel.add(lblEditBannerText, BorderLayout.CENTER);

        btnCancelEdit = new JButton("Cancel Edit / Start New Bill");
        btnCancelEdit.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCancelEdit.setBackground(new Color(220, 53, 69));
        btnCancelEdit.setForeground(Color.WHITE);
        btnCancelEdit.setFocusPainted(false);
        btnCancelEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancelEdit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(BillingPanel.this, 
                    "Are you sure you want to cancel editing this bill and start a new bill?", 
                    "Cancel Edit", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean wasEditing = isEditingExistingBill();
                clearCart();
                if (wasEditing && billStateChangeListener != null) {
                    billStateChangeListener.onEditCancelled(BillingPanel.this);
                }
            }
        });
        editBannerPanel.add(btnCancelEdit, BorderLayout.EAST);
        editBannerPanel.setVisible(false);

        // Top Panel for Data Entry
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Billing Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        topPanel.add(new JLabel("Customer Name (Optional) [F2]:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0; 
        gbc.anchor = GridBagConstraints.WEST;
        cmbCustomer = new JComboBox<>();
        cmbCustomer.setEditable(true);
        cmbCustomer.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        cmbCustomer.addActionListener(e -> updateSelectedCustomerPending());
        topPanel.add(cmbCustomer, gbc);
        
        gbc.gridx = 2; gbc.gridy = 0;
        gbc.weightx = 0; 
        gbc.anchor = GridBagConstraints.EAST;
        topPanel.add(new JLabel("Price Type:"), gbc);
        
        gbc.gridx = 3; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        cmbPriceType = new JComboBox<>(new String[]{"Wholesale", "Retail", "Line Rate"});
        cmbPriceType.setSelectedItem("Wholesale");
        cmbPriceType.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cmbPriceType.setPreferredSize(new Dimension(150, 30));
        cmbPriceType.addActionListener(e -> applyPriceTypeToCart());
        topPanel.add(cmbPriceType, gbc);
        
        gbc.gridx = 4; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        topPanel.add(new JLabel("Items per Page:"), gbc);
        
        gbc.gridx = 5; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        cmbItemsPerPage = new JComboBox<>(new String[]{"None", "5", "10", "15", "20", "25", "30"});
        cmbItemsPerPage.setSelectedItem("None");
        cmbItemsPerPage.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topPanel.add(cmbItemsPerPage, gbc);
        
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        topPanel.add(new JLabel("Print Lang:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        cmbPrintLanguage = new JComboBox<>(new String[]{"Tamil", "English"});
        cmbPrintLanguage.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topPanel.add(cmbPrintLanguage, gbc);
        
        gbc.gridx = 2; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        chkIncludeUpi = new JCheckBox("Include UPI QR (F9)");
        chkIncludeUpi.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(chkIncludeUpi, gbc);
        
        gbc.gridx = 4; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        cmbUpiList = new JComboBox<>();
        cmbUpiList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cmbUpiList.setPreferredSize(new Dimension(220, 30));
        cmbUpiList.addActionListener(e -> {
            Object sel = cmbUpiList.getSelectedItem();
            if (sel != null) {
                String str = sel.toString().trim();
                if ("None".equalsIgnoreCase(str)) {
                    chkIncludeUpi.setSelected(false);
                } else {
                    chkIncludeUpi.setSelected(true);
                }
            }
        });
        chkIncludeUpi.addActionListener(e -> {
            if (chkIncludeUpi.isSelected()) {
                if (cmbUpiList.getItemCount() > 1) {
                    if ("None".equalsIgnoreCase(String.valueOf(cmbUpiList.getSelectedItem()))) {
                        cmbUpiList.setSelectedIndex(1);
                    }
                    cmbUpiList.requestFocusInWindow();
                    cmbUpiList.showPopup();
                }
            } else {
                cmbUpiList.setSelectedItem("None");
            }
        });
        topPanel.add(cmbUpiList, gbc);
        
        JPanel northContainer = new JPanel(new BorderLayout(0, 5));
        northContainer.add(editBannerPanel, BorderLayout.NORTH);
        northContainer.add(topPanel, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        // Center Panel for Cart Table: S.No, Product Name, Qty, Unit Type, Sub Total, Act
        String[] columns = {"S.No", "Product Name", "Qty", "Unit Type", "Sub Total", "Act"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2 || column == 3 || column == 4;
            }
        };
        cartTable = new JTable(cartModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (isCellSelected(row, column)) {
                    ((JComponent) c).setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));
                }
                return c;
            }

            @Override
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && pressed) {
                    if (isEditing()) {
                        getCellEditor().stopCellEditing();
                    }
                    getActionMap().get("enterAction").actionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, null));
                    return true;
                }
                return super.processKeyBinding(ks, e, condition, pressed);
            }
        };
        cartTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        cartTable.setRowHeight(35);
        cartTable.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        cartTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        cartTable.setShowGrid(false);
        cartTable.setShowHorizontalLines(true);
        cartTable.setGridColor(new Color(230, 230, 230));
        cartTable.setIntercellSpacing(new Dimension(0, 0));
        
        // Column Widths: Product Name gets large width, other columns compact
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(0).setMaxWidth(70);
        cartTable.getColumnModel().getColumn(0).setMinWidth(60);
        
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(700); 
        cartTable.getColumnModel().getColumn(1).setMinWidth(300);
        
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(2).setMinWidth(80);
        
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(3).setMinWidth(80);
        
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        cartTable.getColumnModel().getColumn(4).setMinWidth(120);
        
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        cartTable.getColumnModel().getColumn(5).setMaxWidth(80);
        cartTable.getColumnModel().getColumn(5).setMinWidth(70);
        
        cartTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = cartTable.rowAtPoint(e.getPoint());
                int col = cartTable.columnAtPoint(e.getPoint());
                if (col == 5 && row >= 0 && row < cartModel.getRowCount()) {
                    if (row == cartModel.getRowCount() - 1 && getStr(cartModel.getValueAt(row, 1)).isEmpty()) {
                        return; // don't delete the only empty row
                    }
                    if (cartTable.isEditing()) {
                        cartTable.getCellEditor().stopCellEditing();
                    }
                    cartModel.removeRow(row);
                    // Re-number
                    for (int i = 0; i < cartModel.getRowCount(); i++) {
                        cartModel.setValueAt(String.valueOf(i + 1), i, 0);
                    }
                    if (cartModel.getRowCount() == 0) {
                        cartModel.addRow(new Object[]{"1", "", "", "", "", "Del"});
                    }
                    recalculateTotal();
                }
            }
        });
        
        // Remove blue selection background
        cartTable.setSelectionBackground(Color.WHITE);
        cartTable.setSelectionForeground(Color.BLACK);
        cartTable.setCellSelectionEnabled(true);
        cartTable.setSurrendersFocusOnKeystroke(true);
        
        // Custom Enter behavior
        cartTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "enterAction");
        cartTable.getActionMap().put("enterAction", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (cartTable.isEditing()) {
                    cartTable.getCellEditor().stopCellEditing();
                }
                int row = cartTable.getSelectedRow();
                int col = cartTable.getSelectedColumn();
                if (row < 0) row = 0;
                if (col < 0) col = 1;
                
                if (col == 1) { // Product Name -> Move to Qty (col 2)
                    Object val = cartModel.getValueAt(row, 1);
                    String nameStr = val != null ? val.toString().trim() : "";
                    if (nameStr.isEmpty()) {
                        if (cartModel.getRowCount() > 1 && row == cartModel.getRowCount() - 1) {
                            cartModel.removeRow(row);
                        }
                        if (currentTotal > 0) {
                            cartTable.clearSelection(); // Clear selection
                            txtCash.requestFocusInWindow();
                            txtCash.selectAll();
                        }
                        return;
                    }
                    // Move directly to Qty (col 2)
                    cartTable.changeSelection(row, 2, false, false);
                    cartTable.editCellAt(row, 2);
                    Component editor = cartTable.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                        if (editor instanceof JTextField) {
                            ((JTextField) editor).selectAll();
                        }
                    }
                    return;
                }
                
                if (col == 2) { // Qty -> Move to next row Product Name (col 1)
                    if (row == cartModel.getRowCount() - 1) {
                        cartModel.addRow(new Object[]{String.valueOf(cartModel.getRowCount() + 1), "", "", "", "", "Del"});
                    }
                    int nextRow = row + 1;
                    cartTable.changeSelection(nextRow, 1, false, false);
                    cartTable.editCellAt(nextRow, 1);
                    Component editor = cartTable.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                        if (editor instanceof JTextField) {
                            ((JTextField) editor).selectAll();
                        }
                    }
                    return;
                }

                if (col == 4) { // Sub Total manual edit -> Move to next row Product Name (col 1)
                    if (row == cartModel.getRowCount() - 1) {
                        cartModel.addRow(new Object[]{String.valueOf(cartModel.getRowCount() + 1), "", "", "", "", "Del"});
                    }
                    int nextRow = row + 1;
                    cartTable.changeSelection(nextRow, 1, false, false);
                    cartTable.editCellAt(nextRow, 1);
                    Component editor = cartTable.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                        if (editor instanceof JTextField) {
                            ((JTextField) editor).selectAll();
                        }
                    }
                    return;
                }
                
                if (col == 5) { // Del column
                    if (row == cartModel.getRowCount() - 1 && getStr(cartModel.getValueAt(row, 1)).isEmpty()) {
                        return;
                    }
                    cartModel.removeRow(row);
                    for (int i = 0; i < cartModel.getRowCount(); i++) {
                        cartModel.setValueAt(String.valueOf(i + 1), i, 0);
                    }
                    if (cartModel.getRowCount() == 0) {
                        cartModel.addRow(new Object[]{"1", "", "", "", "", "Del"});
                    }
                    recalculateTotal();
                    int nextRow = Math.min(row, cartModel.getRowCount() - 1);
                    cartTable.changeSelection(nextRow, 1, false, false);
                    cartTable.editCellAt(nextRow, 1);
                    Component editor = cartTable.getEditorComponent();
                    if (editor != null) {
                        editor.requestFocusInWindow();
                        if (editor instanceof JTextField) {
                            ((JTextField) editor).selectAll();
                        }
                    }
                    return;
                }
            }
        });
        
        // Custom Editors
        JTextField defaultTextField = new JTextField();
        defaultTextField.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        defaultTextField.addActionListener(e -> {
            if (cartTable.isEditing()) {
                cartTable.getCellEditor().stopCellEditing();
            }
            cartTable.getActionMap().get("enterAction").actionPerformed(
                new java.awt.event.ActionEvent(cartTable, java.awt.event.ActionEvent.ACTION_PERFORMED, "Enter")
            );
        });
        defaultTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2 || e.getKeyCode() == KeyEvent.VK_F1) {
                    focusCustomerName();
                    e.consume();
                }
            }
        });
        cartTable.setDefaultEditor(Object.class, new DefaultCellEditor(defaultTextField));

        nameCombo = new JComboBox<>();
        nameCombo.setEditable(true);
        nameCombo.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        // Keep lightweight popup (true = default) so mouse hover/click works correctly inside JTable.
        // Heavy-weight popup (false) causes mouse coordinate mismatch — hover won't track items properly.
        nameCombo.setLightWeightPopupEnabled(true);
        

        nameCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                c.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
                return c;
            }
        });
        
        // Add autocomplete functionality with code and multi-token search support
        JTextField nameComboEditor = (JTextField) nameCombo.getEditor().getEditorComponent();
        nameComboEditor.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        nameComboEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2 || e.getKeyCode() == KeyEvent.VK_F1) {
                    if (nameCombo.isPopupVisible()) {
                        nameCombo.hidePopup();
                    }
                    focusCustomerName();
                    e.consume();
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                int code = e.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_UP || code == java.awt.event.KeyEvent.VK_DOWN || 
                    code == java.awt.event.KeyEvent.VK_ENTER || code == java.awt.event.KeyEvent.VK_ESCAPE ||
                    code == java.awt.event.KeyEvent.VK_LEFT || code == java.awt.event.KeyEvent.VK_RIGHT ||
                    code == KeyEvent.VK_F2 || code == KeyEvent.VK_F1) {
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    String text = nameComboEditor.getText();
                    java.util.Vector<String> items = new java.util.Vector<>();
                    boolean found = false;
                    
                    if (text.trim().isEmpty()) {
                        int count = 0;
                        for (Product p : allProducts) {
                            items.add(formatProductComboItem(p));
                            count++;
                            if (count >= 50) break;
                        }
                    } else {
                        List<Product> matches = searchProductsMultiToken(text);
                        int count = 0;
                        for (Product p : matches) {
                            items.add(formatProductComboItem(p));
                            found = true;
                            count++;
                            if (count >= 50) break; // Limit to 50 for smooth UI rendering
                        }
                    }
                    
                    if (nameCombo.getModel().getSize() != items.size() || !items.isEmpty()) {
                        nameCombo.setModel(new javax.swing.DefaultComboBoxModel<>(items));
                        nameCombo.getEditor().setItem(text); // preserve typed text
                        if (text.trim().isEmpty()) {
                            nameCombo.setSelectedItem(null);
                        }
                    }
                    
                    if (!text.trim().isEmpty() && found && nameComboEditor.hasFocus() && nameCombo.isShowing()) {
                        if (!nameCombo.isPopupVisible()) {
                            try {
                                nameCombo.showPopup();
                            } catch (java.awt.IllegalComponentStateException ignored) {}
                        }
                    } else {
                        nameCombo.hidePopup();
                    }
                });
            }
        });
        
        JComboBox<String> unitCombo = new JComboBox<>();
        com.store.billing.dao.UnitDAO unitDAO = new com.store.billing.dao.UnitDAO();
        for (com.store.billing.model.Unit u : unitDAO.getAllUnits()) {
            unitCombo.addItem(u.getNameEn());
        }
        unitCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font("Nirmala UI", Font.PLAIN, 14));
                if (value != null) {
                    String val = value.toString().trim();
                    com.store.billing.model.Unit u = com.store.billing.dao.UnitDAO.findUnit(val);
                    if (u != null && u.getNameTa() != null && !u.getNameTa().isEmpty() && !u.getNameTa().equalsIgnoreCase(u.getNameEn())) {
                        setText(u.getNameEn() + " - " + u.getNameTa());
                    } else {
                        setText(val);
                    }
                }
                return this;
            }
        });
        unitCombo.setEditable(true);

        java.awt.event.KeyListener arrowKeyListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.isAltDown() || e.isControlDown()) return;
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN || 
                    code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
                    
                    if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
                        if (nameCombo != null && nameCombo.isPopupVisible()) return;
                        if (unitCombo != null && unitCombo.isPopupVisible()) return;
                    }
                    
                    if (e.getSource() instanceof JTextField) {
                        JTextField tf = (JTextField) e.getSource();
                        if (code == KeyEvent.VK_LEFT && tf.getCaretPosition() > 0) return;
                        if (code == KeyEvent.VK_RIGHT && tf.getCaretPosition() < tf.getText().length()) return;
                    }
                    
                    int r = cartTable.getSelectedRow();
                    int c = cartTable.getSelectedColumn();
                    
                    if (code == KeyEvent.VK_UP && r > 0) r--;
                    else if (code == KeyEvent.VK_DOWN && r < cartTable.getRowCount() - 1) r++;
                    else if (code == KeyEvent.VK_LEFT) {
                        do {
                            c--;
                            if (c < 1) {
                                if (r > 0) {
                                    r--;
                                    c = 5;
                                } else {
                                    return;
                                }
                            }
                        } while (!cartModel.isCellEditable(r, c) && c != 5);
                    }
                    else if (code == KeyEvent.VK_RIGHT) {
                        do {
                            c++;
                            if (c > 5) {
                                if (r < cartTable.getRowCount() - 1) {
                                    r++;
                                    c = 1;
                                } else {
                                    return;
                                }
                            }
                        } while (!cartModel.isCellEditable(r, c) && c != 5);
                    }
                    else return;
                    
                    e.consume();
                    if (cartTable.isEditing()) {
                        cartTable.putClientProperty("arrowKeyNavigation", true);
                        cartTable.getCellEditor().stopCellEditing();
                        cartTable.putClientProperty("arrowKeyNavigation", false);
                    }
                    
                    final int nextRow = r;
                    final int nextCol = c;
                    SwingUtilities.invokeLater(() -> {
                        cartTable.changeSelection(nextRow, nextCol, false, false);
                        if (cartModel.isCellEditable(nextRow, nextCol)) {
                            cartTable.editCellAt(nextRow, nextCol);
                            Component editor = cartTable.getEditorComponent();
                            if (editor != null) {
                                editor.requestFocusInWindow();
                                if (editor instanceof JTextField) {
                                    ((JTextField) editor).selectAll();
                                }
                            }
                        } else {
                            cartTable.requestFocusInWindow();
                        }
                    });
                }
            }
        };

        defaultTextField.addKeyListener(arrowKeyListener);
        nameCombo.getEditor().getEditorComponent().addKeyListener(arrowKeyListener);
        unitCombo.getEditor().getEditorComponent().addKeyListener(arrowKeyListener);
        cartTable.addKeyListener(arrowKeyListener); // Add to table itself to handle non-editing navigation

        java.awt.event.ActionListener comboEnterListener = e -> {
            if ("comboBoxEdited".equals(e.getActionCommand())) {
                if (Boolean.TRUE.equals(cartTable.getClientProperty("arrowKeyNavigation"))) {
                    return;
                }
                // DefaultCellEditor.stopCellEditing() internally calls comboBox.actionPerformed()
                // which fires "comboBoxEdited". Block that spurious event — only act on real user Enter.
                if (nameEditorStopInProgress) return;
                if (cartTable.isEditing()) {
                    cartTable.getCellEditor().stopCellEditing();
                }
                SwingUtilities.invokeLater(() -> {
                    cartTable.getActionMap().get("enterAction").actionPerformed(
                        new java.awt.event.ActionEvent(cartTable, java.awt.event.ActionEvent.ACTION_PERFORMED, "Enter")
                    );
                });
            }
        };

        nameCombo.addActionListener(comboEnterListener);
        DefaultCellEditor nameEditor = new DefaultCellEditor(nameCombo) {
            @Override
            public boolean stopCellEditing() {
                // DefaultCellEditor.stopCellEditing() → delegate.stopCellEditing()
                //   → comboBox.actionPerformed() → fires "comboBoxEdited"
                //   → delegate.actionPerformed() called again (loop!) → enterAction navigates rows.
                // FIX: Bypass the delegate chain entirely. Call fireEditingStopped() directly
                // so the table reads getCellEditorValue() and commits — no events fired.
                nameEditorStopInProgress = true;
                try {
                    fireEditingStopped();
                    return true;
                } finally {
                    nameEditorStopInProgress = false;
                }
            }

            @Override
            public Object getCellEditorValue() {
                // Read directly from editor text field (not from combo model selection)
                // so typed/searched text is always captured correctly.
                // CRITICAL FIX: If editor text is empty or blank, ALWAYS return empty string "".
                // NEVER fall back to nameCombo.getSelectedItem() because JComboBox defaults to item 0 (e.g. Biscuit)!
                Object edItem = nameCombo.getEditor().getItem();
                if (edItem != null) {
                    String str = edItem.toString().trim();
                    if (!str.isEmpty()) {
                        return str;
                    }
                }
                return "";
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                String valStr = value != null ? value.toString().trim() : "";
                nameCombo.getEditor().setItem(valStr);
                if (valStr.isEmpty()) {
                    nameCombo.setSelectedItem(null);
                }
                return c;
            }
        };
        cartTable.getColumnModel().getColumn(1).setCellEditor(nameEditor);

        setupTableListener();

        // Center Area: Cart Table + Table Footer Row with Total directly under Sub Total column
        JPanel tableCenterPanel = new JPanel(new BorderLayout(0, 0));
        tableCenterPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel tableFooter = new JPanel(new BorderLayout(0, 0));
        tableFooter.setBackground(Color.WHITE);
        tableFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 1, 0, new Color(210, 220, 230)),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)
        ));

        JPanel rightTotalBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTotalBox.setOpaque(false);
        
        JLabel lblDiscountText = new JLabel("Discount (F10) : \u20B9");
        lblDiscountText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtDiscount = new JTextField("");
        txtDiscount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtDiscount.setPreferredSize(new Dimension(80, 35));
        txtDiscount.setHorizontalAlignment(JTextField.RIGHT);
        txtDiscount.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
        });
        txtDiscount.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (txtCash != null) {
                        txtCash.requestFocusInWindow();
                    }
                }
            }
        });
        rightTotalBox.add(lblDiscountText);
        rightTotalBox.add(txtDiscount);
        rightTotalBox.add(Box.createHorizontalStrut(15));
        
        lblTotal = new JLabel("Total : \u20B90.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTotal.setForeground(new Color(40, 167, 69));
        rightTotalBox.add(lblTotal);
        // Offset by 55px so Total sits directly under the Sub Total column (before Act column)
        rightTotalBox.add(Box.createHorizontalStrut(55));
        tableFooter.add(rightTotalBox, BorderLayout.EAST);

        tableCenterPanel.add(tableFooter, BorderLayout.SOUTH);
        add(tableCenterPanel, BorderLayout.CENTER);

        // Bottom Panel for Pending and Checkout Controls
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 8));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 220, 230)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        bottomPanel.setBackground(Color.WHITE);

        // Left Side: Previous Pending, Add Option, Pending Paid/Add, and Clear Cart (Box 4, 5, 6 from diagram)
        JPanel leftBox = new JPanel();
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));
        leftBox.setOpaque(false);

        lblPrevPending = new JLabel("Previous Pending: \u20B90.00");
        lblPrevPending.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblPrevPending.setForeground(Color.DARK_GRAY);

        chkAddPrevPending = new JCheckBox("Add Previous Pending to Total (F7)");
        chkAddPrevPending.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chkAddPrevPending.setForeground(new Color(220, 53, 69));
        chkAddPrevPending.setOpaque(false);
        chkAddPrevPending.setFocusPainted(false);
        chkAddPrevPending.addActionListener(e -> recalculateTotal());

        JPanel pendPaidRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pendPaidRow.setOpaque(false);
        JLabel lblPendPaid = new JLabel("Add/Pay Pending \u20B9 (F8):");
        lblPendPaid.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPendingPaid = new JTextField(7);
        txtPendingPaid.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtPendingPaid.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { recalculateTotal(); }
        });
        txtPendingPaid.addActionListener(e -> {
            txtCash.requestFocusInWindow();
            txtCash.selectAll();
        });
        txtPendingPaid.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2 || e.getKeyCode() == KeyEvent.VK_F1) {
                    focusCustomerName();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_F3 || e.getKeyCode() == KeyEvent.VK_UP) {
                    focusProductSearch();
                    e.consume();
                }
            }
        });
        pendPaidRow.add(lblPendPaid);
        pendPaidRow.add(txtPendingPaid);

        JButton btnClear = new JButton("Clear Cart (F5)");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnClear.setBackground(new Color(108, 117, 125));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> clearCart());

        leftBox.add(lblPrevPending);
        leftBox.add(Box.createVerticalStrut(3));
        leftBox.add(chkAddPrevPending);
        leftBox.add(Box.createVerticalStrut(3));
        leftBox.add(pendPaidRow);
        leftBox.add(Box.createVerticalStrut(5));
        leftBox.add(btnClear);

        bottomPanel.add(leftBox, BorderLayout.WEST);

        // Right Side: Cash / GPay, Remaining / Balance, & Action Buttons (Box 12, 13, 15, 8 from diagram)
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        JPanel paymentPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        paymentPanel.setOpaque(false);

        java.awt.event.KeyAdapter backToTableAdapter = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2 || e.getKeyCode() == KeyEvent.VK_F1) {
                    focusCustomerName();
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP || (e.getKeyCode() == KeyEvent.VK_LEFT && ((JTextField)e.getSource()).getCaretPosition() == 0)) {
                    int lastRow = cartModel.getRowCount() - 1;
                    if (lastRow >= 0) {
                        cartTable.requestFocusInWindow();
                        cartTable.changeSelection(lastRow, 1, false, false);
                        cartTable.editCellAt(lastRow, 1);
                        Component editor = cartTable.getEditorComponent();
                        if (editor != null) {
                            editor.requestFocusInWindow();
                            if (editor instanceof JTextField) {
                                ((JTextField) editor).selectAll();
                            }
                        }
                    }
                }
            }
        };

        JLabel lblCash = new JLabel("Cash \u20B9:");
        lblCash.setFont(new Font("Segoe UI", Font.BOLD, 16));
        paymentPanel.add(lblCash);
        txtCash = new JTextField(6);
        txtCash.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtCash.addActionListener(e -> {
            txtGPay.requestFocusInWindow();
            txtGPay.selectAll();
        });
        txtCash.addKeyListener(backToTableAdapter);
        paymentPanel.add(txtCash);

        JLabel lblGPay = new JLabel("GPay \u20B9:");
        lblGPay.setFont(new Font("Segoe UI", Font.BOLD, 16));
        paymentPanel.add(lblGPay);
        txtGPay = new JTextField(6);
        txtGPay.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtGPay.addActionListener(e -> processCheckout(false, false));
        txtGPay.addKeyListener(backToTableAdapter);
        paymentPanel.add(txtGPay);

        // Real-time listener for Cash & GPay to compute remaining balance dynamically
        javax.swing.event.DocumentListener payDocListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateRemainingBalance(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateRemainingBalance(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateRemainingBalance(); }
        };
        txtCash.getDocument().addDocumentListener(payDocListener);
        txtGPay.getDocument().addDocumentListener(payDocListener);

        // Middle Row: Remaining / Pending Balance Display (Directly below Cash & GPay)
        JPanel remainingPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        remainingPanel.setOpaque(false);

        chkAddAsPending = new JCheckBox("Add Remaining as Pending in Bill (F6)");
        chkAddAsPending.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chkAddAsPending.setForeground(new Color(220, 53, 69));
        chkAddAsPending.setOpaque(false);
        chkAddAsPending.setFocusPainted(false);
        chkAddAsPending.setVisible(false);
        chkAddAsPending.addActionListener(e -> updateRemainingBalance());
        remainingPanel.add(chkAddAsPending);

        lblRemaining = new JLabel("Remaining: \u20B90.00");
        lblRemaining.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblRemaining.setForeground(Color.GRAY);
        remainingPanel.add(lblRemaining);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        buttonPanel.setOpaque(false);

        btnDirectPrint = new JButton("Direct Print (F11)");
        btnDirectPrint.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnDirectPrint.setBackground(new Color(40, 167, 69));
        btnDirectPrint.setForeground(Color.WHITE);
        btnDirectPrint.setFocusPainted(false);
        btnDirectPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDirectPrint.addActionListener(e -> processCheckout(true, false));
        buttonPanel.add(btnDirectPrint);

        btnCheckout = new JButton("Preview & Print (F12)");
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnCheckout.setBackground(new Color(0, 123, 255));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);
        btnCheckout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCheckout.addActionListener(e -> processCheckout(false, false));
        buttonPanel.add(btnCheckout);

        btnSaveOnly = new JButton("Save Only (Ctrl+S)");
        btnSaveOnly.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSaveOnly.setBackground(new Color(23, 162, 184)); // Info Blue
        btnSaveOnly.setForeground(Color.WHITE);
        btnSaveOnly.setFocusPainted(false);
        btnSaveOnly.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaveOnly.addActionListener(e -> processCheckout(false, true));
        buttonPanel.add(btnSaveOnly);

        rightPanel.add(paymentPanel);
        rightPanel.add(remainingPanel);
        rightPanel.add(buttonPanel);

        bottomPanel.add(rightPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        
        setupKeyboardShortcuts();
        setupTopPanelNavigation();
        
        // 2D Arrow Key Navigation for Bottom Payment & Action Controls
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { txtPendingPaid, txtCash, txtGPay },
            { btnClear, btnDirectPrint, btnCheckout }
        });
        
        refreshProducts();
        loadUpiSettings();
        
        // Add initial empty row
        cartModel.addRow(new Object[]{"1", "", "", "", "", "Del"});
        
        // Auto-focus first product search box whenever Billing tab/page is shown
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                focusProductSearch();
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        // Initial Focus
        focusProductSearch();
    }

    private double calculatePriceWithThreshold(Product p, String basePriceType, double qty) {
        // First check for Party wise custom price
        if (cmbCustomer != null) {
            Object sel = cmbCustomer.getSelectedItem();
            if (sel instanceof Customer) {
                Customer c = (Customer) sel;
                Double customPrice = partyPriceDAO.getPartyPrice(c.getId(), p.getId());
                if (customPrice != null) {
                    return customPrice;
                }
            }
        }

        String pt = basePriceType;
        if ("Wholesale".equalsIgnoreCase(basePriceType)) pt = "Whole";
        double price = p.getPrice(pt);
        if (qty > 0) {
            if ("Retail".equalsIgnoreCase(basePriceType) && p.getRetailQtyThreshold() > 0 && qty >= p.getRetailQtyThreshold()) {
                price = p.getPrice("Retail(Dis)");
            } else if (("Wholesale".equalsIgnoreCase(basePriceType) || "Whole".equalsIgnoreCase(basePriceType)) && p.getWholeQtyThreshold() > 0 && qty >= p.getWholeQtyThreshold()) {
                price = p.getPrice("WHOLE(Dis)");
            }
        }
        return price;
    }

    public String formatProductComboItem(Product p) {
        if (p == null) return "";
        String code = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? "[" + p.getBarcode().trim() + "] " : "";
        String name = (p.getDisplayName() != null && !p.getDisplayName().trim().isEmpty()) ? p.getDisplayName().trim() :
                      (p.getTamilName() != null && !p.getTamilName().trim().isEmpty()) ? p.getTamilName().trim() : p.getName();
        
        String unit = (p.getUnitType() != null && !p.getUnitType().trim().isEmpty()) ? " - " + p.getUnitType() : "";
        return code + name + unit;
    }

    public List<Product> searchProductsMultiToken(String query) {
        return com.store.billing.util.ProductSearchUtil.searchProducts(allProducts, query, 50);
    }

    public Product findProductInCart(String name, String unit) {
        if (name == null || name.trim().isEmpty()) return null;
        String q = name.trim();
        String u = unit != null ? unit.trim() : "";
        
        // 1. Exact displayName + unit match
        for (Product prod : allProducts) {
            String dName = (prod.getDisplayName() != null && !prod.getDisplayName().trim().isEmpty()) ? prod.getDisplayName().trim() : "";
            if (dName.equalsIgnoreCase(q)) {
                String prodUnit = prod.getUnitType() != null ? prod.getUnitType().trim() : "";
                if (u.isEmpty() || prodUnit.equalsIgnoreCase(u)) {
                    return prod;
                }
            }
        }

        // 2. Exact tamilName + unit match
        for (Product prod : allProducts) {
            String tName = (prod.getTamilName() != null && !prod.getTamilName().trim().isEmpty()) ? prod.getTamilName().trim() : "";
            if (tName.equalsIgnoreCase(q)) {
                String prodUnit = prod.getUnitType() != null ? prod.getUnitType().trim() : "";
                if (u.isEmpty() || prodUnit.equalsIgnoreCase(u)) {
                    return prod;
                }
            }
        }

        // 3. Exact englishName + unit match
        for (Product prod : allProducts) {
            String eName = (prod.getEnglishName() != null && !prod.getEnglishName().trim().isEmpty()) ? prod.getEnglishName().trim() : "";
            if (eName.equalsIgnoreCase(q)) {
                String prodUnit = prod.getUnitType() != null ? prod.getUnitType().trim() : "";
                if (u.isEmpty() || prodUnit.equalsIgnoreCase(u)) {
                    return prod;
                }
            }
        }

        // 4. Exact base name + unit match
        for (Product prod : allProducts) {
            String bName = (prod.getName() != null && !prod.getName().trim().isEmpty()) ? prod.getName().trim() : "";
            if (bName.equalsIgnoreCase(q)) {
                String prodUnit = prod.getUnitType() != null ? prod.getUnitType().trim() : "";
                if (u.isEmpty() || prodUnit.equalsIgnoreCase(u)) {
                    return prod;
                }
            }
        }
        
        return findProduct(name);
    }

    public Product findProduct(String query) {
        if (query == null) return null;
        String q = query.trim();
        if (q.isEmpty()) return null;
        
        // 0. Exact match against dropdown display text
        for (Product prod : allProducts) {
            if (formatProductComboItem(prod).equalsIgnoreCase(q)) {
                return prod;
            }
        }

        // 1. If starts with [CODE] e.g. "[B10] Rs.10 ..."
        if (q.startsWith("[") && q.contains("]")) {
            String codeInside = q.substring(1, q.indexOf("]")).trim().toLowerCase();
            String nameAfter = q.substring(q.indexOf("]") + 1).trim().toLowerCase();
            
            for (Product prod : allProducts) {
                String prodCode = prod.getBarcode() != null ? prod.getBarcode().trim().toLowerCase() : "";
                if (prodCode.equals(codeInside)) {
                    String dName = (prod.getDisplayName() != null && !prod.getDisplayName().isEmpty() ? prod.getDisplayName() : prod.getName()).toLowerCase();
                    if (dName.contains(nameAfter) || nameAfter.contains(dName)) {
                        return prod;
                    }
                }
            }
            Product codeMatch = null;
            for (Product prod : allProducts) {
                String prodCode = prod.getBarcode() != null ? prod.getBarcode().trim().toLowerCase() : "";
                if (prodCode.equals(codeInside)) {
                    if (codeMatch == null || prod.getRetailPrice() < codeMatch.getRetailPrice()) {
                        codeMatch = prod;
                    }
                }
            }
            if (codeMatch != null) return codeMatch;
        }

        // 2. Exact Display Name lookup (Top priority)
        for (Product prod : allProducts) {
            if (prod.getDisplayName() != null && !prod.getDisplayName().trim().isEmpty() && prod.getDisplayName().trim().equalsIgnoreCase(q)) {
                return prod;
            }
        }

        // 3. Exact Tamil Name lookup
        for (Product prod : allProducts) {
            if (prod.getTamilName() != null && !prod.getTamilName().trim().isEmpty() && prod.getTamilName().trim().equalsIgnoreCase(q)) {
                return prod;
            }
        }

        // 4. Exact English Name lookup
        for (Product prod : allProducts) {
            if (prod.getEnglishName() != null && !prod.getEnglishName().trim().isEmpty() && prod.getEnglishName().trim().equalsIgnoreCase(q)) {
                return prod;
            }
        }

        // 5. Exact Base Name lookup (prefer lower retail price / packet over box if shared base name)
        Product baseMatch = null;
        for (Product prod : allProducts) {
            if (prod.getName() != null && !prod.getName().trim().isEmpty() && prod.getName().trim().equalsIgnoreCase(q)) {
                if (baseMatch == null || prod.getRetailPrice() < baseMatch.getRetailPrice()) {
                    baseMatch = prod;
                }
            }
        }
        if (baseMatch != null) return baseMatch;

        Product barcodeMatch = null;
        for (Product prod : allProducts) {
            if (prod.getBarcode() != null && prod.getBarcode().trim().equalsIgnoreCase(q)) {
                if (barcodeMatch == null || prod.getRetailPrice() < barcodeMatch.getRetailPrice()) {
                    barcodeMatch = prod;
                }
            }
        }
        if (barcodeMatch != null) return barcodeMatch;

        // 7. Multi-token search fallback
        List<Product> matches = searchProductsMultiToken(q);
        if (!matches.isEmpty()) {
            return matches.get(0);
        }

        return null;
    }

    private void applyPriceTypeToCart() {
        if (isUpdatingTable) return;
        String priceType = (String) cmbPriceType.getSelectedItem();
        isUpdatingTable = true;
        try {
            for (int row = 0; row < cartModel.getRowCount(); row++) {
                String nameStr = getStr(cartModel.getValueAt(row, 1));
                String qtyStr = getStr(cartModel.getValueAt(row, 2));
                String unitStr = getStr(cartModel.getValueAt(row, 3));
                if (!nameStr.isEmpty()) {
                    Product p = findProductInCart(nameStr, unitStr);
                    if (p != null) {
                        double qty = 0;
                        try { qty = Double.parseDouble(qtyStr); } catch (Exception ignored) {}
                        double price = calculatePriceWithThreshold(p, priceType, qty);
                        double rawSubtotal = price * qty;
                        long roundedSubtotal = Math.round(rawSubtotal);
                        cartModel.setValueAt(String.format("%.2f", (double) roundedSubtotal), row, 4);
                    }
                }
            }
        } finally {
            isUpdatingTable = false;
        }
        recalculateTotal();
    }

    private void setupTableListener() {
        cartModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (isUpdatingTable) return;
                
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row < 0 || col < 0) return;
                
                if (e.getType() == TableModelEvent.UPDATE) {
                    processTableUpdate(row, col);
                }
            }
        });
    }

    private void processTableUpdate(int row, int col) {
        if (isUpdatingTable) return;
        isUpdatingTable = true;
        try {
            if (col == 1) { // Product Name (col 1)
                String nameStr = getStr(cartModel.getValueAt(row, 1));
                
                Product found = null;
                if (!nameStr.isEmpty()) {
                    found = findProduct(nameStr);
                }
                
                if (found != null) {
                    String displayName = (found.getDisplayName() != null && !found.getDisplayName().isEmpty()) ? found.getDisplayName() :
                                         (found.getTamilName() != null && !found.getTamilName().isEmpty()) ? found.getTamilName() : found.getName();
                    cartModel.setValueAt(String.valueOf(row + 1), row, 0);
                    cartModel.setValueAt(displayName, row, 1);
                    
                    String qtyStr = getStr(cartModel.getValueAt(row, 2));
                    double qty = 1;
                    if (!qtyStr.isEmpty()) {
                        try { qty = Double.parseDouble(qtyStr); } catch (Exception ignored) {}
                    }
                    String formattedQty = (qty == (long) qty) ? String.valueOf((long) qty) : String.valueOf(qty);
                    cartModel.setValueAt(formattedQty, row, 2);
                    cartModel.setValueAt(found.getUnitType(), row, 3);
                    
                    String priceType = (String) cmbPriceType.getSelectedItem();
                    double price = calculatePriceWithThreshold(found, priceType, qty);
                    double rawSubtotal = price * qty;
                    long roundedSubtotal = Math.round(rawSubtotal);
                    cartModel.setValueAt(String.format("%.2f", (double) roundedSubtotal), row, 4);
                } else if (nameStr.isEmpty()) {
                    cartModel.setValueAt("", row, 2);
                    cartModel.setValueAt("", row, 3);
                    cartModel.setValueAt("", row, 4);
                }
            } else if (col == 2) { // Qty (col 2)
                String nameStr = getStr(cartModel.getValueAt(row, 1));
                String qtyStr = getStr(cartModel.getValueAt(row, 2));
                String unitStr = getStr(cartModel.getValueAt(row, 3));
                double price = 0;
                if (!nameStr.isEmpty()) {
                    Product p = findProductInCart(nameStr, unitStr);
                    if (p != null) {
                        double qty = 0;
                        try { qty = Double.parseDouble(qtyStr); } catch (Exception ignored) {}
                        String priceType = (String) cmbPriceType.getSelectedItem();
                        price = calculatePriceWithThreshold(p, priceType, qty);
                    }
                }
                double qty = 0;
                try { qty = Double.parseDouble(qtyStr); } catch (Exception ignored) {}
                
                double rawSubtotal = price * qty;
                long roundedSubtotal = Math.round(rawSubtotal);
                if (price > 0 && qty > 0) {
                    cartModel.setValueAt(String.valueOf(row + 1), row, 0);
                    cartModel.setValueAt(String.format("%.2f", (double) roundedSubtotal), row, 4);
                } else if (qtyStr.isEmpty()) {
                    cartModel.setValueAt("", row, 4);
                }
            } else if (col == 4) { // Sub Total manual edit via arrow keys
                String subStr = getStr(cartModel.getValueAt(row, 4));
                if (!subStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(subStr);
                        long rounded = Math.round(val);
                        cartModel.setValueAt(String.format("%.2f", (double) rounded), row, 4);
                    } catch (Exception ignored) {}
                }
            }
            
            recalculateTotal();
        } finally {
            isUpdatingTable = false;
        }
    }

    private String getStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    private void recalculateTotal() {
        double itemsTotal = 0.0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String subStr = getStr(cartModel.getValueAt(i, 4));
            if (!subStr.isEmpty()) {
                try {
                    itemsTotal += Double.parseDouble(subStr);
                } catch (NumberFormatException ignored) {}
            }
        }

        double pendingToAdd = 0.0;
        if (chkAddPrevPending != null && chkAddPrevPending.isSelected() && currentCustomerPending > 0.009) {
            pendingToAdd += currentCustomerPending;
        }
        if (txtPendingPaid != null) {
            String pendPaidStr = txtPendingPaid.getText().trim();
            if (!pendPaidStr.isEmpty()) {
                try {
                    pendingToAdd += Double.parseDouble(pendPaidStr);
                } catch (Exception ignored) {}
            }
        }

        currentTotal = itemsTotal + pendingToAdd;
        double discount = 0.0;
        if (txtDiscount != null) {
            try {
                discount = Double.parseDouble(txtDiscount.getText().trim());
            } catch (Exception ignored) {}
        }
        currentTotal -= discount;
        
        if (lblTotal != null) {
            lblTotal.setText(String.format("Total : \u20B9%.2f", currentTotal));
        }
        updateRemainingBalance();
    }

    private void updateRemainingBalance() {
        if (lblRemaining == null) return;
        double cash = 0.0;
        double gpay = 0.0;
        if (txtCash != null) {
            String cStr = txtCash.getText().trim();
            if (!cStr.isEmpty()) {
                try { cash = Double.parseDouble(cStr); } catch (Exception ignored) {}
            }
        }
        if (txtGPay != null) {
            String gStr = txtGPay.getText().trim();
            if (!gStr.isEmpty()) {
                try { gpay = Double.parseDouble(gStr); } catch (Exception ignored) {}
            }
        }
        double totalPaid = cash + gpay;
        double remaining = currentTotal - totalPaid;

        boolean isCredit = totalPaid <= 0.001 && currentTotal > 0.001;
        String curCustName = "";
        if (cmbCustomer != null && cmbCustomer.getEditor() != null && cmbCustomer.getEditor().getEditorComponent() instanceof JTextField) {
            curCustName = ((JTextField) cmbCustomer.getEditor().getEditorComponent()).getText().trim();
        }
        Customer selectedCust = null;
        Object selObj = cmbCustomer != null ? cmbCustomer.getSelectedItem() : null;
        if (selObj instanceof Customer) selectedCust = (Customer) selObj;
        boolean hasNamedCust = selectedCust != null || (!curCustName.isEmpty() && !"Customer".equalsIgnoreCase(curCustName));

        if (totalPaid <= 0.001) {
            if (currentTotal > 0.001) {
                if (chkAddAsPending != null) {
                    chkAddAsPending.setVisible(true);
                }
                boolean isSel = chkAddAsPending != null && chkAddAsPending.isSelected();
                if (isSel) {
                    lblRemaining.setText(String.format("Pending: \u20B9%.2f [Active in Bill]", currentTotal));
                    lblRemaining.setForeground(new Color(220, 53, 69)); // Red
                } else {
                    lblRemaining.setText(String.format("Remaining: \u20B9%.2f", currentTotal));
                    lblRemaining.setForeground(new Color(180, 100, 0)); // Amber
                }
            } else {
                if (chkAddAsPending != null) {
                    chkAddAsPending.setSelected(false);
                    chkAddAsPending.setVisible(false);
                }
                lblRemaining.setText("Remaining: \u20B90.00");
                lblRemaining.setForeground(Color.GRAY);
            }
        } else if (remaining > 0.009) {
            if (chkAddAsPending != null) {
                chkAddAsPending.setVisible(true);
            }
            boolean isSel = chkAddAsPending != null && chkAddAsPending.isSelected();
            if (isSel) {
                lblRemaining.setText(String.format("Pending: \u20B9%.2f [Active in Bill]", remaining));
                lblRemaining.setForeground(new Color(220, 53, 69)); // Red
            } else {
                lblRemaining.setText(String.format("Remaining: \u20B9%.2f", remaining));
                lblRemaining.setForeground(new Color(180, 100, 0)); // Amber
            }
        } else if (remaining < -0.009) {
            if (chkAddAsPending != null) {
                chkAddAsPending.setSelected(false);
                chkAddAsPending.setVisible(false);
            }
            lblRemaining.setText(String.format("Change / Return: \u20B9%.2f", Math.abs(remaining)));
            lblRemaining.setForeground(new Color(0, 123, 255)); // Blue
        } else {
            if (chkAddAsPending != null) {
                chkAddAsPending.setSelected(false);
                chkAddAsPending.setVisible(false);
            }
            lblRemaining.setText("Remaining: \u20B90.00 (Fully Paid)");
            lblRemaining.setForeground(new Color(40, 167, 69)); // Green
        }
    }
    
    public void loadUpiSettings() {
        boolean wasSelected = chkIncludeUpi.isSelected();
        Object currentUpi = cmbUpiList.getSelectedItem();
        
        cmbUpiList.removeAllItems();
        cmbUpiList.addItem("None");
        String upiString = settingsDAO.getSetting("upi_ids", "");
        if (!upiString.isEmpty()) {
            String[] ids = upiString.split(",");
            for (String id : ids) {
                if (!id.trim().isEmpty() && !"None".equalsIgnoreCase(id.trim())) {
                    cmbUpiList.addItem(id.trim());
                }
            }
        }
        
        if (currentUpi != null) {
            cmbUpiList.setSelectedItem(currentUpi);
        } else {
            cmbUpiList.setSelectedItem("None");
        }
        chkIncludeUpi.setSelected(wasSelected);
    }
    
    public void focusCustomerName() {
        if (cartTable.isEditing()) {
            try {
                cartTable.getCellEditor().stopCellEditing();
            } catch (Exception ignored) {}
        }
        cartTable.clearSelection();
        SwingUtilities.invokeLater(() -> {
            Component custComp = cmbCustomer.getEditor().getEditorComponent();
            if (custComp != null) {
                custComp.requestFocusInWindow();
                if (custComp instanceof JTextField) {
                    ((JTextField) custComp).selectAll();
                }
            } else {
                cmbCustomer.requestFocusInWindow();
            }
        });
    }

    private void setupKeyboardShortcuts() {
        // F2 & F1 -> Jump to Customer Name
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "focusCustomer");
        this.getActionMap().put("focusCustomer", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusCustomerName();
            }
        });
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0), "focusCustomer");

        // F10 -> Jump to Discount Field
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F10, 0), "focusDiscount");
        this.getActionMap().put("focusDiscount", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtDiscount != null) {
                    txtDiscount.requestFocusInWindow();
                    txtDiscount.selectAll();
                }
            }
        });

        // F3 -> Jump to Product Name (Cart Table)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "focusProductSearch");
        this.getActionMap().put("focusProductSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusProductSearch();
            }
        });

        // F6 -> Toggle Add Remaining as Pending in Bill
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "toggleAddAsPending");
        this.getActionMap().put("toggleAddAsPending", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (chkAddAsPending != null && chkAddAsPending.isVisible()) {
                    chkAddAsPending.setSelected(!chkAddAsPending.isSelected());
                    updateRemainingBalance();
                }
            }
        });

        // F7 -> Toggle Add Previous Pending to Total
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "togglePrevPending");
        this.getActionMap().put("togglePrevPending", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (chkAddPrevPending != null) {
                    chkAddPrevPending.setSelected(!chkAddPrevPending.isSelected());
                    recalculateTotal();
                }
            }
        });

        // F8 -> Focus Add/Pay Pending Box
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "focusPendingPaid");
        this.getActionMap().put("focusPendingPaid", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (txtPendingPaid != null) {
                    txtPendingPaid.requestFocusInWindow();
                    txtPendingPaid.selectAll();
                }
            }
        });

        // F9 -> Toggle Include UPI QR Code (Default None)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "toggleUpiQr");
        this.getActionMap().put("toggleUpiQr", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (chkIncludeUpi != null) {
                    boolean newState = !chkIncludeUpi.isSelected();
                    chkIncludeUpi.setSelected(newState);
                    if (newState) {
                        if (cmbUpiList != null && cmbUpiList.getItemCount() > 1) {
                            if ("None".equalsIgnoreCase(String.valueOf(cmbUpiList.getSelectedItem()))) {
                                cmbUpiList.setSelectedIndex(1);
                            }
                            cmbUpiList.requestFocusInWindow();
                            cmbUpiList.showPopup();
                        }
                    } else {
                        if (cmbUpiList != null) {
                            cmbUpiList.setSelectedItem("None");
                        }
                    }
                }
            }
        });

        // F11 -> Direct Print
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0), "directPrint");
        this.getActionMap().put("directPrint", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                processCheckout(true, false);
            }
        });
        
        // F12 -> Checkout & Print (Preview)
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0), "checkout");
        this.getActionMap().put("checkout", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                processCheckout(false, false);
            }
        });
        
        // F5 -> Clear Cart
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0), "clear");
        this.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearCart();
            }
        });

        // Ctrl + S -> Save Only
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK), "saveOnly");
        this.getActionMap().put("saveOnly", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                processCheckout(false, true);
            }
        });
    }

    private void setupTopPanelNavigation() {
        // 1. Customer combo box Enter / Key handling
        Component custComp = cmbCustomer.getEditor().getEditorComponent();
        if (custComp instanceof JTextField) {
            JTextField custTf = (JTextField) custComp;
            
            custTf.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        e.consume();
                        if (cmbCustomer.isPopupVisible() && cmbCustomer.getItemCount() > 0) {
                            Object item = cmbCustomer.getSelectedItem();
                            if (item instanceof String || item == null) {
                                item = cmbCustomer.getItemAt(0);
                                cmbCustomer.setSelectedItem(item);
                            }
                            if (item instanceof Customer) {
                                Customer cust = (Customer) item;
                                String fetchName = (cust.getTamilName() != null && !cust.getTamilName().trim().isEmpty())
                                        ? cust.getTamilName().trim()
                                        : cust.getName();
                                custTf.setText(fetchName);
                            }
                        }
                        cmbCustomer.hidePopup();
                        updateSelectedCustomerPending();
                        cmbItemsPerPage.requestFocusInWindow();
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbCustomer.isPopupVisible()) {
                        focusFirstTableRow();
                    }
                }
                
                @Override
                public void keyReleased(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || 
                        keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
                        return;
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        String text = custTf.getText();
                        java.util.Vector<Object> items = new java.util.Vector<>();
                        List<Customer> allCust = customerDAO.getAllCustomers();
                        
                        if (text.isEmpty()) {
                            items.add("");
                            items.addAll(allCust);
                        } else {
                            List<Customer> matched = com.store.billing.util.CustomerSearchUtil.filter(allCust, text);
                            items.addAll(matched);
                        }
                        
                        if (cmbCustomer.getModel().getSize() != items.size() || !items.isEmpty()) {
                            cmbCustomer.setModel(new DefaultComboBoxModel<>(items));
                            custTf.setText(text);
                        }
                        
                        if (cmbCustomer.getItemCount() > 0) {
                            cmbCustomer.showPopup();
                        } else {
                            cmbCustomer.hidePopup();
                        }
                    });
                }
            });
            
            // Render English, Tamil name and phone in dropdown
            cmbCustomer.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Customer) {
                        setText(com.store.billing.util.CustomerSearchUtil.getDisplayLabel((Customer) value));
                    } else if (value != null) {
                        setText(value.toString());
                    }
                    return this;
                }
            });
        }

        // 2. Items per page combo Enter handling
        cmbItemsPerPage.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (cmbItemsPerPage.isPopupVisible()) {
                        cmbItemsPerPage.setPopupVisible(false);
                    }
                    cmbPrintLanguage.requestFocusInWindow();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbItemsPerPage.isPopupVisible()) {
                    cmbItemsPerPage.showPopup();
                }
            }
        });

        // 3. Print Language combo box Enter handling
        cmbPrintLanguage.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (cmbPrintLanguage.isPopupVisible()) {
                        cmbPrintLanguage.setPopupVisible(false);
                    }
                    cmbPriceType.requestFocusInWindow();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbPrintLanguage.isPopupVisible()) {
                    cmbPrintLanguage.showPopup();
                }
            }
        });

        // 4. Price Type combo box Enter handling -> Jumps to UPI QR or directly to Cart Table!
        cmbPriceType.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (cmbPriceType.isPopupVisible()) {
                        cmbPriceType.setPopupVisible(false);
                    }
                    focusFirstTableRow();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbPriceType.isPopupVisible()) {
                    cmbPriceType.showPopup();
                }
            }
        });

        // 5. Include UPI QR Checkbox Enter handling
        chkIncludeUpi.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (chkIncludeUpi.isSelected()) {
                        cmbUpiList.requestFocusInWindow();
                    } else {
                        focusFirstTableRow();
                    }
                }
            }
        });

        // 6. UPI List combo box Enter handling -> Jumps to Cart Table!
        cmbUpiList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (cmbUpiList.isPopupVisible()) {
                        cmbUpiList.setPopupVisible(false);
                    }
                    focusFirstTableRow();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN && !cmbUpiList.isPopupVisible()) {
                    cmbUpiList.showPopup();
                }
            }
        });

        // 2D Arrow Key Navigation for Top Panel Controls
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { cmbCustomer, cmbItemsPerPage, cmbPrintLanguage },
            { cmbPriceType, chkIncludeUpi, cmbUpiList }
        });
    }

    public void focusProductSearch() {
        focusFirstTableRow();
    }

    private void focusFirstTableRow() {
        Runnable doFocus = () -> {
            if (cartModel.getRowCount() == 0) {
                cartModel.addRow(new Object[]{"1", "", "", "", "", "Del"});
            }
            int targetRow = -1;
            for (int i = 0; i < cartModel.getRowCount(); i++) {
                if (getStr(cartModel.getValueAt(i, 1)).isEmpty()) {
                    targetRow = i;
                    break;
                }
            }
            if (targetRow == -1) {
                cartModel.addRow(new Object[]{String.valueOf(cartModel.getRowCount() + 1), "", "", "", "", "Del"});
                targetRow = cartModel.getRowCount() - 1;
            }
            final int r = targetRow;
            cartTable.changeSelection(r, 1, false, false);
            if (!cartTable.isEditing() || cartTable.getEditingRow() != r || cartTable.getEditingColumn() != 1) {
                cartTable.editCellAt(r, 1);
            }
            Component editor = cartTable.getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
                editor.requestFocusInWindow();
                if (editor instanceof JComboBox) {
                    JComboBox<?> cb = (JComboBox<?>) editor;
                    Component edComp = cb.getEditor().getEditorComponent();
                    if (edComp != null) {
                        edComp.requestFocus();
                        edComp.requestFocusInWindow();
                        if (edComp instanceof JTextField) {
                            ((JTextField) edComp).selectAll();
                        }
                    }
                } else if (editor instanceof JTextField) {
                    ((JTextField) editor).selectAll();
                }
            } else {
                cartTable.requestFocusInWindow();
            }
        };

        SwingUtilities.invokeLater(doFocus);
        Timer timer1 = new Timer(150, e -> doFocus.run());
        timer1.setRepeats(false);
        timer1.start();

        Timer timer2 = new Timer(350, e -> doFocus.run());
        timer2.setRepeats(false);
        timer2.start();
    }
    
    public void refreshProducts() {
        javax.swing.SwingWorker<List<Product>, Void> worker = new javax.swing.SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                return productDAO.getAllProducts();
            }

            @Override
            protected void done() {
                try {
                    allProducts = get();
                    java.util.Map<String, Product> newMap = new java.util.HashMap<>(allProducts.size() * 4);
                    java.util.Vector<String> initialItems = new java.util.Vector<>(50);
                    
                    int count = 0;
                    for (Product p : allProducts) {
                        if (p.getBarcode() != null && !p.getBarcode().isEmpty()) {
                            newMap.put(p.getBarcode().toLowerCase(), p);
                        }
                        if (p.getName() != null && !p.getName().isEmpty()) {
                            newMap.put(p.getName().toLowerCase(), p);
                        }
                        if (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) {
                            newMap.put(p.getDisplayName().toLowerCase(), p);
                        }
                        if (p.getEnglishName() != null && !p.getEnglishName().isEmpty()) {
                            newMap.put(p.getEnglishName().toLowerCase(), p);
                        }
                        if (p.getTamilName() != null && !p.getTamilName().isEmpty()) {
                            newMap.put(p.getTamilName().toLowerCase(), p);
                        }
                        String formatted = formatProductComboItem(p);
                        if (!formatted.isEmpty()) {
                            newMap.put(formatted.toLowerCase(), p);
                            if (count < 50) {
                                initialItems.add(formatted);
                                count++;
                            }
                        }
                    }
                    productMap = newMap;
                    nameCombo.setModel(new javax.swing.DefaultComboBoxModel<>(initialItems));
                    nameCombo.setSelectedItem(null);
                    nameCombo.getEditor().setItem("");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    public void clearCart() {
        if (cartTable.isEditing()) {
            cartTable.getCellEditor().stopCellEditing();
        }
        editingBillId = -1;
        editingDailyBillNo = -1;
        editingBillDate = null;
        if (editBannerPanel != null) editBannerPanel.setVisible(false);
        if (btnCheckout != null) btnCheckout.setText("Preview & Print (F12)");
        if (btnDirectPrint != null) btnDirectPrint.setText("Direct Print (F11)");

        cartModel.setRowCount(0);
        cartModel.addRow(new Object[]{"1", "", "", "", "", "Del"});
        currentTotal = 0.0;
        cmbCustomer.setSelectedItem("");
        txtCash.setText("");
        txtGPay.setText("");
        if (txtDiscount != null) txtDiscount.setText("");
        if (txtPendingPaid != null) txtPendingPaid.setText("");
        if (chkAddPrevPending != null) chkAddPrevPending.setSelected(false);
        if (chkIncludeUpi != null) chkIncludeUpi.setSelected(false);
        if (cmbUpiList != null && cmbUpiList.getItemCount() > 0) cmbUpiList.setSelectedItem("None");
        if (chkAddAsPending != null) {
            chkAddAsPending.setSelected(false);
            chkAddAsPending.setVisible(false);
        }
        updateSelectedCustomerPending();
        updateRemainingBalance();
        
        SwingUtilities.invokeLater(() -> {
            if (cartTable.getRowCount() > 0) {
                cartTable.changeSelection(0, 1, false, false);
                cartTable.editCellAt(0, 1);
                Component editor = cartTable.getEditorComponent();
                if (editor != null) editor.requestFocusInWindow();
            }
        });
        if (billStateChangeListener != null) {
            billStateChangeListener.onBillCleared(this);
        }
    }

    public boolean isEditingExistingBill() {
        return editingBillId > 0;
    }

    public int getEditingBillId() {
        return editingBillId;
    }

    public int getEditingDailyBillNo() {
        return editingDailyBillNo;
    }

    public boolean isDirty() {
        if (isEditingExistingBill()) return true;
        if (cartTable.isEditing()) {
            cartTable.getCellEditor().stopCellEditing();
        }
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String name = getStr(cartModel.getValueAt(i, 1));
            if (!name.trim().isEmpty()) {
                return true;
            }
        }
        Object cust = cmbCustomer.getSelectedItem();
        if (cust != null && !cust.toString().trim().isEmpty()) return true;
        if (txtCash != null && !txtCash.getText().trim().isEmpty()) return true;
        if (txtGPay != null && !txtGPay.getText().trim().isEmpty()) return true;
        return false;
    }

    public interface OnBillStateChangeListener {
        void onBillCleared(BillingPanel panel);
        void onBillSavedOrPrinted(BillingPanel panel, int billId, boolean wasEditing);
        void onEditCancelled(BillingPanel panel);
    }

    private OnBillStateChangeListener billStateChangeListener;

    public void setOnBillStateChangeListener(OnBillStateChangeListener listener) {
        this.billStateChangeListener = listener;
    }

    public void openPrintPreviewDialog() {
        processCheckout(false, false);
    }

    public void loadBillForEditing(Bill bill) {
        if (bill == null) return;
        editingBillId = bill.getId();
        editingDailyBillNo = bill.getDailyBillNo() > 0 ? bill.getDailyBillNo() : bill.getId();
        editingBillDate = bill.getBillDate() != null ? bill.getBillDate() : new Date();
        if (editBannerPanel != null) {
            editBannerPanel.setVisible(true);
            lblEditBannerText.setText("EDITING BILL #" + editingBillId + " - Daily No. " + editingDailyBillNo + " (Modifying Existing Record)");
        }
        if (btnCheckout != null) btnCheckout.setText("Update & Preview (F12)");
        if (btnDirectPrint != null) btnDirectPrint.setText("Update & Print (F11)");

        // Set Customer
        boolean matched = false;
        if (bill.getCustomerName() != null && !bill.getCustomerName().isEmpty()) {
            int custId = bill.getCustomerObj() != null ? bill.getCustomerObj().getId() : -1;
            for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                Object item = cmbCustomer.getItemAt(i);
                if (item instanceof Customer) {
                    Customer c = (Customer) item;
                    if ((custId > 0 && c.getId() == custId) ||
                        c.getName().equalsIgnoreCase(bill.getCustomerName())) {
                        cmbCustomer.setSelectedItem(c);
                        currentCustomerPending = c.getPendingAmount();
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                if (cmbCustomer.getEditor() != null && cmbCustomer.getEditor().getEditorComponent() instanceof JTextField) {
                    ((JTextField) cmbCustomer.getEditor().getEditorComponent()).setText(bill.getCustomerName());
                } else {
                    cmbCustomer.setSelectedItem(bill.getCustomerName());
                }
            }
        } else {
            cmbCustomer.setSelectedItem("");
            currentCustomerPending = 0.0;
        }

        // Set Payment Amounts
        if (bill.getPaidAmount() > 0) {
            if ("GPay".equalsIgnoreCase(bill.getPaymentMode())) {
                txtCash.setText("");
                txtGPay.setText(String.format("%.2f", bill.getPaidAmount()));
            } else {
                txtCash.setText(String.format("%.2f", bill.getPaidAmount()));
                txtGPay.setText("");
            }
        } else {
            txtCash.setText("");
            txtGPay.setText("");
        }
        if (txtDiscount != null) {
            if (bill.getDiscount() > 0.009) {
                txtDiscount.setText(String.format("%.2f", bill.getDiscount()));
            } else {
                txtDiscount.setText("");
            }
        }

        if (chkAddAsPending != null) {
            chkAddAsPending.setSelected(bill.isShowBalanceOnBill() && bill.getPendingAmount() > 0.009);
        }

        // Load Items into cartTable (S.No, Product Name, Qty, Piece, Sub Total, Del)
        isUpdatingTable = true;
        try {
            if (cartTable.isEditing()) {
                cartTable.getCellEditor().stopCellEditing();
            }
            cartModel.setRowCount(0);

            int rowIdx = 1;
            if (bill.getItems() != null) {
                for (BillItem item : bill.getItems()) {
                    String pName = item.getProductName();
                    if (item.getProductId() > 0) {
                        for (Product p : allProducts) {
                            if (p.getId() == item.getProductId()) {
                                pName = (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) ? p.getDisplayName() :
                                        (p.getTamilName() != null && !p.getTamilName().isEmpty()) ? p.getTamilName() : p.getName();
                                break;
                            }
                        }
                    }
                    String qtyStr = (item.getQuantity() == (long) item.getQuantity()) ? String.valueOf((long) item.getQuantity()) : String.valueOf(item.getQuantity());
                    cartModel.addRow(new Object[]{
                            String.valueOf(rowIdx++),
                            pName,
                            qtyStr,
                            item.getUnitType() != null ? item.getUnitType() : "piece",
                            String.format("%.2f", item.getSubtotal()),
                            "Del"
                    });
                }
            }
            // Add blank row at the bottom for adding new products
            cartModel.addRow(new Object[]{String.valueOf(rowIdx), "", "", "", "", "Del"});
        } finally {
            isUpdatingTable = false;
        }

        updateSelectedCustomerPending();
        recalculateTotal();

        SwingUtilities.invokeLater(() -> {
            cartTable.requestFocusInWindow();
            int lastRow = cartModel.getRowCount() - 1;
            cartTable.changeSelection(lastRow, 1, false, false);
            cartTable.editCellAt(lastRow, 1);
            Component editor = cartTable.getEditorComponent();
            if (editor != null) editor.requestFocusInWindow();
        });
    }

    public void loadCustomers() {
        Object currentSelection = cmbCustomer.getSelectedItem();
        String currentText = "";
        if (cmbCustomer.getEditor() != null && cmbCustomer.getEditor().getItem() != null) {
            currentText = cmbCustomer.getEditor().getItem().toString();
        }
        
        java.util.Vector<Object> items = new java.util.Vector<>();
        items.add("");
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            items.add(c);
        }
        cmbCustomer.setModel(new javax.swing.DefaultComboBoxModel<>(items));
        
        // Restore selection
        if (currentSelection instanceof Customer) {
            Customer selectedCust = (Customer) currentSelection;
            boolean found = false;
            for (Object item : items) {
                if (item instanceof Customer && ((Customer) item).getId() == selectedCust.getId()) {
                    cmbCustomer.setSelectedItem(item);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cmbCustomer.setSelectedItem(selectedCust);
            }
        } else if (currentSelection != null && !currentSelection.toString().trim().isEmpty()) {
            cmbCustomer.setSelectedItem(currentSelection);
        } else if (!currentText.trim().isEmpty()) {
            cmbCustomer.setSelectedItem(currentText);
        }
        
        updateSelectedCustomerPending();
    }

    private void updateSelectedCustomerPending() {
        Object sel = cmbCustomer.getSelectedItem();
        if (sel instanceof Customer) {
            Customer c = (Customer) sel;
            currentCustomerPending = Math.max(0.0, c.getPendingAmount());
        } else if (sel != null && !sel.toString().trim().isEmpty()) {
            String name = sel.toString().trim();
            currentCustomerPending = 0.0;
            List<Customer> all = customerDAO.getAllCustomers();
            for (Customer c : all) {
                if (c.getName().equalsIgnoreCase(name) || 
                    (c.getTamilName() != null && c.getTamilName().equalsIgnoreCase(name))) {
                    currentCustomerPending = Math.max(0.0, c.getPendingAmount());
                    break;
                }
            }
        } else {
            currentCustomerPending = 0.0;
        }

        if (lblPrevPending != null) {
            lblPrevPending.setText(String.format("Previous Pending: \u20B9%.2f", currentCustomerPending));
            lblPrevPending.setForeground(currentCustomerPending > 0.009 ? new Color(220, 53, 69) : new Color(40, 167, 69));
        }
        recalculateTotal();
    }

    private void processCheckout(boolean skipPreview, boolean isSaveOnly) {
        if (cartTable.isEditing()) {
            cartTable.getCellEditor().stopCellEditing();
        }
        
        // Remove trailing empty rows if any
        while (cartModel.getRowCount() > 1 && getStr(cartModel.getValueAt(cartModel.getRowCount() - 1, 1)).isEmpty()) {
            cartModel.removeRow(cartModel.getRowCount() - 1);
        }

        List<BillItem> currentItems = new ArrayList<>();
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            String nameStr = getStr(cartModel.getValueAt(i, 1));
            String qtyStr = getStr(cartModel.getValueAt(i, 2));
            String unitStr = getStr(cartModel.getValueAt(i, 3));
            String subStr = getStr(cartModel.getValueAt(i, 4));
            
            if (nameStr.isEmpty()) continue;
            
            try {
                double qty = Double.parseDouble(qtyStr);
                double subtotal = Double.parseDouble(subStr);
                if (qty <= 0) continue;
                double price = 0;
                
                BillItem item = new BillItem();
                item.setProductName(nameStr); // default to whatever is in the cart
                Product p = findProductInCart(nameStr, unitStr);
                if (p != null) {
                    item.setProductName(p.getName()); // Set base name to English name
                    item.setProductId(p.getId());
                    item.setTamilName(p.getTamilName());
                    String priceType = (String) cmbPriceType.getSelectedItem();
                    price = calculatePriceWithThreshold(p, priceType, qty);
                }
                item.setUnitPrice(price);
                item.setUnitType(unitStr);
                item.setQuantity(qty);
                item.setSubtotal(subtotal);
                
                currentItems.add(item);
            } catch (Exception ignored) {}
        }

        // If Previous Pending or manual pending was added to bill total:
        double pendingAdded = 0.0;
        if (chkAddPrevPending != null && chkAddPrevPending.isSelected() && currentCustomerPending > 0.009) {
            pendingAdded += currentCustomerPending;
        }
        if (txtPendingPaid != null && !txtPendingPaid.getText().trim().isEmpty()) {
            try {
                pendingAdded += Double.parseDouble(txtPendingPaid.getText().trim());
            } catch (Exception ignored) {}
        }

        if (pendingAdded > 0.009) {
            BillItem pendingItem = new BillItem();
            pendingItem.setProductName("Previous Pending / \u0BAA\u0BB4\u0BC8\u0BAF \u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF");
            pendingItem.setTamilName("\u0BAA\u0BB4\u0BC8\u0BAF \u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF");
            pendingItem.setQuantity(1);
            pendingItem.setUnitPrice(pendingAdded);
            pendingItem.setSubtotal(pendingAdded);
            pendingItem.setUnitType("");
            currentItems.add(pendingItem);
        }
        
        if (currentItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        Object selectedCustomerObj = cmbCustomer.getSelectedItem();
        String customerName = "";
        Customer customerObj = null;
        
        if (selectedCustomerObj != null) {
            if (selectedCustomerObj instanceof Customer) {
                customerObj = (Customer) selectedCustomerObj;
                String tName = customerObj.getTamilName();
                customerName = (tName != null && !tName.trim().isEmpty()) ? tName.trim() : customerObj.getName();
            } else {
                customerName = selectedCustomerObj.toString().trim();
                if (!customerName.isEmpty()) {
                    for (Customer c : customerDAO.getAllCustomers()) {
                        if ((c.getTamilName() != null && c.getTamilName().equalsIgnoreCase(customerName)) ||
                            c.getName().equalsIgnoreCase(customerName)) {
                            customerObj = c;
                            String tName = c.getTamilName();
                            if (tName != null && !tName.trim().isEmpty()) {
                                customerName = tName.trim();
                            }
                            break;
                        }
                    }
                }
            }
        }

        String txtCashVal = txtCash.getText().trim();
        String txtGPayVal = txtGPay.getText().trim();
        
        double cashReceived = 0;
        try { cashReceived = Double.parseDouble(txtCashVal); } catch(Exception ignored) {}
        double gpayReceived = 0;
        try { gpayReceived = Double.parseDouble(txtGPayVal); } catch(Exception ignored) {}
        
        double totalReceived = cashReceived + gpayReceived;
        boolean hasEnteredPayment = (cashReceived > 0.009 || gpayReceived > 0.009);
        boolean isPendingSelected = chkAddAsPending != null && chkAddAsPending.isSelected();

        String paymentMode = "Cash";
        double paidAmount = 0.0;
        double pendingBalance = 0.0;
        boolean showBalanceOnBill = false;

        if (!hasEnteredPayment) {
            // Amount NOT entered -> Unpaid / Credit bill: amount always goes to customer pending in DB!
            paymentMode = "Credit (Unpaid)";
            paidAmount = 0.0;
            pendingBalance = currentTotal;
            cashReceived = 0.0;
            gpayReceived = 0.0;
            // On the printed bill, show Balance ONLY if user explicitly checked "Add Remaining as Pending in Bill"
            showBalanceOnBill = isPendingSelected;
        } else {
            // Amount WAS entered (Cash and/or GPay)
            if (cashReceived > 0.009 && gpayReceived > 0.009) {
                paymentMode = "Cash / GPay"; 
            } else if (gpayReceived > 0.009) {
                paymentMode = "GPay";
            } else {
                paymentMode = "Cash";
            }

            paidAmount = totalReceived;
            double remainingAmount = Math.max(0, currentTotal - totalReceived);

            if (isPendingSelected && remainingAmount > 0.009) {
                // Rule 3: Partial payment, remaining added as pending
                pendingBalance = remainingAmount;
                showBalanceOnBill = true;
            } else {
                // Rule 4: Full payment (or remaining not added as pending)
                pendingBalance = 0.0;
                showBalanceOnBill = false;
            }
        }
        
        if (pendingBalance > 0.01) {
            if (customerObj == null) {
                if (!customerName.isEmpty() && !"Customer".equalsIgnoreCase(customerName)) {
                    Customer newCust = new Customer();
                    newCust.setName(customerName);
                    int newId = customerDAO.addCustomerAndGetId(newCust);
                    if (newId != -1) {
                        newCust.setId(newId);
                        customerObj = newCust;
                    }
                }
            }
        }
        
        double discountAmount = 0.0;
        if (txtDiscount != null) {
            try { discountAmount = Double.parseDouble(txtDiscount.getText().trim()); } catch (Exception ignored) {}
        }

        Bill bill = new Bill();
        bill.setCustomerName(customerName);
        bill.setCustomerObj(customerObj);
        bill.setPaymentMode(paymentMode);
        bill.setTotalAmount(currentTotal);
        bill.setDiscount(discountAmount);
        bill.setPaidAmount(paidAmount);
        bill.setPendingAmount(pendingBalance);
        bill.setCashAmount(cashReceived);
        bill.setGpayAmount(gpayReceived);
        bill.setItems(currentItems);
        bill.setShowBalanceOnBill(showBalanceOnBill);

        // Set status
        bill.setStatus(isSaveOnly ? "Saved" : "Printed");

        if (editingBillId > 0) {
            bill.setId(editingBillId);
            int dNo = editingDailyBillNo > 0 ? editingDailyBillNo : editingBillId;
            Date bDate = editingBillDate != null ? editingBillDate : new Date();
            Bill oldB = billDAO.getBillById(editingBillId);
            if (oldB != null) {
                if (oldB.getDailyBillNo() > 0) dNo = oldB.getDailyBillNo();
                if (oldB.getBillDate() != null) bDate = oldB.getBillDate();
            }
            if (dNo <= 0) dNo = editingBillId;
            bill.setDailyBillNo(dNo);
            bill.setBillDate(bDate);
        } else {
            Date now = new Date();
            bill.setBillDate(now);
            bill.setDailyBillNo(billDAO.getNextDailyBillNo(now));
        }

        if (isSaveOnly) {
            boolean success = false;
            double oldPendingOnEdit = -1;
            if (editingBillId > 0) {
                Bill oldB = billDAO.getBillById(editingBillId);
                if (oldB != null) oldPendingOnEdit = oldB.getPendingAmount();
                success = billDAO.updateBill(bill);
            } else {
                success = billDAO.createBill(bill);
            }
            if (success) {
                updateCustomerPendingBalance(bill, oldPendingOnEdit);
                boolean wasEditing = isEditingExistingBill();
                int savedId = bill.getId();
                if (editingBillId > 0) {
                    JOptionPane.showMessageDialog(this, "Bill #" + editingBillId + " updated successfully (Not Printed)!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Bill saved successfully (Not Printed)!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                clearCart();
                loadCustomers();
                if (billStateChangeListener != null) {
                    billStateChangeListener.onBillSavedOrPrinted(this, savedId, wasEditing);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save bill. Please check DB connection.");
            }
            return;
        }

        if (skipPreview) {
            // Direct Print (F11): Save or Update DB and print immediately
            boolean success = false;
            double oldPendingOnEdit = -1;
            if (editingBillId > 0) {
                Bill oldB = billDAO.getBillById(editingBillId);
                if (oldB != null) oldPendingOnEdit = oldB.getPendingAmount();
                success = billDAO.updateBill(bill);
            } else {
                success = billDAO.createBill(bill);
            }
            if (success) {
                updateCustomerPendingBalance(bill, oldPendingOnEdit);
                boolean wasEditing = isEditingExistingBill();
                int savedId = bill.getId();
                if (editingBillId > 0) {
                    JOptionPane.showMessageDialog(this, "Bill #" + editingBillId + " updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                printReceipt(bill, currentItems, true);
                clearCart();
                loadCustomers(); // Refresh customer balances
                if (billStateChangeListener != null) {
                    billStateChangeListener.onBillSavedOrPrinted(this, savedId, wasEditing);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to " + (editingBillId > 0 ? "update" : "generate") + " bill. Please check DB connection.");
            }
        } else {
            // Preview & Print (F12)
            if (editingBillId <= 0) {
                bill.setId(billDAO.getNextBillId());
            }
            printReceipt(bill, currentItems, false);
        }
    }
    
    private void printReceipt(Bill bill, List<BillItem> currentItems, boolean skipPreview) {
        String shopName = settingsDAO.getSetting("shop_name", "MUSTAFA STORES");
        String shopSince = settingsDAO.getSetting("shop_since", "");
        String shopAddress = settingsDAO.getSetting("shop_address", "");
        String shopPhone = settingsDAO.getSetting("shop_phone", "");
        
        int itemsPerPage;
        if ("None".equals(cmbItemsPerPage.getSelectedItem())) {
            itemsPerPage = Math.max(1, currentItems.size());
        } else {
            itemsPerPage = Integer.parseInt(cmbItemsPerPage.getSelectedItem().toString());
        }
        int totalPages = (int) Math.ceil((double) currentItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        boolean printTamil = "Tamil".equals(cmbPrintLanguage.getSelectedItem().toString());
        String baseFontSize = printTamil ? "54px" : "44px";
        
        String thNo = "<b>S.No</b>";
        String thItem = printTamil ? "<b><span style='font-size: 34px; font-weight: 900;'>\u0BAA\u0BCA\u0BB0\u0BC1\u0BB3\u0BCD</span></b>" : "<b>Item</b>";
        String thQty = printTamil ? "<b><span style='font-size: 34px; font-weight: 900;'>\u0B85\u0BB3\u0BB5\u0BC1</span></b>" : "<b>Qty</b>";
        String thTotal = printTamil ? "<b><span style='font-size: 34px; font-weight: 900;'>\u0BAE\u0BA4\u0BBF\u0BAA\u0BCD\u0BAA\u0BC1</span></b>" : "<b>Total</b>";

        String lblTot = printTamil ? "\u0BAE\u0BCA\u0BA4\u0BCD\u0BA4\u0BAE\u0BCD:" : "Tot:";
        String lblDiscount = printTamil ? "\u0B95\u0BB4\u0BBF\u0BA4\u0BCD\u0BA4\u0BB2\u0BCD:" : "Discount:";
        String lblNetTot = printTamil ? "\u0BAE\u0BC0\u0BA4\u0BBF \u0BA4\u0BCA\u0B95\u0BC8:" : "Net Tot:";
        String lblCash = printTamil ? "\u0BB5\u0BB0\u0BB5\u0BC1:" : "Cash:";
        String lblBalance = printTamil ? "\u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF:" : "Balance:";

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm a");
        String dateStr = dateFormat.format(bill.getBillDate() != null ? bill.getBillDate() : new java.util.Date());
        int displayNo = bill.getDailyBillNo() > 0 ? bill.getDailyBillNo() : (bill.getId() > 0 ? bill.getId() : 1);
        String noStr = "No. " + displayNo;

        List<JPanel> printReceiptPanels = new ArrayList<>();
        JTabbedPane tabbedPane = new JTabbedPane();

        for (int p = 0; p < totalPages; p++) {
            String pageStr = "Page " + (p + 1) + " / " + totalPages;

            // =========================================================================
            // 1. BUILD PHYSICAL PRINT PANEL (For Thermal Printer, UNTOUCHED)
            // =========================================================================
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: sans-serif; font-size: ").append(baseFontSize).append("; margin: 0px; padding: 0px;'>");
            html.append("<div style='text-align: center;'>");
            html.append("<b><font size='6' color='black'>").append(shopName.toUpperCase()).append("</font></b><br>");
            if (shopSince != null && !shopSince.trim().isEmpty()) {
                String sinceStr = shopSince.trim().toLowerCase().startsWith("since") ? shopSince.trim() : "Since " + shopSince.trim();
                html.append("<b><span style='font-size: 38px; color: black;'>").append(sinceStr).append("</span></b><br>");
            }
            if (shopAddress != null && !shopAddress.trim().isEmpty()) {
                html.append("<span style='font-weight: bold;'>").append(shopAddress).append("</span><br>");
            }
            if (shopPhone != null && !shopPhone.trim().isEmpty()) {
                html.append("<span style='font-weight: bold;'>Phone No: ").append(shopPhone).append("</span><br>");
            }
            
            html.append("<b style='font-size: 44px; font-weight: 900; color: black;'>QUOTATION</b><br>");
            html.append("</div><hr style='border: 1px dashed black;'>");
            
            html.append("<table width='100%' cellpadding='0' cellspacing='0' style='font-size: ").append(baseFontSize).append("; font-weight: normal;'>");
            html.append("<tr>");
            html.append("<td width='26%' align='left'><b>").append(noStr).append("</b></td>");
            html.append("<td width='48%' align='center'><b>").append(dateStr).append("</b></td>");
            html.append("<td width='26%' align='right'><b>").append(pageStr).append("</b></td>");
            html.append("</tr>");
            html.append("</table>");
            
            html.append("<div style='margin-top: 5px;'><b>To: ").append(bill.getCustomerName().isEmpty() ? "Customer" : bill.getCustomerName()).append("</b><br>");
            if (bill.getCustomerObj() != null) {
                if (bill.getCustomerObj().getMobile() != null && !bill.getCustomerObj().getMobile().isEmpty()) {
                    html.append("&nbsp;&nbsp;&nbsp;&nbsp;<b>Ph: ").append(bill.getCustomerObj().getMobile()).append("</b><br>");
                }
                if (bill.getCustomerObj().getAddress() != null && !bill.getCustomerObj().getAddress().isEmpty()) {
                    html.append("&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(bill.getCustomerObj().getAddress()).append("</b><br>");
                }
            }
            html.append("</div><hr style='border: 1px dashed black;'>");
            
            html.append("<table width='100%' cellspacing='0' cellpadding='3' style='font-size: ").append(baseFontSize).append(";'>");
            html.append("<tr>");
            html.append("<th align='center' width='120' style='font-weight: 900; color: black;'>").append(thNo).append("</th>");
            html.append("<th align='left' width='100%' style='font-weight: bold; color: black; padding-left: 5px;'>").append(thItem).append("</th>");
            html.append("<th align='left' nowrap style='padding-left: 5px; padding-right: 30px; font-weight: bold; color: black;'>").append(thQty).append("</th>");
            html.append("<th align='right' nowrap style='font-weight: bold; color: black;'>").append(thTotal).append("</th>");
            html.append("</tr>");
            html.append("<tr><td colspan='4'><hr style='border: 1px dashed black;'></td></tr>");
            
            int start = p * itemsPerPage;
            int end = Math.min(start + itemsPerPage, currentItems.size());
            
            for (int i = start; i < end; i++) {
                BillItem item = currentItems.get(i);
                String itemName = item.getProductName();
                if (printTamil && item.getTamilName() != null && !item.getTamilName().trim().isEmpty()) {
                    itemName = item.getTamilName();
                }
                
                boolean isPendingLine = itemName.contains("Pending") || itemName.contains("\u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF");
                String qtyUnit = isPendingLine ? "" : ThermalPrinterUtil.formatQtyUnit(item.getQuantity(), item.getUnitType(), printTamil);
                
                html.append("<tr>");
                html.append("<td align='right' valign='top' width='120' style='padding-right: 6px;'><b>").append(i + 1).append(".</b></td>");
                html.append("<td align='left' valign='top' width='100%' style='word-break: break-word; padding-left: 5px;'><b>").append(itemName).append("</b></td>");
                html.append("<td align='left' valign='top' nowrap style='padding-left: 5px; padding-right: 30px;'><b>").append(qtyUnit).append("</b></td>");
                html.append("<td align='right' valign='top' nowrap><b>").append(String.format("%.2f", item.getSubtotal())).append("</b></td>");
                html.append("</tr>");
            }
            
            html.append("</table>");
            
            JPanel printReceiptPanel = new JPanel(new BorderLayout());
            printReceiptPanel.setPreferredSize(new Dimension(460, 560));
            
            boolean hasQr = chkIncludeUpi.isSelected() && cmbUpiList.getSelectedItem() != null && !"None".equalsIgnoreCase(cmbUpiList.getSelectedItem().toString());

            if (p == totalPages - 1) {
                html.append("<hr style='border: 1px dashed black;'>");
                html.append("<table width='100%' cellpadding='2' cellspacing='0' style='font-size: 36px;'>");

                // Show Tot always
                html.append("<tr>");
                html.append("<td colspan='2' align='right' valign='middle' nowrap>");
                if (bill.getDiscount() > 0.009) {
                    html.append("<b><font size='6' color='black'>").append(lblTot).append(" ").append(String.format("%.2f", bill.getTotalAmount() + bill.getDiscount())).append("</font></b>");
                } else {
                    html.append("<b><font size='6' color='black'>").append(lblTot).append(" ").append(String.format("%.2f", bill.getTotalAmount())).append("</font></b>");
                }
                html.append("</td>");
                html.append("</tr>");

                if (bill.getDiscount() > 0.009) {
                    html.append("<tr>");
                    html.append("<td colspan='2' align='right' valign='middle' nowrap><b><font size='5' color='black'>").append(lblDiscount).append(" ").append(String.format("%.2f", bill.getDiscount())).append("</font></b></td>");
                    html.append("</tr>");
                    html.append("<tr>");
                    html.append("<td colspan='2' align='right' valign='middle' nowrap><b><font size='6' color='black'>").append(lblNetTot).append(" ").append(String.format("%.2f", bill.getTotalAmount())).append("</font></b></td>");
                    html.append("</tr>");
                }

                // Show Cash only if partial payment was entered and balance exists (Rule 3)
                boolean hasEnteredAmount = (bill.getCashAmount() > 0.009 || bill.getGpayAmount() > 0.009);
                if (hasEnteredAmount && bill.getPaidAmount() > 0.009 && bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
                    html.append("<tr>");
                    html.append("<td colspan='2' align='right' valign='middle' nowrap><b><font size='5'>").append(lblCash).append(" ").append(String.format("%.2f", bill.getPaidAmount())).append("</font></b></td>");
                    html.append("</tr>");
                }
                // Show Balance only if pending exists AND showBalanceOnBill is true
                if (bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
                    html.append("<tr>");
                    html.append("<td colspan='2' align='right' valign='middle' nowrap><b><font size='5'>").append(lblBalance).append(" ").append(String.format("%.2f", bill.getPendingAmount())).append("</font></b></td>");
                    html.append("</tr>");
                }

                html.append("</table>");
                html.append("<hr style='border: 1px dashed black;'>");
                
                html.append("<div style='text-align: center; margin-top: 5px; font-size: 36px;'><b>*** THANK YOU! VISIT AGAIN ***</b></div>");
                html.append("</body></html>");
                
                JTextPane textPane = new JTextPane();
                textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                textPane.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
                textPane.setContentType("text/html");
                textPane.setText(html.toString());
                textPane.setEditable(false);
                JScrollPane sp = new JScrollPane(textPane);
                sp.setBorder(null);
                printReceiptPanel.add(sp, BorderLayout.CENTER);

                if (hasQr) {
                    String upiId = cmbUpiList.getSelectedItem().toString();
                    boolean includeAmount = bill.getGpayAmount() > 0.009;
                    double qrAmount = bill.getGpayAmount();
                    ImageIcon qrIcon = com.store.billing.util.QRCodeGenerator.generateUPIQRCode(upiId, shopName, qrAmount, includeAmount, 200, 200);
                    if (qrIcon != null) {
                        JPanel bottomQrPanel = new JPanel(new BorderLayout());
                        bottomQrPanel.setBackground(Color.WHITE);

                        JLabel qrLabel = new JLabel(qrIcon);
                        qrLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                        qrLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                        
                        bottomQrPanel.add(qrLabel, BorderLayout.EAST);
                        printReceiptPanel.add(bottomQrPanel, BorderLayout.SOUTH);
                    }
                }
            } else {
                html.append("</table>");
                html.append("<div style='text-align: center; margin-top: 10px; font-style: italic;'>--- Continued on next page ---</div>");
                html.append("</body></html>");
                
                JTextPane textPane = new JTextPane();
                textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                textPane.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
                textPane.setContentType("text/html");
                textPane.setText(html.toString());
                textPane.setEditable(false);
                JScrollPane sp = new JScrollPane(textPane);
                sp.setBorder(null);
                printReceiptPanel.add(sp, BorderLayout.CENTER);
            }
            
            printReceiptPanels.add(printReceiptPanel);

            // =========================================================================
            // 2. BUILD CLEAN SCREEN PREVIEW PANEL (Specifically for User Monitor)
            // =========================================================================
            if (!skipPreview) {
                StringBuilder previewHtml = new StringBuilder();
                previewHtml.append("<html><body style='font-family: \"Nirmala UI\", \"Segoe UI\", Arial, sans-serif; font-size: 13px; margin: 10px; padding: 0px; color: #111;'>");
                previewHtml.append("<div style='text-align: center;'>");
                previewHtml.append("<b><span style='font-size: 18px; color: black;'>").append(shopName.toUpperCase()).append("</span></b><br>");
                if (shopSince != null && !shopSince.trim().isEmpty()) {
                    String sinceStr = shopSince.trim().toLowerCase().startsWith("since") ? shopSince.trim() : "Since " + shopSince.trim();
                    previewHtml.append("<b><span style='font-size: 13px; color: #333;'>").append(sinceStr).append("</span></b><br>");
                }
                if (shopAddress != null && !shopAddress.trim().isEmpty()) {
                    previewHtml.append("<span style='font-size: 12px; font-weight: bold;'>").append(shopAddress).append("</span><br>");
                }
                if (shopPhone != null && !shopPhone.trim().isEmpty()) {
                    previewHtml.append("<span style='font-size: 12px; font-weight: bold;'>Phone No: ").append(shopPhone).append("</span><br>");
                }
                previewHtml.append("<b style='font-size: 14px; font-weight: 900; color: black;'>QUOTATION</b><br>");
                previewHtml.append("<hr style='border-top: 1px dashed black; margin: 4px 0;'>");
                previewHtml.append("</div>");

                previewHtml.append("<table width='100%' cellpadding='1' cellspacing='0' style='font-size: 12px;'>");
                previewHtml.append("<tr>");
                previewHtml.append("<td width='26%' align='left'><b>").append(noStr).append("</b></td>");
                previewHtml.append("<td width='48%' align='center'><b>").append(dateStr).append("</b></td>");
                previewHtml.append("<td width='26%' align='right'><b>").append(pageStr).append("</b></td>");
                previewHtml.append("</tr>");
                previewHtml.append("</table>");

                previewHtml.append("<div style='margin-top: 4px; font-size: 12px;'><b>To: ").append(bill.getCustomerName().isEmpty() ? "Customer" : bill.getCustomerName()).append("</b><br>");
                if (bill.getCustomerObj() != null) {
                    if (bill.getCustomerObj().getMobile() != null && !bill.getCustomerObj().getMobile().isEmpty()) {
                        previewHtml.append("&nbsp;&nbsp;&nbsp;&nbsp;<b>Ph: ").append(bill.getCustomerObj().getMobile()).append("</b><br>");
                    }
                    if (bill.getCustomerObj().getAddress() != null && !bill.getCustomerObj().getAddress().isEmpty()) {
                        previewHtml.append("&nbsp;&nbsp;&nbsp;&nbsp;<b>").append(bill.getCustomerObj().getAddress()).append("</b><br>");
                    }
                }
                previewHtml.append("</div><hr style='border-top: 1px dashed black; margin: 4px 0;'>");

                previewHtml.append("<table width='100%' cellspacing='0' cellpadding='2' style='font-size: 13px;'>");
                previewHtml.append("<tr>");
                previewHtml.append("<th align='left' width='38' nowrap style='font-weight: bold; color: black;'>S.No</th>");
                previewHtml.append("<th align='left' width='160' style='font-weight: bold; color: black; padding-left: 5px;'>").append(printTamil ? "பொருள்" : "Item").append("</th>");
                previewHtml.append("<th align='left' width='80' nowrap style='font-weight: bold; color: black; padding-left: 5px;'>").append(printTamil ? "அளவு" : "Qty").append("</th>");
                previewHtml.append("<th align='right' width='60' nowrap style='font-weight: bold; color: black;'>").append(printTamil ? "மதிப்பு" : "Total").append("</th>");
                previewHtml.append("</tr>");
                previewHtml.append("<tr><td colspan='4'><hr style='border-top: 1px dashed black; margin: 2px 0;'></td></tr>");

                for (int i = start; i < end; i++) {
                    BillItem item = currentItems.get(i);
                    String itemName = item.getProductName();
                    if (printTamil && item.getTamilName() != null && !item.getTamilName().trim().isEmpty()) {
                        itemName = item.getTamilName();
                    }
                    boolean isPendingLine = itemName.contains("Pending") || itemName.contains("\u0BAA\u0BBE\u0B95\u0BCD\u0B95\u0BBF");
                    String qtyUnit = isPendingLine ? "" : ThermalPrinterUtil.formatQtyUnit(item.getQuantity(), item.getUnitType(), printTamil);

                    previewHtml.append("<tr>");
                    previewHtml.append("<td align='left' valign='top' nowrap><b>").append(i + 1).append(".</b></td>");
                    previewHtml.append("<td align='left' valign='top' style='word-break: break-word; padding-left: 5px;'><b>").append(itemName).append("</b></td>");
                    previewHtml.append("<td align='left' valign='top' nowrap style='padding-left: 5px;'><b>").append(qtyUnit).append("</b></td>");
                    previewHtml.append("<td align='right' valign='top' nowrap><b>").append(String.format("%.2f", item.getSubtotal())).append("</b></td>");
                    previewHtml.append("</tr>");
                }
                previewHtml.append("</table>");

                JPanel receiptCard = new JPanel(new BorderLayout());
                receiptCard.setBackground(Color.WHITE);
                receiptCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 205, 210), 1),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));

                if (p == totalPages - 1) {
                    previewHtml.append("<hr style='border-top: 1px dashed black; margin: 4px 0;'>");
                    previewHtml.append("<table width='100%' cellpadding='2' cellspacing='0' style='font-size: 13px;'>");

                    // Show Tot always
                    previewHtml.append("<tr>");
                    if (bill.getDiscount() > 0.009) {
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 16px; color: black;'>").append(lblTot).append(" ").append(String.format("%.2f", bill.getTotalAmount() + bill.getDiscount())).append("</b></td>");
                    } else {
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 16px; color: black;'>").append(lblTot).append(" ").append(String.format("%.2f", bill.getTotalAmount())).append("</b></td>");
                    }
                    previewHtml.append("</tr>");

                    if (bill.getDiscount() > 0.009) {
                        previewHtml.append("<tr>");
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 14px; color: black;'>").append(lblDiscount).append(" ").append(String.format("%.2f", bill.getDiscount())).append("</b></td>");
                        previewHtml.append("</tr>");
                        previewHtml.append("<tr>");
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 16px; color: black;'>").append(lblNetTot).append(" ").append(String.format("%.2f", bill.getTotalAmount())).append("</b></td>");
                        previewHtml.append("</tr>");
                    }

                    // Show Cash only if partial payment was entered and balance exists (Rule 3)
                    boolean hasEnteredAmountPrev = (bill.getCashAmount() > 0.009 || bill.getGpayAmount() > 0.009);
                    if (hasEnteredAmountPrev && bill.getPaidAmount() > 0.009 && bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
                        previewHtml.append("<tr>");
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 14px;'>").append(lblCash).append(" ").append(String.format("%.2f", bill.getPaidAmount())).append("</b></td>");
                        previewHtml.append("</tr>");
                    }
                    // Show Balance only if pending exists AND showBalanceOnBill is true
                    if (bill.getPendingAmount() > 0.009 && bill.isShowBalanceOnBill()) {
                        previewHtml.append("<tr>");
                        previewHtml.append("<td colspan='2' align='right' valign='middle' nowrap><b style='font-size: 14px;'>").append(lblBalance).append(" ").append(String.format("%.2f", bill.getPendingAmount())).append("</b></td>");
                        previewHtml.append("</tr>");
                    }

                    previewHtml.append("</table>");
                    previewHtml.append("<hr style='border-top: 1px dashed black; margin: 4px 0;'>");
                    previewHtml.append("<div style='text-align: center; margin-top: 5px; font-size: 12px; font-weight: bold;'>*** THANK YOU! VISIT AGAIN ***</div>");
                    previewHtml.append("</body></html>");
                } else {
                    previewHtml.append("</table>");
                    previewHtml.append("<div style='text-align: center; margin-top: 10px; font-style: italic; font-size: 12px;'>--- Continued on next page ---</div>");
                    previewHtml.append("</body></html>");
                }

                JTextPane previewTp = new JTextPane();
                previewTp.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
                previewTp.setFont(new Font("Nirmala UI", Font.PLAIN, 13));
                previewTp.setContentType("text/html");
                previewTp.setText(previewHtml.toString());
                previewTp.setEditable(false);
                previewTp.setBackground(Color.WHITE);
                previewTp.setCaretPosition(0);
                receiptCard.add(previewTp, BorderLayout.CENTER);

                if (p == totalPages - 1 && hasQr) {
                    String upiId = cmbUpiList.getSelectedItem().toString();
                    boolean includeAmount = bill.getGpayAmount() > 0.009;
                    double qrAmount = bill.getGpayAmount();
                    ImageIcon qrIcon = com.store.billing.util.QRCodeGenerator.generateUPIQRCode(upiId, shopName, qrAmount, includeAmount, 150, 150);
                    if (qrIcon != null) {
                        JPanel bottomQrPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
                        bottomQrPanel.setBackground(Color.WHITE);
                        bottomQrPanel.add(new JLabel(qrIcon));
                        receiptCard.add(bottomQrPanel, BorderLayout.SOUTH);
                    }
                }

                int cardWidth = 400;
                previewTp.setSize(cardWidth - 20, Short.MAX_VALUE);
                Dimension cardLayoutSize = receiptCard.getLayout().preferredLayoutSize(receiptCard);
                receiptCard.setPreferredSize(new Dimension(cardWidth, cardLayoutSize.height));

                JPanel cardWrapper = new JPanel(new GridBagLayout());
                cardWrapper.setBackground(new Color(240, 242, 245));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.weightx = 1.0;
                gbc.weighty = 0.0;
                gbc.anchor = GridBagConstraints.NORTH;
                gbc.insets = new Insets(15, 15, 25, 15);
                cardWrapper.add(receiptCard, gbc);

                JScrollPane previewScroll = new JScrollPane(cardWrapper);
                previewScroll.setBorder(null);
                previewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                previewScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                previewScroll.getVerticalScrollBar().setUnitIncrement(20);
                SwingUtilities.invokeLater(() -> previewScroll.getVerticalScrollBar().setValue(0));

                tabbedPane.addTab("Page " + (p + 1), previewScroll);
            }
        }

        if (skipPreview) {
            PrintService defaultService = null;
            String printerName = settingsDAO.getSetting("default_printer", "None (Show System Dialog)");
            if (!printerName.equals("None (Show System Dialog)")) {
                PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                for (PrintService service : services) {
                    if (service.getName().equals(printerName)) {
                        defaultService = service;
                        break;
                    }
                }
            }
            if (defaultService == null) {
                JOptionPane.showMessageDialog(this, "No default printer set for Direct Print. Please configure in Settings or use Preview & Print (F12).", "Printer Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            final PrintService finalDefaultService = defaultService;
            new Thread(() -> {
                try {
                    // ALWAYS use Image Printing so Tamil shop names and headers render perfectly, avoiding ESC/POS corruption and wrapping bugs.
                    for (int i = 0; i < printReceiptPanels.size(); i++) {
                        ThermalPrinterUtil.printReceiptPanel(printReceiptPanels.get(i), finalDefaultService);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Print Receipt Preview - Bill #" + displayNo, true);
        dialog.setLayout(new BorderLayout());
        dialog.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel dialogBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnPrint = new JButton(editingBillId > 0 ? "Update & Print Bill (F11)" : "Print Bill (F11)");
        btnPrint.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnPrint.setBackground(new Color(40, 167, 69));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> {
            try {
                // 1. SAVE OR UPDATE BILL TO DATABASE ONLY UPON PRINT
                double oldPendingOnEdit = -1;
                if (editingBillId > 0) {
                    Bill oldB = billDAO.getBillById(editingBillId);
                    if (oldB != null) oldPendingOnEdit = oldB.getPendingAmount();
                    bill.setId(editingBillId);
                    if (!billDAO.updateBill(bill)) {
                        JOptionPane.showMessageDialog(dialog, "Failed to update bill #" + editingBillId + ". Please check DB connection.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    JOptionPane.showMessageDialog(dialog, "Bill #" + editingBillId + " updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    if (!billDAO.createBill(bill)) {
                        JOptionPane.showMessageDialog(dialog, "Failed to save bill. Please check DB connection.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                updateCustomerPendingBalance(bill, oldPendingOnEdit);

                // 2. PRINT RECEIPT
                PrintService targetService = null;
                String printerName = settingsDAO.getSetting("default_printer", "None (Show System Dialog)");
                if (!printerName.equals("None (Show System Dialog)")) {
                    PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                    for (PrintService service : services) {
                        if (service.getName().equals(printerName)) {
                            targetService = service;
                            break;
                        }
                    }
                }
                if (targetService == null) {
                    targetService = PrintServiceLookup.lookupDefaultPrintService();
                }
                
                final PrintService finalTargetService = targetService;
                if (finalTargetService != null) {
                    new Thread(() -> {
                        try {
                            for (int i = 0; i < printReceiptPanels.size(); i++) {
                                ThermalPrinterUtil.printReceiptPanel(printReceiptPanels.get(i), finalTargetService);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }).start();
                } else {
                    JOptionPane.showMessageDialog(dialog, "No default printer configured. Please select in Settings.", "Printer Not Found", JOptionPane.WARNING_MESSAGE);
                }

                // 3. CLEAR CART ONLY AFTER PRINTING
                boolean wasEditing = isEditingExistingBill();
                int savedId = bill.getId();
                clearCart();
                loadCustomers();
                dialog.dispose();
                if (billStateChangeListener != null) {
                    billStateChangeListener.onBillSavedOrPrinted(BillingPanel.this, savedId, wasEditing);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton btnClose = new JButton("Close (Esc)");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnClose.addActionListener(e -> dialog.dispose());
        
        dialogBtnPanel.add(btnPrint);
        dialogBtnPanel.add(btnClose);
        dialog.add(dialogBtnPanel, BorderLayout.SOUTH);

        // Keyboard Shortcuts on Preview Dialog:
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "dialogPrint");
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dialogPrint");
        dialog.getRootPane().getActionMap().put("dialogPrint", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnPrint.doClick();
            }
        });

        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialogClose");
        dialog.getRootPane().getActionMap().put("dialogClose", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dialog.dispose();
            }
        });
        
        dialog.getRootPane().setDefaultButton(btnPrint);
        
        dialog.setSize(520, 750);
        dialog.setMinimumSize(new Dimension(480, 650));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateCustomerPendingBalance(Bill bill, double oldPendingOnEdit) {
        int custId = -1;
        if (bill.getCustomerObj() != null) {
            custId = bill.getCustomerObj().getId();
        } else if (bill.getCustomerName() != null && !bill.getCustomerName().trim().isEmpty()) {
            List<Customer> all = customerDAO.getAllCustomers();
            for (Customer c : all) {
                if (c.getName().equalsIgnoreCase(bill.getCustomerName().trim()) ||
                    (c.getTamilName() != null && c.getTamilName().equalsIgnoreCase(bill.getCustomerName().trim()))) {
                    custId = c.getId();
                    break;
                }
            }
        }
        if (custId <= 0) return;
        
        if (editingBillId > 0) {
            if (oldPendingOnEdit >= 0) {
                double diff = bill.getPendingAmount() - oldPendingOnEdit;
                if (Math.abs(diff) > 0.001) {
                    customerDAO.updatePendingAmount(custId, diff);
                }
            }
        } else {
            if (chkAddPrevPending != null && chkAddPrevPending.isSelected() && currentCustomerPending > 0.009) {
                customerDAO.setPendingAmount(custId, bill.getPendingAmount());
            } else if (bill.getPendingAmount() > 0.009) {
                customerDAO.updatePendingAmount(custId, bill.getPendingAmount());
            } else {
                double cash = 0; try { cash = Double.parseDouble(txtCash.getText().trim()); } catch(Exception ignored) {}
                double gpay = 0; try { gpay = Double.parseDouble(txtGPay.getText().trim()); } catch(Exception ignored) {}
                double received = cash + gpay;
                if (received > currentTotal) {
                    double excess = received - currentTotal;
                    double newPend = Math.max(0.0, currentCustomerPending - excess);
                    customerDAO.setPendingAmount(custId, newPend);
                }
            }
        }
        customerDAO.syncCustomerPendingBalance(custId);
    }
}

