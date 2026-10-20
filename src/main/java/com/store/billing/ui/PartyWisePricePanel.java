package com.store.billing.ui;

import com.store.billing.dao.CustomerDAO;
import com.store.billing.dao.PartyPriceDAO;
import com.store.billing.dao.ProductDAO;
import com.store.billing.model.Customer;
import com.store.billing.model.PartyPrice;
import com.store.billing.model.Product;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

public class PartyWisePricePanel extends JPanel {

    private JComboBox<Object> cmbCustomer;
    private JComboBox<Object> cmbProduct;
    private JTextField txtCustomPrice;
    private JButton btnSave;
    private JTable priceTable;
    private DefaultTableModel tableModel;

    private CustomerDAO customerDAO;
    private ProductDAO productDAO;
    private PartyPriceDAO partyPriceDAO;

    private List<Customer> allCustomers = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private List<PartyPrice> currentCustomerPrices = new ArrayList<>();
    private boolean isUpdatingCustomer = false;
    private boolean isUpdatingProduct = false;

    public PartyWisePricePanel() {
        customerDAO = new CustomerDAO();
        productDAO = new ProductDAO();
        partyPriceDAO = new PartyPriceDAO();

        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 246, 250));

        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 246, 250));
        JLabel lblTitle = new JLabel("Set Party wise Price");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(new Color(44, 62, 80));
        headerPanel.add(lblTitle, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Panel (Form + Table)
        JPanel centerPanel = new JPanel(new BorderLayout(15, 20));
        centerPanel.setBackground(new Color(245, 246, 250));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Font inputFont = new Font("Nirmala UI", Font.PLAIN, 15);


        // Customer Row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        JLabel lblCust = new JLabel("Search Customer:");
        lblCust.setFont(labelFont);
        formPanel.add(lblCust, gbc);
        
        cmbCustomer = new JComboBox<>();
        cmbCustomer.setPreferredSize(new Dimension(300, 35));
        cmbCustomer.setFont(inputFont);
        setupCustomerAutoComplete();
        gbc.gridx = 1; gbc.weightx = 0.9;
        formPanel.add(cmbCustomer, gbc);

        // Product Row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        JLabel lblProd = new JLabel("Search Product:");
        lblProd.setFont(labelFont);
        formPanel.add(lblProd, gbc);
        
        cmbProduct = new JComboBox<>();
        cmbProduct.setPreferredSize(new Dimension(300, 35));
        cmbProduct.setFont(inputFont);
        setupProductAutoComplete();
        gbc.gridx = 1; gbc.weightx = 0.9;
        formPanel.add(cmbProduct, gbc);

        // Custom Price Row
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.1;
        JLabel lblPrice = new JLabel("Custom Price (\u20B9):");
        lblPrice.setFont(labelFont);
        formPanel.add(lblPrice, gbc);
        
        txtCustomPrice = new JTextField();
        txtCustomPrice.setPreferredSize(new Dimension(300, 35));
        txtCustomPrice.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtCustomPrice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1; gbc.weightx = 0.9;
        formPanel.add(txtCustomPrice, gbc);

        // Save Button
        btnSave = new JButton("Save Custom Price");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnSave.setBackground(new Color(41, 128, 185));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setPreferredSize(new Dimension(200, 40));
        
        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(btnSave, gbc);

        centerPanel.add(formPanel, BorderLayout.NORTH);

        // Table Panel
        String[] columns = {"S.No", "Product Name", "Custom Price (\u20B9)", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        priceTable = new JTable(tableModel);
        priceTable.setRowHeight(35);
        priceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        priceTable.getTableHeader().setBackground(new Color(236, 240, 241));
        priceTable.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        priceTable.setShowGrid(true);
        priceTable.setGridColor(new Color(220, 220, 220));
        priceTable.setSelectionBackground(new Color(220, 235, 255));
        priceTable.setSelectionForeground(Color.BLACK);
        
        priceTable.getColumnModel().getColumn(3).setMaxWidth(120);
        priceTable.getColumnModel().getColumn(3).setMinWidth(120);
        priceTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel panel = new JPanel(new GridBagLayout());
                if (isSelected) {
                    panel.setBackground(table.getSelectionBackground());
                } else {
                    panel.setBackground(table.getBackground());
                }
                
                JButton btn = new JButton("Delete");
                btn.setBackground(new Color(231, 76, 60)); // Red color
                btn.setForeground(Color.WHITE);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
                btn.setFocusPainted(false);
                btn.setBorderPainted(false);
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                panel.add(btn);
                return panel;
            }
        });
        
        priceTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = priceTable.rowAtPoint(evt.getPoint());
                int col = priceTable.columnAtPoint(evt.getPoint());
                if (row >= 0 && col == 3) { // Action column
                    if (currentCustomerPrices != null && row < currentCustomerPrices.size()) {
                        int id = currentCustomerPrices.get(row).getId();
                        int confirm = JOptionPane.showConfirmDialog(PartyWisePricePanel.this, 
                                "Are you sure you want to delete this custom price?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            partyPriceDAO.deletePartyPrice(id);
                            loadPricesForSelectedCustomer();
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(priceTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(new Color(245, 246, 250));
        JLabel lblTableTitle = new JLabel("Existing Custom Prices for Selected Customer");
        lblTableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        tableContainer.add(lblTableTitle, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        
        centerPanel.add(tableContainer, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Event Listeners
        cmbCustomer.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !isUpdatingCustomer) {
                loadPricesForSelectedCustomer();
                updatePriceField();
            }
        });

        cmbProduct.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !isUpdatingProduct) {
                updatePriceField();
            }
        });

        btnSave.addActionListener(e -> savePrice());
        
        // Allow pressing Enter in Custom Price box to save directly
        txtCustomPrice.addActionListener(e -> savePrice());

        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { cmbCustomer },
            { cmbProduct },
            { txtCustomPrice },
            { btnSave }
        });

        loadData();
    }

    private void setupCustomerAutoComplete() {
        cmbCustomer.setEditable(true);
        JTextField editor = (JTextField) cmbCustomer.getEditor().getEditorComponent();
        editor.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        
        // Remove Action Listener added previously as we handle it in keyPressed now
        
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    evt.consume(); // Prevent default text commit
                    if (cmbCustomer.getItemCount() > 0) {
                        Object item = cmbCustomer.getSelectedItem();
                        if (item instanceof String || item == null) {
                            item = cmbCustomer.getItemAt(0);
                            isUpdatingCustomer = true;
                            cmbCustomer.setSelectedItem(item);
                            isUpdatingCustomer = false;
                        }
                        if (item instanceof Customer) {
                            isUpdatingCustomer = true;
                            editor.setText(((Customer) item).getName());
                            isUpdatingCustomer = false;
                        }
                    }
                    cmbCustomer.hidePopup();
                    loadPricesForSelectedCustomer();
                    updatePriceField();
                    cmbProduct.requestFocusInWindow();
                }
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (isNavigationKey(evt.getKeyCode())) return;
                
                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    isUpdatingCustomer = true;
                    java.util.Vector<Object> items = new java.util.Vector<>();
                    
                    if (text.isEmpty()) {
                        items.addAll(allCustomers);
                    } else {
                        String lowerText = text.toLowerCase();
                        for (Customer c : allCustomers) {
                            if (c.getName().toLowerCase().contains(lowerText) || 
                                (c.getMobile() != null && c.getMobile().contains(lowerText))) {
                                items.add(c);
                            }
                        }
                    }
                    
                    if (cmbCustomer.getModel().getSize() != items.size() || !items.isEmpty()) {
                        cmbCustomer.setModel(new DefaultComboBoxModel<>(items));
                        editor.setText(text);
                    }
                    
                    if (cmbCustomer.getItemCount() > 0) {
                        cmbCustomer.showPopup();
                    } else {
                        cmbCustomer.hidePopup();
                    }
                    isUpdatingCustomer = false;
                });
            }
        });
        
        cmbCustomer.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer) {
                    setText(((Customer) value).getName());
                }
                return this;
            }
        });
    }

    private void setupProductAutoComplete() {
        cmbProduct.setEditable(true);
        JTextField editor = (JTextField) cmbProduct.getEditor().getEditorComponent();
        editor.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    evt.consume(); // Prevent default text commit
                    if (cmbProduct.getItemCount() > 0) {
                        Object item = cmbProduct.getSelectedItem();
                        if (item instanceof String || item == null) {
                            item = cmbProduct.getItemAt(0);
                            isUpdatingProduct = true;
                            cmbProduct.setSelectedItem(item);
                            isUpdatingProduct = false;
                        }
                        if (item instanceof Product) {
                            isUpdatingProduct = true;
                            editor.setText(((Product) item).getName());
                            isUpdatingProduct = false;
                        }
                    }
                    cmbProduct.hidePopup();
                    updatePriceField();
                    txtCustomPrice.requestFocusInWindow();
                }
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                if (isNavigationKey(evt.getKeyCode())) return;
                
                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    isUpdatingProduct = true;
                    java.util.Vector<Object> items = new java.util.Vector<>();
                    
                    if (text.isEmpty()) {
                        items.addAll(allProducts);
                    } else {
                        java.util.List<Product> searchResults = com.store.billing.util.ProductSearchUtil.searchProducts(allProducts, text, 50);
                        items.addAll(searchResults);
                    }
                    
                    if (cmbProduct.getModel().getSize() != items.size() || !items.isEmpty()) {
                        cmbProduct.setModel(new DefaultComboBoxModel<>(items));
                        editor.setText(text);
                    }
                    
                    if (cmbProduct.getItemCount() > 0) {
                        cmbProduct.showPopup();
                    } else {
                        cmbProduct.hidePopup();
                    }
                    isUpdatingProduct = false;
                });
            }
        });
        
        cmbProduct.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product) {
                    Product p = (Product) value;
                    setText(p.getName() + " - MRP: \u20B9" + p.getRetailPrice());
                }
                return this;
            }
        });
    }
    
    private boolean isNavigationKey(int keyCode) {
        return keyCode == java.awt.event.KeyEvent.VK_ENTER ||
               keyCode == java.awt.event.KeyEvent.VK_UP ||
               keyCode == java.awt.event.KeyEvent.VK_DOWN ||
               keyCode == java.awt.event.KeyEvent.VK_LEFT ||
               keyCode == java.awt.event.KeyEvent.VK_RIGHT ||
               keyCode == java.awt.event.KeyEvent.VK_ESCAPE;
    }

    public void loadData() {
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                allCustomers = customerDAO.getAllCustomers();
                allProducts = productDAO.getAllProducts();
                return null;
            }

            @Override
            protected void done() {
                isUpdatingCustomer = true;
                cmbCustomer.removeAllItems();
                for (Customer c : allCustomers) {
                    cmbCustomer.addItem(c);
                }
                cmbCustomer.setSelectedItem(null);
                isUpdatingCustomer = false;

                isUpdatingProduct = true;
                cmbProduct.removeAllItems();
                for (Product p : allProducts) {
                    cmbProduct.addItem(p);
                }
                cmbProduct.setSelectedItem(null);
                isUpdatingProduct = false;
                
                txtCustomPrice.setText("");
                tableModel.setRowCount(0);
                setCursor(java.awt.Cursor.getDefaultCursor());
            }
        }.execute();
    }

    private void updatePriceField() {
        Object custObj = cmbCustomer.getSelectedItem();
        if (custObj instanceof String) {
            String typed = (String) custObj;
            for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                Object item = cmbCustomer.getItemAt(i);
                if (item instanceof Customer && ((Customer) item).getName().equalsIgnoreCase(typed)) {
                    custObj = item; break;
                }
            }
            if (custObj instanceof String && cmbCustomer.getItemCount() > 0) {
                custObj = cmbCustomer.getItemAt(0);
            }
            isUpdatingCustomer = true;
            cmbCustomer.setSelectedItem(custObj);
            isUpdatingCustomer = false;
        }

        Object prodObj = cmbProduct.getSelectedItem();
        if (prodObj instanceof String) {
            String typed = (String) prodObj;
            for (int i = 0; i < cmbProduct.getItemCount(); i++) {
                Object item = cmbProduct.getItemAt(i);
                if (item instanceof Product && ((Product) item).getName().equalsIgnoreCase(typed)) {
                    prodObj = item; break;
                }
            }
            if (prodObj instanceof String && cmbProduct.getItemCount() > 0) {
                prodObj = cmbProduct.getItemAt(0);
            }
            isUpdatingProduct = true;
            cmbProduct.setSelectedItem(prodObj);
            isUpdatingProduct = false;
        }
        
        if (prodObj instanceof Product) {
            Product p = (Product) prodObj;
            Double customPrice = null;
            
            if (custObj instanceof Customer) {
                Customer c = (Customer) custObj;
                customPrice = partyPriceDAO.getPartyPrice(c.getId(), p.getId());
            }
            
            if (customPrice != null) {
                txtCustomPrice.setText(String.format("%.2f", customPrice));
            } else {
                txtCustomPrice.setText(String.format("%.2f", p.getRetailPrice())); // Fallback to retail price
            }
        } else {
            txtCustomPrice.setText("");
        }
    }

    private void savePrice() {
        Object custObj = cmbCustomer.getSelectedItem();
        if (custObj instanceof String) {
            String typed = (String) custObj;
            for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                Object item = cmbCustomer.getItemAt(i);
                if (item instanceof Customer && ((Customer) item).getName().equalsIgnoreCase(typed)) {
                    custObj = item; break;
                }
            }
            if (custObj instanceof String && cmbCustomer.getItemCount() > 0) {
                custObj = cmbCustomer.getItemAt(0);
            }
            isUpdatingCustomer = true;
            cmbCustomer.setSelectedItem(custObj);
            isUpdatingCustomer = false;
        }

        Object prodObj = cmbProduct.getSelectedItem();
        if (prodObj instanceof String) {
            String typed = (String) prodObj;
            for (int i = 0; i < cmbProduct.getItemCount(); i++) {
                Object item = cmbProduct.getItemAt(i);
                if (item instanceof Product && ((Product) item).getName().equalsIgnoreCase(typed)) {
                    prodObj = item; break;
                }
            }
            if (prodObj instanceof String && cmbProduct.getItemCount() > 0) {
                prodObj = cmbProduct.getItemAt(0);
            }
            isUpdatingProduct = true;
            cmbProduct.setSelectedItem(prodObj);
            isUpdatingProduct = false;
        }

        if (!(custObj instanceof Customer)) {
            JOptionPane.showMessageDialog(this, "Please select a valid Customer from the list.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!(prodObj instanceof Product)) {
            JOptionPane.showMessageDialog(this, "Please select a valid Product from the list.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Customer selectedCustomer = (Customer) custObj;
        Product selectedProduct = (Product) prodObj;
        
        try {
            double price = Double.parseDouble(txtCustomPrice.getText());
            partyPriceDAO.saveOrUpdatePartyPrice(selectedCustomer.getId(), selectedProduct.getId(), price);
            JOptionPane.showMessageDialog(this, "Price saved successfully for " + selectedCustomer.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
            loadPricesForSelectedCustomer();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric price.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPricesForSelectedCustomer() {
        tableModel.setRowCount(0);
        Object custObj = cmbCustomer.getSelectedItem();
        if (custObj instanceof Customer) {
            Customer selected = (Customer) custObj;
            currentCustomerPrices = partyPriceDAO.getPartyPricesByCustomer(selected.getId());
            int sno = 1;
            for (PartyPrice pp : currentCustomerPrices) {
                tableModel.addRow(new Object[]{
                        sno++,
                        pp.getProductName(),
                        String.format("%.2f", pp.getCustomPrice()),
                        "Delete"
                });
            }
        } else {
            currentCustomerPrices = new ArrayList<>();
        }
    }
}
