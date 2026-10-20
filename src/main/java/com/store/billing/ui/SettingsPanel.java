package com.store.billing.ui;

import com.store.billing.dao.SettingsDAO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class SettingsPanel extends JPanel {
    private SettingsDAO settingsDAO;
    
    private JTextField txtShopName, txtShopSince, txtShopAddress, txtShopPhone;
    
    private JCheckBox chkEnableUpi;
    private JTextField txtNewUpi;
    private DefaultListModel<String> upiListModel;
    private JList<String> upiList;
    
    private JComboBox<String> cmbDefaultPrinter;

    public SettingsPanel() {
        settingsDAO = new SettingsDAO();
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 20, 20));

        // 1. Shop Details Panel
        JPanel shopPanel = new JPanel(new GridBagLayout());
        shopPanel.setBorder(BorderFactory.createTitledBorder("Shop Details (For Print Receipt)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        Font fieldFont = new Font("Nirmala UI", Font.PLAIN, 15);
        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        JLabel lblShop = new JLabel("Shop Name:");
        lblShop.setFont(labelFont);
        shopPanel.add(lblShop, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        txtShopName = new JTextField();
        txtShopName.setFont(fieldFont);
        shopPanel.add(txtShopName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        JLabel lblSince = new JLabel("Since:");
        lblSince.setFont(labelFont);
        shopPanel.add(lblSince, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        txtShopSince = new JTextField();
        txtShopSince.setFont(fieldFont);
        shopPanel.add(txtShopSince, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        JLabel lblAddr = new JLabel("Shop Address:");
        lblAddr.setFont(labelFont);
        shopPanel.add(lblAddr, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.8;
        txtShopAddress = new JTextField();
        txtShopAddress.setFont(fieldFont);
        shopPanel.add(txtShopAddress, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.2;
        JLabel lblPhone = new JLabel("Shop Phone:");
        lblPhone.setFont(labelFont);
        shopPanel.add(lblPhone, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.8;
        txtShopPhone = new JTextField();
        txtShopPhone.setFont(fieldFont);
        shopPanel.add(txtShopPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.2;
        JLabel lblPrinter = new JLabel("Default Printer:");
        lblPrinter.setFont(labelFont);
        shopPanel.add(lblPrinter, gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.8;
        cmbDefaultPrinter = new JComboBox<>();
        cmbDefaultPrinter.setFont(fieldFont);
        cmbDefaultPrinter.addItem("None (Show System Dialog)");
        
        // Load printers in background thread so app UI launches instantly without delay
        new Thread(() -> {
            try {
                PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
                SwingUtilities.invokeLater(() -> {
                    String savedPrinter = settingsDAO.getSetting("default_printer", "None (Show System Dialog)");
                    for (PrintService service : printServices) {
                        cmbDefaultPrinter.addItem(service.getName());
                    }
                    cmbDefaultPrinter.setSelectedItem(savedPrinter);
                });
            } catch (Exception ignored) {}
        }).start();
        
        shopPanel.add(cmbDefaultPrinter, gbc);

        mainPanel.add(shopPanel);

        // 2. UPI Settings Panel
        JPanel upiPanel = new JPanel(new BorderLayout(10, 10));
        upiPanel.setBorder(BorderFactory.createTitledBorder("UPI QR Code Settings"));
        
        JPanel upiTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chkEnableUpi = new JCheckBox("Enable UPI QR Code on Printed Bills");
        chkEnableUpi.setFont(labelFont);
        upiTop.add(chkEnableUpi);
        upiPanel.add(upiTop, BorderLayout.NORTH);
        
        JPanel upiCenter = new JPanel(new BorderLayout(5, 5));
        upiListModel = new DefaultListModel<>();
        upiList = new JList<>(upiListModel);
        upiList.setFont(fieldFont);
        upiCenter.add(new JScrollPane(upiList), BorderLayout.CENTER);
        
        JPanel upiInput = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblUpi = new JLabel("New UPI ID (e.g. yourname@upi): ");
        lblUpi.setFont(labelFont);
        upiInput.add(lblUpi);
        txtNewUpi = new JTextField(20);
        txtNewUpi.setFont(fieldFont);
        upiInput.add(txtNewUpi);
        
        JButton btnAddUpi = new JButton("Add UPI");
        btnAddUpi.addActionListener(e -> {
            String newUpi = txtNewUpi.getText().trim();
            if (!newUpi.isEmpty()) {
                if (!upiListModel.contains(newUpi)) {
                    upiListModel.addElement(newUpi);
                    txtNewUpi.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "UPI ID already exists!");
                }
            }
        });
        upiInput.add(btnAddUpi);
        
        JButton btnRemoveUpi = new JButton("Remove Selected");
        btnRemoveUpi.addActionListener(e -> {
            int selectedIndex = upiList.getSelectedIndex();
            if (selectedIndex != -1) {
                upiListModel.remove(selectedIndex);
            }
        });
        upiInput.add(btnRemoveUpi);
        
        upiCenter.add(upiInput, BorderLayout.SOUTH);
        upiPanel.add(upiCenter, BorderLayout.CENTER);

        mainPanel.add(upiPanel);
        add(mainPanel, BorderLayout.CENTER);

        // Save Button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Save All Settings");
        btnSave.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> saveSettings());
        bottomPanel.add(btnSave);
        
        add(bottomPanel, BorderLayout.SOUTH);

        // Enter Key Navigation: Shop Name -> Since -> Shop Address -> Shop Phone -> Default Printer -> Enable UPI -> New UPI -> Save
        txtShopName.addActionListener(e -> txtShopSince.requestFocusInWindow());
        txtShopSince.addActionListener(e -> txtShopAddress.requestFocusInWindow());
        txtShopAddress.addActionListener(e -> txtShopPhone.requestFocusInWindow());
        txtShopPhone.addActionListener(e -> cmbDefaultPrinter.requestFocusInWindow());

        cmbDefaultPrinter.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    chkEnableUpi.requestFocusInWindow();
                }
            }
        });

        chkEnableUpi.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    if (chkEnableUpi.isSelected()) {
                        txtNewUpi.requestFocusInWindow();
                    } else {
                        btnSave.requestFocusInWindow();
                    }
                }
            }
        });

        txtNewUpi.addActionListener(e -> {
            String newUpi = txtNewUpi.getText().trim();
            if (!newUpi.isEmpty()) {
                if (!upiListModel.contains(newUpi)) {
                    upiListModel.addElement(newUpi);
                    txtNewUpi.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "UPI ID already exists!");
                }
            }
            btnSave.requestFocusInWindow();
        });
        
        // Arrow Key 2D Navigation (Left, Right, Up, Down)
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
                { txtShopName },
                { txtShopSince },
                { txtShopAddress },
                { txtShopPhone },
                { cmbDefaultPrinter },
                { chkEnableUpi },
                { txtNewUpi, btnAddUpi, btnRemoveUpi },
                { btnSave }
        });

        loadSettings();
    }

    private void loadSettings() {
        txtShopName.setText(settingsDAO.getSetting("shop_name", "MUSTAFA STORES"));
        txtShopSince.setText(settingsDAO.getSetting("shop_since", ""));
        txtShopAddress.setText(settingsDAO.getSetting("shop_address", "123 Main Street, City"));
        txtShopPhone.setText(settingsDAO.getSetting("shop_phone", "+91 9876543210"));
        
        String savedPrinter = settingsDAO.getSetting("default_printer", "None (Show System Dialog)");
        cmbDefaultPrinter.setSelectedItem(savedPrinter);
        
        chkEnableUpi.setSelected(Boolean.parseBoolean(settingsDAO.getSetting("upi_enabled", "false")));
        
        String upiString = settingsDAO.getSetting("upi_ids", "");
        upiListModel.clear();
        if (!upiString.isEmpty()) {
            String[] ids = upiString.split(",");
            for (String id : ids) {
                if (!id.trim().isEmpty()) {
                    upiListModel.addElement(id.trim());
                }
            }
        }
    }

    private void saveSettings() {
        settingsDAO.updateSetting("shop_name", txtShopName.getText().trim());
        settingsDAO.updateSetting("shop_since", txtShopSince.getText().trim());
        settingsDAO.updateSetting("shop_address", txtShopAddress.getText().trim());
        settingsDAO.updateSetting("shop_phone", txtShopPhone.getText().trim());
        
        settingsDAO.updateSetting("default_printer", cmbDefaultPrinter.getSelectedItem().toString());
        
        settingsDAO.updateSetting("upi_enabled", String.valueOf(chkEnableUpi.isSelected()));
        
        StringBuilder upiBuilder = new StringBuilder();
        for (int i = 0; i < upiListModel.getSize(); i++) {
            upiBuilder.append(upiListModel.getElementAt(i));
            if (i < upiListModel.getSize() - 1) {
                upiBuilder.append(",");
            }
        }
        settingsDAO.updateSetting("upi_ids", upiBuilder.toString());
        
        JOptionPane.showMessageDialog(this, "Settings Saved Successfully!");
    }
}
