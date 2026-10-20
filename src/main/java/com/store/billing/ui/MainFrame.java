package com.store.billing.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;
    
    private BillingTabsPanel billingPanel;
    private InventoryPanel inventoryPanel;
    private HistoryPanel historyPanel;
    private CustomerPanel customerPanel;
    private SettingsPanel settingsPanel;
    private PurchaseAccountsPanel purchaseAccountsPanel;
    private ReportPanel reportPanel;
    private PartyWisePricePanel partyWisePricePanel;

    public MainFrame() {
        setTitle("MUSTAFA STORES Billing System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setMinimumSize(new Dimension(1024, 600));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null); // Center on screen
        setLayout(new BorderLayout());

        // Sidebar Panel
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        // Logo or App Name in Sidebar
        JLabel appLogo = new JLabel("MUSTAFA STORES");
        appLogo.setFont(new Font("SansSerif", Font.BOLD, 24)); // slightly smaller to fit
        appLogo.setForeground(Color.WHITE);
        appLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(appLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 40)));

        // Content Panel (CardLayout to switch screens)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        billingPanel = new BillingTabsPanel();
        inventoryPanel = new InventoryPanel();
        historyPanel = new HistoryPanel();
        customerPanel = new CustomerPanel();
        settingsPanel = new SettingsPanel();
        purchaseAccountsPanel = new PurchaseAccountsPanel();
        reportPanel = new ReportPanel();
        partyWisePricePanel = new PartyWisePricePanel();
        
        contentPanel.add(billingPanel, "Billing");
        contentPanel.add(inventoryPanel, "Inventory");
        contentPanel.add(purchaseAccountsPanel, "Purchases");
        contentPanel.add(customerPanel, "Customers");
        contentPanel.add(partyWisePricePanel, "PartyWisePrice");
        contentPanel.add(historyPanel, "History");
        contentPanel.add(reportPanel, "Reports");
        contentPanel.add(settingsPanel, "Settings");

        historyPanel.setOnEditBillListener(billId -> {
            com.store.billing.dao.BillDAO bDao = new com.store.billing.dao.BillDAO();
            com.store.billing.model.Bill bill = bDao.getBillById(billId);
            if (bill != null) {
                cardLayout.show(contentPanel, "Billing");
                billingPanel.refreshProducts();
                billingPanel.loadUpiSettings();
                billingPanel.loadCustomers();
                billingPanel.loadBillForEditing(bill);
            } else {
                JOptionPane.showMessageDialog(MainFrame.this, 
                        "Could not find Bill #" + billId + " in the database.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        historyPanel.setOnPrintBillListener(billId -> {
            com.store.billing.dao.BillDAO bDao = new com.store.billing.dao.BillDAO();
            com.store.billing.model.Bill bill = bDao.getBillById(billId);
            if (bill != null) {
                cardLayout.show(contentPanel, "Billing");
                billingPanel.refreshProducts();
                billingPanel.loadUpiSettings();
                billingPanel.loadCustomers();
                billingPanel.printBillDirectly(bill);
            } else {
                JOptionPane.showMessageDialog(MainFrame.this, 
                        "Could not find Bill #" + billId + " in the database.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        customerPanel.setOnEditBillListener(billId -> {
            com.store.billing.dao.BillDAO bDao = new com.store.billing.dao.BillDAO();
            com.store.billing.model.Bill bill = bDao.getBillById(billId);
            if (bill != null) {
                cardLayout.show(contentPanel, "Billing");
                billingPanel.refreshProducts();
                billingPanel.loadUpiSettings();
                billingPanel.loadCustomers();
                billingPanel.loadBillForEditing(bill);
            } else {
                JOptionPane.showMessageDialog(MainFrame.this, 
                        "Could not find Bill #" + billId + " in the database.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Sidebar Buttons
        JButton btnBilling = createSidebarButton("Billing & POS");
        JButton btnInventory = createSidebarButton("Inventory / Stock");
        JButton btnPurchases = createSidebarButton("Purchase & Accounts");
        JButton btnCustomers = createSidebarButton("Customers");
        JButton btnPartyWisePrice = createSidebarButton("Party wise Price");
        JButton btnHistory = createSidebarButton("Billing History");
        JButton btnReports = createSidebarButton("Sales & Reports");
        JButton btnSettings = createSidebarButton("Settings");
        
        btnBilling.addActionListener(e -> {
            cardLayout.show(contentPanel, "Billing");
            billingPanel.refreshProducts();
            billingPanel.loadUpiSettings();
            billingPanel.loadCustomers();
            billingPanel.focusProductSearch();
        });
        btnInventory.addActionListener(e -> {
            cardLayout.show(contentPanel, "Inventory");
            inventoryPanel.loadProducts();
        });
        btnPurchases.addActionListener(e -> {
            cardLayout.show(contentPanel, "Purchases");
            purchaseAccountsPanel.loadPurchases();
        });
        btnCustomers.addActionListener(e -> {
            customerPanel.loadCustomers();
            cardLayout.show(contentPanel, "Customers");
        });
        btnPartyWisePrice.addActionListener(e -> {
            partyWisePricePanel.loadData();
            cardLayout.show(contentPanel, "PartyWisePrice");
        });
        btnHistory.addActionListener(e -> {
            historyPanel.loadHistory();
            cardLayout.show(contentPanel, "History");
        });
        btnReports.addActionListener(e -> cardLayout.show(contentPanel, "Reports"));
        btnSettings.addActionListener(e -> cardLayout.show(contentPanel, "Settings"));
        
        sidebar.add(btnBilling);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnInventory);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnPurchases);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnCustomers);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnPartyWisePrice);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnHistory);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnReports);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(btnSettings);

        // Wrapper for content area to include a top bar for sidebar toggle
        JPanel contentWrapper = new JPanel(new BorderLayout());
        
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(new Color(236, 240, 241));
        
        JButton toggleSidebarBtn = new JButton("\u2630"); // Hamburger menu icon
        toggleSidebarBtn.setFont(new Font("SansSerif", Font.BOLD, 22));
        toggleSidebarBtn.setForeground(new Color(44, 62, 80));
        toggleSidebarBtn.setFocusPainted(false);
        toggleSidebarBtn.setBorderPainted(false);
        toggleSidebarBtn.setContentAreaFilled(false);
        toggleSidebarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        toggleSidebarBtn.addActionListener(e -> {
            sidebar.setVisible(!sidebar.isVisible());
        });
        
        topBar.add(toggleSidebarBtn);
        
        contentWrapper.add(topBar, BorderLayout.NORTH);
        contentWrapper.add(contentPanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                billingPanel.focusProductSearch();
            }
        });

        SwingUtilities.invokeLater(() -> billingPanel.focusProductSearch());
    }
    
    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(220, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("SansSerif", Font.PLAIN, 16));
        button.setBackground(new Color(52, 73, 94));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(41, 128, 185));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(52, 73, 94));
            }
        });
        
        return button;
    }
}
