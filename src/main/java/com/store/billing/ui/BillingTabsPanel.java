package com.store.billing.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BillingTabsPanel extends JPanel {
    private JTabbedPane tabbedPane;

    public BillingTabsPanel() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Use FlatLaf trailing component for the "Add Tab" button
        JButton btnAddTab = new JButton("+ New Bill (Ctrl+T)");
        btnAddTab.setFocusPainted(false);
        btnAddTab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddTab.addActionListener((ActionEvent e) -> addNewTab());
        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", btnAddTab);
        
        tabbedPane.putClientProperty("JTabbedPane.tabClosable", true);
        tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback", (java.util.function.IntConsumer) tabIndex -> {
            if (tabbedPane.getTabCount() > 1) {
                BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(tabIndex);
                if (bp.isDirty() && !bp.isEditingExistingBill()) {
                    int confirm = JOptionPane.showConfirmDialog(BillingTabsPanel.this,
                            "This bill has items entered. Are you sure you want to close it?",
                            "Close Tab", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) return;
                }
                tabbedPane.removeTabAt(tabIndex);
                updateTabTitles();
                focusProductSearch();
            } else {
                BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(tabIndex);
                bp.clearCart();
            }
        });

        addNewTab();
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Add keyboard shortcut Ctrl+T to open a new tab
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK), "newTab");
        this.getActionMap().put("newTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewTab();
            }
        });
        
        // Add keyboard shortcut Ctrl+W to close current tab
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK), "closeTab");
        this.getActionMap().put("closeTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = tabbedPane.getSelectedIndex();
                if (selected >= 0) {
                    if (tabbedPane.getTabCount() > 1) {
                        BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(selected);
                        if (bp.isDirty() && !bp.isEditingExistingBill()) {
                            int confirm = JOptionPane.showConfirmDialog(BillingTabsPanel.this,
                                    "This bill has items entered. Are you sure you want to close it?",
                                    "Close Tab", JOptionPane.YES_NO_OPTION);
                            if (confirm != JOptionPane.YES_OPTION) return;
                        }
                        tabbedPane.removeTabAt(selected);
                        updateTabTitles();
                        focusProductSearch();
                    } else {
                        BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(selected);
                        bp.clearCart();
                    }
                }
            }
        });

        // Add keyboard shortcut Alt+Right to go to next tab
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.ALT_DOWN_MASK), "nextTab");
        this.getActionMap().put("nextTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int count = tabbedPane.getTabCount();
                if (count > 1) {
                    int next = (tabbedPane.getSelectedIndex() + 1) % count;
                    tabbedPane.setSelectedIndex(next);
                    focusProductSearch();
                }
            }
        });

        // Add keyboard shortcut Alt+Left to go to previous tab
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.ALT_DOWN_MASK), "prevTab");
        this.getActionMap().put("prevTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int count = tabbedPane.getTabCount();
                if (count > 1) {
                    int prev = (tabbedPane.getSelectedIndex() - 1 + count) % count;
                    tabbedPane.setSelectedIndex(prev);
                    focusProductSearch();
                }
            }
        });
    }

    public void updateTabTitles() {
        int normalBillCount = 1;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(i);
            if (bp.isEditingExistingBill()) {
                int no = bp.getEditingDailyBillNo();
                tabbedPane.setTitleAt(i, "Edit #" + no);
            } else {
                tabbedPane.setTitleAt(i, "Bill " + (normalBillCount++));
            }
        }
    }

    private BillingPanel createBillingPanel() {
        BillingPanel newPanel = new BillingPanel();
        newPanel.setOnBillStateChangeListener(new BillingPanel.OnBillStateChangeListener() {
            @Override
            public void onBillCleared(BillingPanel panel) {
                updateTabTitles();
            }

            @Override
            public void onBillSavedOrPrinted(BillingPanel panel, int billId, boolean wasEditing) {
                updateTabTitles();
                if (wasEditing && tabbedPane.getTabCount() > 1) {
                    int idx = tabbedPane.indexOfComponent(panel);
                    if (idx >= 0) {
                        tabbedPane.removeTabAt(idx);
                        updateTabTitles();
                        focusProductSearch();
                    }
                }
            }

            @Override
            public void onEditCancelled(BillingPanel panel) {
                updateTabTitles();
                if (tabbedPane.getTabCount() > 1) {
                    int idx = tabbedPane.indexOfComponent(panel);
                    if (idx >= 0) {
                        tabbedPane.removeTabAt(idx);
                        updateTabTitles();
                        focusProductSearch();
                    }
                }
            }
        });
        return newPanel;
    }

    private void addNewTab() {
        BillingPanel newPanel = createBillingPanel();
        tabbedPane.addTab("Bill " + (tabbedPane.getTabCount() + 1), newPanel);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        updateTabTitles();
        
        SwingUtilities.invokeLater(() -> {
            newPanel.refreshProducts();
            newPanel.loadUpiSettings();
            newPanel.loadCustomers();
            newPanel.focusProductSearch();
        });
    }

    public BillingPanel getCurrentBillingPanel() {
        if (tabbedPane.getSelectedIndex() >= 0) {
            return (BillingPanel) tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
        }
        return null;
    }

    public void refreshProducts() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            ((BillingPanel) tabbedPane.getComponentAt(i)).refreshProducts();
        }
    }

    public void loadUpiSettings() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            ((BillingPanel) tabbedPane.getComponentAt(i)).loadUpiSettings();
        }
    }

    public void loadCustomers() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            ((BillingPanel) tabbedPane.getComponentAt(i)).loadCustomers();
        }
    }

    public void focusProductSearch() {
        BillingPanel current = getCurrentBillingPanel();
        if (current != null) {
            current.focusProductSearch();
        }
    }

    public void loadBillForEditing(com.store.billing.model.Bill bill) {
        if (bill == null) return;

        // 1. Check if this bill is already open for editing in one of the tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(i);
            if (bp.getEditingBillId() == bill.getId()) {
                tabbedPane.setSelectedIndex(i);
                bp.loadBillForEditing(bill);
                updateTabTitles();
                bp.focusProductSearch();
                return;
            }
        }

        // 2. Check if current tab is completely clean/empty (NOT dirty)
        BillingPanel targetPanel = null;
        BillingPanel current = getCurrentBillingPanel();
        if (current != null && !current.isDirty()) {
            targetPanel = current;
        } else {
            // 3. Current tab has active typing! Look for any other tab that is completely clean
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                BillingPanel bp = (BillingPanel) tabbedPane.getComponentAt(i);
                if (!bp.isDirty()) {
                    targetPanel = bp;
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        }

        // 4. If no clean tab found, create a NEW TAB so we NEVER overwrite active typing!
        if (targetPanel == null) {
            targetPanel = createBillingPanel();
            int displayNo = bill.getDailyBillNo() > 0 ? bill.getDailyBillNo() : bill.getId();
            String title = "Edit #" + displayNo;
            tabbedPane.addTab(title, targetPanel);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        }

        targetPanel.refreshProducts();
        targetPanel.loadUpiSettings();
        targetPanel.loadCustomers();
        targetPanel.loadBillForEditing(bill);
        updateTabTitles();
        targetPanel.focusProductSearch();
    }

    public void printBillDirectly(com.store.billing.model.Bill bill) {
        if (bill == null) return;
        loadBillForEditing(bill);
        SwingUtilities.invokeLater(() -> {
            BillingPanel current = getCurrentBillingPanel();
            if (current != null) {
                current.openPrintPreviewDialog();
            }
        });
    }
}
