package com.store.billing.ui;

import com.store.billing.dao.ReportDAO;
import com.store.billing.model.ReportItem;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReportPanel extends JPanel {
    private ReportDAO reportDAO;
    private JTable reportTable;
    private DefaultTableModel tableModel;
    
    private JSpinner fromDateSpinner;
    private JSpinner toDateSpinner;
    
    private JComboBox<String> cmbCategory;
    private TableRowSorter<DefaultTableModel> rowSorter;
    
    private JLabel lblTotalQty;
    private JLabel lblTotalSales;
    private JLabel lblTotalProfit;

    public ReportPanel() {
        this.reportDAO = new ReportDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Top Filter Panel
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Report Filters"));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        JButton btnToday = new JButton("Today");
        JButton btnThisWeek = new JButton("This Week");
        JButton btnThisMonth = new JButton("This Month");
        
        topRow.add(btnToday);
        topRow.add(btnThisWeek);
        topRow.add(btnThisMonth);

        topRow.add(new JLabel(" |  From Date:"));
        fromDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor fromEditor = new JSpinner.DateEditor(fromDateSpinner, "dd-MM-yyyy");
        fromDateSpinner.setEditor(fromEditor);
        topRow.add(fromDateSpinner);

        topRow.add(new JLabel(" To Date:"));
        toDateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor toEditor = new JSpinner.DateEditor(toDateSpinner, "dd-MM-yyyy");
        toDateSpinner.setEditor(toEditor);
        topRow.add(toDateSpinner);

        JButton btnGenerate = new JButton("Generate Report");
        btnGenerate.setBackground(new Color(0, 123, 255));
        btnGenerate.setForeground(Color.WHITE);
        btnGenerate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topRow.add(btnGenerate);
        
        JButton btnDelete = new JButton("Delete Sales");
        btnDelete.setBackground(new Color(220, 53, 69)); // Red color
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topRow.add(btnDelete);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        bottomRow.add(new JLabel("Category Filter:"));
        cmbCategory = new JComboBox<>(new String[]{"All Categories"});
        bottomRow.add(cmbCategory);

        filterPanel.add(topRow);
        filterPanel.add(bottomRow);

        add(filterPanel, BorderLayout.NORTH);

        // 2. Table Panel
        String[] columns = {"Category", "Product Name", "Qty Sold", "Total Sales (₹)", "Est. Profit (₹)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(32);
        reportTable.setFont(new Font("Nirmala UI", Font.PLAIN, 15));
        reportTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        reportTable.getTableHeader().setBackground(new Color(235, 240, 245));
        
        rowSorter = new TableRowSorter<>(tableModel);
        reportTable.setRowSorter(rowSorter);

        cmbCategory.addActionListener(e -> {
            if (cmbCategory.getSelectedItem() == null) return;
            String text = cmbCategory.getSelectedItem().toString();
            if ("All Categories".equals(text)) {
                rowSorter.setRowFilter(null);
            } else {
                rowSorter.setRowFilter(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(text) + "$", 0));
            }
            updateGrandTotalsFromTable();
        });
        
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setForeground(Color.WHITE);
                } else {
                    c.setForeground(new Color(33, 37, 41));
                }
                return c;
            }
        };
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        reportTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        reportTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        reportTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        
        // Category styling (bold for category column)
        reportTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(new Font("Segoe UI", Font.BOLD, 15));
                if (isSelected) {
                    c.setForeground(Color.WHITE);
                } else {
                    c.setForeground(new Color(33, 37, 41));
                }
                
                if (row > 0) {
                    Object prevValue = table.getValueAt(row - 1, column);
                    if (value != null && value.equals(prevValue)) {
                        setText("");
                    }
                }
                return c;
            }
        });

        // Alternating row colors
        reportTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                    c.setForeground(new Color(33, 37, 41));
                } else {
                    c.setForeground(Color.WHITE);
                }
                
                // Align numeric columns to right
                if (column >= 2) {
                    ((JLabel)c).setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    ((JLabel)c).setHorizontalAlignment(JLabel.LEFT);
                }
                
                return c;
            }
        });

        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // 3. Bottom Summary Panel
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Grand Totals"));
        
        lblTotalQty = createSummaryLabel("Total Items Sold: 0");
        lblTotalSales = createSummaryLabel("Total Sales: ₹0.00");
        lblTotalProfit = createSummaryLabel("Total Profit: ₹0.00");
        lblTotalProfit.setForeground(new Color(40, 167, 69)); // Green for profit
        
        summaryPanel.add(lblTotalQty);
        summaryPanel.add(lblTotalSales);
        summaryPanel.add(lblTotalProfit);
        
        add(summaryPanel, BorderLayout.SOUTH);

        // 4. Listeners
        btnToday.addActionListener(e -> setDateRangeAndGenerate(0));
        btnThisWeek.addActionListener(e -> setDateRangeAndGenerate(-7));
        btnThisMonth.addActionListener(e -> setDateRangeAndGenerate(-30));
        btnGenerate.addActionListener(e -> generateReport());
        
        btnDelete.addActionListener(e -> {
            java.util.Date fromDate = (java.util.Date) fromDateSpinner.getValue();
            java.util.Date toDate = (java.util.Date) toDateSpinner.getValue();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to permanently DELETE ALL BILLS from " + sdf.format(fromDate) + " to " + sdf.format(toDate) + "?\nThis action cannot be undone!",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                com.store.billing.dao.BillDAO billDAO = new com.store.billing.dao.BillDAO();
                if (billDAO.deleteBillsByDateRange(fromDate, toDate)) {
                    JOptionPane.showMessageDialog(this, "Sales data deleted successfully.");
                    generateReport();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete sales data.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Default to Today
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { btnToday, btnThisWeek, btnThisMonth, ((JSpinner.DefaultEditor)fromDateSpinner.getEditor()).getTextField(), ((JSpinner.DefaultEditor)toDateSpinner.getEditor()).getTextField(), btnGenerate, btnDelete },
            { cmbCategory, cmbCategory, cmbCategory, cmbCategory, cmbCategory, cmbCategory, cmbCategory }
        });
        setDateRangeAndGenerate(0);
    }
    
    private JLabel createSummaryLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void setDateRangeAndGenerate(int daysOffset) {
        Calendar cal = Calendar.getInstance();
        toDateSpinner.setValue(cal.getTime());
        
        if (daysOffset == -7) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        } else if (daysOffset == -30) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            cal.add(Calendar.DAY_OF_YEAR, daysOffset); 
        }
        
        fromDateSpinner.setValue(cal.getTime());
        generateReport();
    }

    private void generateReport() {
        Date fromDate = (Date) fromDateSpinner.getValue();
        Date toDate = (Date) toDateSpinner.getValue();

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        fromDate = cal.getTime();

        List<ReportItem> data = reportDAO.getSalesByCategory(fromDate, toDate);
        
        tableModel.setRowCount(0);
        
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        categories.add("All Categories");
        
        for (ReportItem item : data) {
            categories.add(item.getCategory());
            tableModel.addRow(new Object[]{
                item.getCategory(),
                item.getProductName(),
                item.getQuantitySold(),
                String.format("%.2f", item.getTotalSales()),
                String.format("%.2f", item.getTotalProfit())
            });
        }
        
        // Temporarily remove listener to avoid triggering it while updating items
        java.awt.event.ActionListener[] listeners = cmbCategory.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) {
            cmbCategory.removeActionListener(l);
        }
        
        String currentSel = (String) cmbCategory.getSelectedItem();
        cmbCategory.removeAllItems();
        for(String cat : categories) {
             cmbCategory.addItem(cat);
        }
        if (currentSel != null && categories.contains(currentSel)) {
             cmbCategory.setSelectedItem(currentSel);
        } else {
             cmbCategory.setSelectedIndex(0);
        }
        
        for (java.awt.event.ActionListener l : listeners) {
            cmbCategory.addActionListener(l);
        }
        
        // Ensure filter matches new selection
        if (cmbCategory.getSelectedIndex() == 0) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote((String)cmbCategory.getSelectedItem()) + "$", 0));
        }
        
        updateGrandTotalsFromTable();
    }
    
    private void updateGrandTotalsFromTable() {
        int totalQty = 0;
        double totalSales = 0;
        double totalProfit = 0;
        for (int i = 0; i < reportTable.getRowCount(); i++) {
            totalQty += Integer.parseInt(reportTable.getValueAt(i, 2).toString());
            totalSales += Double.parseDouble(reportTable.getValueAt(i, 3).toString().replace(",", ""));
            totalProfit += Double.parseDouble(reportTable.getValueAt(i, 4).toString().replace(",", ""));
        }
        lblTotalQty.setText("Total Items Sold: " + totalQty);
        lblTotalSales.setText(String.format("Total Sales: ₹%.2f", totalSales));
        lblTotalProfit.setText(String.format("Total Profit: ₹%.2f", totalProfit));
    }
}
