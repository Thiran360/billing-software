package com.store.billing.ui;

import com.store.billing.dao.ProductDAO;
import com.store.billing.model.Product;
import com.store.billing.util.ExcelParser;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryPanel extends JPanel {
    private final ProductDAO productDAO;
    private List<Product> allProductsList = new ArrayList<>();
    private List<String> allCategoriesList = new ArrayList<>();
    private final com.store.billing.dao.UnitDAO unitDAO = new com.store.billing.dao.UnitDAO();
    private List<com.store.billing.model.Unit> allUnitsList = new ArrayList<>();

    // Top Filter Bar Components
    private JComboBox<String> cmbFilterCategory;
    private JTextField txtSearch;
    private JLabel lblProductStats;
    private JButton btnResetFilter, btnSyncExcel;

    // Form Components
    private JTextField txtCode, txtName, txtDisplayName, txtTamilName, txtEnglishName;
    private JComboBox<String> cmbCategory, cmbUnitType;
    private JTextField txtPurchaseRate, txtLineRate, txtStock;
    private JTextField txtWholePrice, txtWholeDis, txtWholeQtyThreshold;
    private JTextField txtRetailPrice, txtRetailDis, txtRetailQtyThreshold;
    private JLabel lblFormStatus;

    private JButton btnAdd, btnUpdate, btnDelete, btnClear;

    // Center Table Components
    private JTable productTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private int selectedProductId = -1;

    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font TAMIL_FONT = new Font("Nirmala UI", Font.PLAIN, 13);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 13);

    public InventoryPanel() {
        productDAO = new ProductDAO();
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Top Filter Bar: Category & Search
        JPanel topFilterPanel = createTopFilterPanel();

        // 2. Data Entry Panel (Form for Add/Update/Delete)
        JPanel formPanel = createProductFormPanel();

        // Combine Top Filter & Form into North Container
        JPanel northContainer = new JPanel(new BorderLayout(0, 6));
        northContainer.add(topFilterPanel, BorderLayout.NORTH);
        northContainer.add(formPanel, BorderLayout.CENTER);
        add(northContainer, BorderLayout.NORTH);

        // 3. Center Table: ID-wise Aligned Product List
        JPanel tableContainer = createTablePanel();
        add(tableContainer, BorderLayout.CENTER);

        // Initialize Data
        loadCategories();
        loadUnits();
        loadProducts();

        // Auto-focus code field when panel is shown
        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                focusCodeField();
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        // Add F2 and F3 keyboard shortcuts
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), "focusEdit");
        this.getActionMap().put("focusEdit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusCodeField();
            }
        });

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), "focusSearch");
        this.getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusSearchField();
            }
        });

        // Add F4 keyboard shortcut to focus the table
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0), "focusTable");
        this.getActionMap().put("focusTable", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (productTable != null && productTable.getRowCount() > 0) {
                    productTable.requestFocusInWindow();
                    if (productTable.getSelectedRow() == -1) {
                        productTable.setRowSelectionInterval(0, 0);
                    }
                }
            }
        });

        focusCodeField();
    }

    public void focusSearchField() {
        Runnable doFocus = () -> {
            if (txtSearch != null) {
                txtSearch.requestFocusInWindow();
                txtSearch.selectAll();
            }
        };
        SwingUtilities.invokeLater(doFocus);
    }

    public void focusCodeField() {
        Runnable doFocus = () -> {
            if (txtCode != null) {
                txtCode.requestFocusInWindow();
                txtCode.selectAll();
            }
        };
        SwingUtilities.invokeLater(doFocus);
        javax.swing.Timer timer = new javax.swing.Timer(150, e -> doFocus.run());
        timer.setRepeats(false);
        timer.start();
    }

    private JPanel createTopFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 230), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        panel.setBackground(new Color(246, 249, 252));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 6, 2, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        // 1. Category Filter Label & Combo
        gbc.gridx = 0; gbc.weightx = 0.0;
        JLabel lblCategory = new JLabel("Category:");
        lblCategory.setFont(LABEL_FONT);
        lblCategory.setForeground(new Color(33, 47, 61));
        panel.add(lblCategory, gbc);

        gbc.gridx = 1; gbc.weightx = 0.25;
        cmbFilterCategory = new JComboBox<>();
        cmbFilterCategory.setFont(FIELD_FONT);
        cmbFilterCategory.setPreferredSize(new Dimension(190, 30));
        cmbFilterCategory.addActionListener(e -> applyCategoryAndSearchFilter());
        panel.add(cmbFilterCategory, gbc);

        // 2. Search Label & Text Field
        gbc.gridx = 2; gbc.weightx = 0.0;
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(LABEL_FONT);
        lblSearch.setForeground(new Color(33, 47, 61));
        panel.add(lblSearch, gbc);

        gbc.gridx = 3; gbc.weightx = 0.35;
        txtSearch = new JTextField();
        txtSearch.setFont(FIELD_FONT);
        txtSearch.setPreferredSize(new Dimension(200, 30));
        txtSearch.putClientProperty("JTextField.placeholderText", "Name, Code, Tamil, Category...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyCategoryAndSearchFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyCategoryAndSearchFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyCategoryAndSearchFilter(); }
        });
        panel.add(txtSearch, gbc);

        // 3. Reset Button
        gbc.gridx = 4; gbc.weightx = 0.0;
        btnResetFilter = new JButton("Reset");
        btnResetFilter.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnResetFilter.setPreferredSize(new Dimension(80, 30));
        btnResetFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnResetFilter.addActionListener(e -> {
            txtSearch.setText("");
            if (cmbFilterCategory.getItemCount() > 0) {
                cmbFilterCategory.setSelectedIndex(0);
            }
        });
        panel.add(btnResetFilter, gbc);

        // 4. Product Stats Label
        gbc.gridx = 5; gbc.weightx = 0.25;
        lblProductStats = new JLabel("Total Products: 0", SwingConstants.CENTER);
        lblProductStats.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblProductStats.setForeground(new Color(41, 128, 185));
        panel.add(lblProductStats, gbc);

        // 5. Sync Excel Button
        gbc.gridx = 6; gbc.weightx = 0.0;
        btnSyncExcel = new JButton("Sync Excel");
        btnSyncExcel.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnSyncExcel.setBackground(new Color(235, 243, 250));
        btnSyncExcel.setPreferredSize(new Dimension(115, 30));
        btnSyncExcel.setToolTipText("Sync / Reload products from Excel sheet");
        btnSyncExcel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSyncExcel.addActionListener(e -> handleExcelSync());
        panel.add(btnSyncExcel, gbc);

        return panel;
    }

    private JPanel createProductFormPanel() {
        JPanel mainFormPanel = new JPanel(new BorderLayout(4, 4));
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(190, 205, 220), 1),
                "Product Details (Add / Update / Delete)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                HEADER_FONT,
                new Color(33, 47, 61)
        );
        mainFormPanel.setBorder(border);

        // Top Status indicator for form mode
        lblFormStatus = new JLabel("");
        lblFormStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblFormStatus.setForeground(new Color(46, 125, 50));
        lblFormStatus.setBorder(BorderFactory.createEmptyBorder(1, 8, 3, 8));
        mainFormPanel.add(lblFormStatus, BorderLayout.NORTH);

        // Form Fields Grid - Clean 8-column layout (4 pairs of label+field)
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Dimension numDim = new Dimension(80, 28);
        Dimension fieldDim = new Dimension(140, 28);

        // Initialize Fields
        txtCode = new JTextField(); txtCode.setFont(FIELD_FONT); txtCode.setPreferredSize(numDim);
        cmbCategory = new JComboBox<>(); cmbCategory.setEditable(true); cmbCategory.setFont(FIELD_FONT); cmbCategory.setPreferredSize(fieldDim);
        setupCategoryAutoComplete();
        txtName = new JTextField(); txtName.setFont(FIELD_FONT); txtName.setPreferredSize(fieldDim);
        cmbUnitType = new JComboBox<>();
        cmbUnitType.setEditable(true);
        cmbUnitType.setFont(FIELD_FONT);
        cmbUnitType.setPreferredSize(numDim);
        cmbUnitType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(TAMIL_FONT);
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
        setupUnitTypeAutoComplete();
        txtDisplayName = new JTextField(); txtDisplayName.setFont(TAMIL_FONT); txtDisplayName.setPreferredSize(fieldDim);
        txtTamilName = new JTextField(); txtTamilName.setFont(TAMIL_FONT); txtTamilName.setPreferredSize(fieldDim);
        txtEnglishName = new JTextField(); txtEnglishName.setFont(FIELD_FONT); txtEnglishName.setPreferredSize(fieldDim);
        txtStock = new JTextField("100"); txtStock.setFont(FIELD_FONT); txtStock.setPreferredSize(numDim);
        
        txtPurchaseRate = new JTextField(); txtPurchaseRate.setFont(FIELD_FONT); txtPurchaseRate.setPreferredSize(numDim);
        txtLineRate = new JTextField(); txtLineRate.setFont(FIELD_FONT); txtLineRate.setPreferredSize(numDim);
        txtWholePrice = new JTextField(); txtWholePrice.setFont(FIELD_FONT); txtWholePrice.setPreferredSize(numDim);
        txtWholeDis = new JTextField(); txtWholeDis.setFont(FIELD_FONT); txtWholeDis.setPreferredSize(numDim);
        
        txtWholeQtyThreshold = new JTextField(); txtWholeQtyThreshold.setFont(FIELD_FONT); txtWholeQtyThreshold.setPreferredSize(numDim);
        txtRetailPrice = new JTextField(); txtRetailPrice.setFont(FIELD_FONT); txtRetailPrice.setPreferredSize(numDim);
        txtRetailDis = new JTextField(); txtRetailDis.setFont(FIELD_FONT); txtRetailDis.setPreferredSize(numDim);
        txtRetailQtyThreshold = new JTextField(); txtRetailQtyThreshold.setFont(FIELD_FONT); txtRetailQtyThreshold.setPreferredSize(numDim);

        double lw = 0.0; // label weight
        double fw = 0.25; // field weight (4 fields = 1.0 total weight)

        // ROW 0: Code, Category, Saving Name, Unit
        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = lw; fieldsPanel.add(new JLabel("Code:"), gbc);
        gbc.gridx = 1; gbc.weightx = fw; fieldsPanel.add(txtCode, gbc);
        gbc.gridx = 2; gbc.weightx = lw; fieldsPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 3; gbc.weightx = fw; fieldsPanel.add(cmbCategory, gbc);
        gbc.gridx = 4; gbc.weightx = lw; fieldsPanel.add(new JLabel("Saving Name *:"), gbc);
        gbc.gridx = 5; gbc.weightx = fw; fieldsPanel.add(txtName, gbc);
        gbc.gridx = 6; gbc.weightx = lw; fieldsPanel.add(new JLabel("Unit:"), gbc);
        JPanel unitBoxPanel = new JPanel(new BorderLayout(3, 0));
        unitBoxPanel.add(cmbUnitType, BorderLayout.CENTER);
        JButton btnQuickUnits = new JButton("+");
        btnQuickUnits.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnQuickUnits.setToolTipText("Manage Units / அலகுகள் மேலாண்மை");
        btnQuickUnits.setPreferredSize(new Dimension(30, 28));
        btnQuickUnits.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuickUnits.addActionListener(e -> openUnitManagementDialog());
        unitBoxPanel.add(btnQuickUnits, BorderLayout.EAST);
        gbc.gridx = 7; gbc.weightx = fw; fieldsPanel.add(unitBoxPanel, gbc);

        // ROW 1: Display Name, Tamil Name, English Name, Stock Qty
        gbc.gridy = 1;
        gbc.gridx = 0; gbc.weightx = lw; fieldsPanel.add(new JLabel("Display Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = fw; fieldsPanel.add(txtDisplayName, gbc);
        gbc.gridx = 2; gbc.weightx = lw; fieldsPanel.add(new JLabel("Tamil Name:"), gbc);
        gbc.gridx = 3; gbc.weightx = fw; fieldsPanel.add(txtTamilName, gbc);
        gbc.gridx = 4; gbc.weightx = lw; fieldsPanel.add(new JLabel("English Name:"), gbc);
        gbc.gridx = 5; gbc.weightx = fw; fieldsPanel.add(txtEnglishName, gbc);
        gbc.gridx = 6; gbc.weightx = lw; fieldsPanel.add(new JLabel("Stock Quantity:"), gbc);
        gbc.gridx = 7; gbc.weightx = fw; fieldsPanel.add(txtStock, gbc);

        // ROW 2: Purchase Rate, Line Rate, Whole Price, WHOLE(Dis)
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.weightx = lw; fieldsPanel.add(new JLabel("Purchase Rate:"), gbc);
        gbc.gridx = 1; gbc.weightx = fw; fieldsPanel.add(txtPurchaseRate, gbc);
        gbc.gridx = 2; gbc.weightx = lw; fieldsPanel.add(new JLabel("Line Rate:"), gbc);
        gbc.gridx = 3; gbc.weightx = fw; fieldsPanel.add(txtLineRate, gbc);
        gbc.gridx = 4; gbc.weightx = lw; fieldsPanel.add(new JLabel("Whole Price:"), gbc);
        gbc.gridx = 5; gbc.weightx = fw; fieldsPanel.add(txtWholePrice, gbc);
        gbc.gridx = 6; gbc.weightx = lw; fieldsPanel.add(new JLabel("WHOLE(Dis):"), gbc);
        gbc.gridx = 7; gbc.weightx = fw; fieldsPanel.add(txtWholeDis, gbc);

        // ROW 3: Whole Qty, Retail Price, Retail(Dis), Retail Qty
        gbc.gridy = 3;
        gbc.gridx = 0; gbc.weightx = lw; fieldsPanel.add(new JLabel("Whole Qty:"), gbc);
        gbc.gridx = 1; gbc.weightx = fw; fieldsPanel.add(txtWholeQtyThreshold, gbc);
        gbc.gridx = 2; gbc.weightx = lw; fieldsPanel.add(new JLabel("Retail Price:"), gbc);
        gbc.gridx = 3; gbc.weightx = fw; fieldsPanel.add(txtRetailPrice, gbc);
        gbc.gridx = 4; gbc.weightx = lw; fieldsPanel.add(new JLabel("Retail(Dis):"), gbc);
        gbc.gridx = 5; gbc.weightx = fw; fieldsPanel.add(txtRetailDis, gbc);
        gbc.gridx = 6; gbc.weightx = lw; fieldsPanel.add(new JLabel("Retail Qty:"), gbc);
        gbc.gridx = 7; gbc.weightx = fw; fieldsPanel.add(txtRetailQtyThreshold, gbc);

        // Action Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnClear = new JButton("Clear Form");
        btnClear.setFont(LABEL_FONT);
        btnClear.setPreferredSize(new Dimension(115, 32));
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> clearFields());
        buttonPanel.add(btnClear);

        btnAdd = new JButton("Add Product");
        btnAdd.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAdd.setBackground(new Color(39, 174, 96));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setPreferredSize(new Dimension(135, 32));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> addProduct());
        buttonPanel.add(btnAdd);

        btnUpdate = new JButton("Update Product");
        btnUpdate.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnUpdate.setBackground(new Color(41, 128, 185));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setPreferredSize(new Dimension(145, 32));
        btnUpdate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUpdate.addActionListener(e -> updateProduct());
        buttonPanel.add(btnUpdate);

        btnDelete = new JButton("Delete Selected");
        btnDelete.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnDelete.setBackground(new Color(192, 57, 43));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setPreferredSize(new Dimension(145, 32));
        btnDelete.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDelete.addActionListener(e -> deleteProduct());
        buttonPanel.add(btnDelete);

        JButton btnManageUnits = new JButton("Manage Units (அலகுகள்)");
        btnManageUnits.setFont(new Font("Nirmala UI", Font.BOLD, 13));
        btnManageUnits.setBackground(new Color(108, 117, 125));
        btnManageUnits.setForeground(Color.WHITE);
        btnManageUnits.setPreferredSize(new Dimension(190, 32));
        btnManageUnits.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnManageUnits.addActionListener(e -> openUnitManagementDialog());
        buttonPanel.add(btnManageUnits);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 8;
        gbc.anchor = GridBagConstraints.EAST;
        fieldsPanel.add(buttonPanel, gbc);

        mainFormPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Enter key listeners
        setupEnterKeyNavigation();

        return mainFormPanel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(190, 205, 220), 1),
                "Product Stock List (Aligned by ID)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                HEADER_FONT,
                new Color(33, 47, 61)
        ));

        // 17 Columns
        String[] columns = {
                "ID", "Code", "Category", "Saving Name", "Display Name", 
                "Print Name (Tamil)", "Print Name (English)", "Unit", 
                "Purchase Rate", "Line Rate", "Whole", "WHOLE(Dis)", "Whole Qty", 
                "Retail", "Retail(Dis)", "Retail Qty", "Stock Qty"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table is view-only, edits happen via form
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 12 || columnIndex == 15 || columnIndex == 16) {
                    return Integer.class;
                } else if (columnIndex == 8 || columnIndex == 9 || columnIndex == 10 || columnIndex == 11 || columnIndex == 13 || columnIndex == 14) {
                    return Double.class;
                }
                return String.class;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setRowHeight(25);
        productTable.setFont(TAMIL_FONT);
        productTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        productTable.getTableHeader().setBackground(new Color(236, 242, 248));
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Sorter - strict ID ascending default
        rowSorter = new TableRowSorter<>(tableModel);
        rowSorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        productTable.setRowSorter(rowSorter);

        // Column Renderers and Widths
        setupTableColumns();

        // Selection Listener: Load selected product into form
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && productTable.getSelectedRow() != -1) {
                int viewRow = productTable.getSelectedRow();
                int modelRow = productTable.convertRowIndexToModel(viewRow);
                populateFormFromModelRow(modelRow);
            }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupTableColumns() {
        int[] widths = {
                55,   // ID
                75,   // Code
                120,  // Category
                185,  // Saving Name
                185,  // Display Name
                160,  // Print Name (Tamil)
                160,  // Print Name (English)
                75,   // Unit
                95,   // Purchase Rate
                85,   // Line Rate
                85,   // Whole
                90,   // WHOLE(Dis)
                75,   // Whole Qty
                85,   // Retail
                90,   // Retail(Dis)
                75,   // Retail Qty
                80    // Stock Qty
        };

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        for (int i = 0; i < widths.length && i < productTable.getColumnCount(); i++) {
            productTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            
            // Apply alignment
            if (i == 0 || i == 7 || i == 12 || i == 15 || i == 16) {
                productTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            } else if (i >= 8 && i <= 14 && i != 12) {
                productTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else {
                productTable.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
            }
        }
    }

    private void setupCategoryAutoComplete() {
        JTextField editor = (JTextField) cmbCategory.getEditor().getEditorComponent();
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    evt.consume();
                    if (cmbCategory.getItemCount() > 0) {
                        String text = editor.getText().trim();
                        boolean exactMatch = false;
                        for(int i=0; i<cmbCategory.getItemCount(); i++) {
                            Object item = cmbCategory.getItemAt(i);
                            if(item != null && item.toString().equalsIgnoreCase(text)) {
                                exactMatch = true;
                                break;
                            }
                        }
                        if (!exactMatch) {
                            Object firstItem = cmbCategory.getItemAt(0);
                            cmbCategory.setSelectedItem(firstItem);
                            editor.setText(firstItem.toString());
                        }
                    }
                    cmbCategory.hidePopup();
                    txtName.requestFocusInWindow();
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                int code = evt.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_ENTER ||
                    code == java.awt.event.KeyEvent.VK_UP ||
                    code == java.awt.event.KeyEvent.VK_DOWN ||
                    code == java.awt.event.KeyEvent.VK_LEFT ||
                    code == java.awt.event.KeyEvent.VK_RIGHT ||
                    code == java.awt.event.KeyEvent.VK_ESCAPE) {
                    return;
                }
                
                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    java.util.Vector<String> items = new java.util.Vector<>();
                    boolean found = false;
                    
                    if (text.isEmpty()) {
                        items.addAll(allCategoriesList);
                    } else {
                        String lowerText = text.toLowerCase();
                        for (String c : allCategoriesList) {
                            if (c.toLowerCase().contains(lowerText)) {
                                items.add(c);
                                found = true;
                            }
                        }
                    }
                    
                    if (cmbCategory.getModel().getSize() != items.size() || !items.isEmpty()) {
                        cmbCategory.setModel(new javax.swing.DefaultComboBoxModel<>(items));
                        cmbCategory.getEditor().setItem(text);
                    }
                    
                    if (!text.isEmpty() && found && editor.hasFocus() && cmbCategory.isShowing()) {
                        if (!cmbCategory.isPopupVisible()) {
                            try {
                                cmbCategory.showPopup();
                            } catch (java.awt.IllegalComponentStateException ignored) {}
                        }
                    } else {
                        cmbCategory.hidePopup();
                    }
                });
            }
        });
    }

    private void setupUnitTypeAutoComplete() {
        JTextField editor = (JTextField) cmbUnitType.getEditor().getEditorComponent();
        editor.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!cmbUnitType.isPopupVisible() && cmbUnitType.getItemCount() > 0) {
                    try {
                        cmbUnitType.showPopup();
                    } catch (Exception ignored) {}
                }
            }
        });
        editor.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    evt.consume();
                    if (cmbUnitType.getItemCount() > 0) {
                        String text = editor.getText().trim();
                        boolean exactMatch = false;
                        for(int i=0; i<cmbUnitType.getItemCount(); i++) {
                            Object item = cmbUnitType.getItemAt(i);
                            if(item != null && item.toString().equalsIgnoreCase(text)) {
                                exactMatch = true;
                                break;
                            }
                        }
                        if (!exactMatch) {
                            Object firstItem = cmbUnitType.getItemAt(0);
                            cmbUnitType.setSelectedItem(firstItem);
                            editor.setText(firstItem.toString());
                        }
                    }
                    cmbUnitType.hidePopup();
                    txtStock.requestFocusInWindow();
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                int code = evt.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_ENTER ||
                    code == java.awt.event.KeyEvent.VK_UP ||
                    code == java.awt.event.KeyEvent.VK_DOWN ||
                    code == java.awt.event.KeyEvent.VK_LEFT ||
                    code == java.awt.event.KeyEvent.VK_RIGHT ||
                    code == java.awt.event.KeyEvent.VK_ESCAPE) {
                    return;
                }
                
                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    java.util.Vector<String> items = new java.util.Vector<>();
                    boolean found = false;
                    
                    if (text == null || text.trim().isEmpty()) {
                        for (com.store.billing.model.Unit u : allUnitsList) items.add(u.getNameEn());
                    } else {
                        String lowerText = text.trim().toLowerCase();
                        for (com.store.billing.model.Unit u : allUnitsList) {
                            if (u.getNameEn().toLowerCase().contains(lowerText) || 
                                (u.getNameTa() != null && u.getNameTa().toLowerCase().contains(lowerText))) {
                                items.add(u.getNameEn());
                                found = true;
                            }
                        }
                    }
                    
                    if (cmbUnitType.getModel().getSize() != items.size() || !items.isEmpty()) {
                        cmbUnitType.setModel(new javax.swing.DefaultComboBoxModel<>(items));
                        cmbUnitType.getEditor().setItem(text);
                    }
                    
                    if (text != null && !text.trim().isEmpty() && found && editor.hasFocus() && cmbUnitType.isShowing()) {
                        if (!cmbUnitType.isPopupVisible()) {
                            try {
                                cmbUnitType.showPopup();
                            } catch (java.awt.IllegalComponentStateException ignored) {}
                        }
                    } else {
                        cmbUnitType.hidePopup();
                    }
                });
            }
        });
    }

    public void loadUnits() {
        allUnitsList = unitDAO.getAllUnits();
        String currentSel = cmbUnitType.getSelectedItem() != null ? cmbUnitType.getSelectedItem().toString().trim() : "";
        java.util.Vector<String> items = new java.util.Vector<>();
        for (com.store.billing.model.Unit u : allUnitsList) {
            items.add(u.getNameEn());
        }
        cmbUnitType.setModel(new DefaultComboBoxModel<>(items));
        if (!currentSel.isEmpty()) {
            cmbUnitType.setSelectedItem(currentSel);
        } else if (cmbUnitType.getItemCount() > 0) {
            cmbUnitType.setSelectedIndex(0);
        }
    }

    private void openUnitManagementDialog() {
        Window win = SwingUtilities.getWindowAncestor(this);
        UnitManagementDialog dlg = new UnitManagementDialog(win, () -> {
            loadUnits();
        });
        dlg.setVisible(true);
    }

    public void loadCategories() {
        allCategoriesList = productDAO.getAllCategories();
        
        // Populate Top Filter
        String currentFilterSel = (String) cmbFilterCategory.getSelectedItem();
        cmbFilterCategory.removeAllItems();
        cmbFilterCategory.addItem("All Categories");
        for (String cat : allCategoriesList) {
            cmbFilterCategory.addItem(cat);
        }
        if (currentFilterSel != null) {
            cmbFilterCategory.setSelectedItem(currentFilterSel);
        }

        // Populate Form Category ComboBox
        String currentFormSel = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "";
        cmbCategory.removeAllItems();
        for (String cat : allCategoriesList) {
            cmbCategory.addItem(cat);
        }
        if (!currentFormSel.isEmpty()) {
            cmbCategory.setSelectedItem(currentFormSel);
        }
    }

    public void loadProducts() {
        // Show a loading cursor
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        javax.swing.SwingWorker<List<Product>, Void> worker = new javax.swing.SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() throws Exception {
                return productDAO.getAllProducts();
            }

            @Override
            protected void done() {
                try {
                    allProductsList = get();
                    productTable.setRowSorter(null); // Detach sorter to prevent O(N^2) sorting bottleneck
                    tableModel.setRowCount(0);
                    for (Product p : allProductsList) {
                        tableModel.addRow(new Object[]{
                                p.getId(),
                                p.getBarcode(),
                                p.getCategory(),
                                p.getName(),
                                p.getDisplayName(),
                                p.getTamilName(),
                                p.getEnglishName(),
                                p.getUnitType(),
                                p.getPurchaseRate() > 0 ? p.getPurchaseRate() : 0.0,
                                p.getLineRate() > 0 ? p.getLineRate() : 0.0,
                                p.getWholePrice() > 0 ? p.getWholePrice() : 0.0,
                                p.getWholeDis() > 0 ? p.getWholeDis() : 0.0,
                                p.getWholeQtyThreshold(),
                                p.getRetailPrice() > 0 ? p.getRetailPrice() : 0.0,
                                p.getRetailDis() > 0 ? p.getRetailDis() : 0.0,
                                p.getRetailQtyThreshold(),
                                p.getStockQuantity()
                        });
                    }
                    productTable.setRowSorter(rowSorter); // Reattach sorter
                    applyCategoryAndSearchFilter();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void applyCategoryAndSearchFilter() {
        String selectedCategory = (String) cmbFilterCategory.getSelectedItem();
        String searchText = txtSearch.getText().trim().toLowerCase();

        boolean filterCategory = selectedCategory != null && !selectedCategory.isEmpty() && !"All Categories".equalsIgnoreCase(selectedCategory);

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (filterCategory) {
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    Object catVal = entry.getValue(2); // Category column index 2
                    return catVal != null && selectedCategory.equalsIgnoreCase(catVal.toString().trim());
                }
            });
        }

        if (!searchText.isEmpty()) {
            java.util.List<String> parsedTokens = com.store.billing.util.ProductSearchUtil.parseQuery(searchText, allProductsList).tokens;
            String[] tokens = parsedTokens.toArray(new String[0]);
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<?, ?> entry) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < entry.getValueCount(); i++) {
                        Object val = entry.getValue(i);
                        if (val != null) {
                            sb.append(val.toString().toLowerCase()).append(" ");
                        }
                    }
                    String combined = sb.toString();
                    String normCombined = com.store.billing.util.ProductSearchUtil.normalizeText(combined);
                    for (String token : tokens) {
                        boolean matched = false;
                        List<String> variants = com.store.billing.util.ProductSearchUtil.expandTokenVariants(token);
                        for (String v : variants) {
                            if (combined.contains(v) || normCombined.contains(v)) {
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            return false;
                        }
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

        int visibleCount = productTable.getRowCount();
        int totalCount = allProductsList.size();
        String catName = filterCategory ? selectedCategory : "All Categories";
        lblProductStats.setText("Showing: " + visibleCount + " / " + totalCount + " (" + catName + ")");
    }

    private void populateFormFromModelRow(int modelRow) {
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return;

        selectedProductId = (int) tableModel.getValueAt(modelRow, 0);
        txtCode.setText(getStr(tableModel.getValueAt(modelRow, 1)));
        
        String cat = getStr(tableModel.getValueAt(modelRow, 2));
        cmbCategory.setSelectedItem(cat);

        txtName.setText(getStr(tableModel.getValueAt(modelRow, 3)));
        txtDisplayName.setText(getStr(tableModel.getValueAt(modelRow, 4)));
        txtTamilName.setText(getStr(tableModel.getValueAt(modelRow, 5)));
        txtEnglishName.setText(getStr(tableModel.getValueAt(modelRow, 6)));

        String unit = getStr(tableModel.getValueAt(modelRow, 7));
        cmbUnitType.setSelectedItem(unit.isEmpty() ? "piece" : unit);

        txtPurchaseRate.setText(getNumStr(tableModel.getValueAt(modelRow, 8)));
        txtLineRate.setText(getNumStr(tableModel.getValueAt(modelRow, 9)));
        txtWholePrice.setText(getNumStr(tableModel.getValueAt(modelRow, 10)));
        txtWholeDis.setText(getNumStr(tableModel.getValueAt(modelRow, 11)));
        txtWholeQtyThreshold.setText(getIntStr(tableModel.getValueAt(modelRow, 12)));
        txtRetailPrice.setText(getNumStr(tableModel.getValueAt(modelRow, 13)));
        txtRetailDis.setText(getNumStr(tableModel.getValueAt(modelRow, 14)));
        txtRetailQtyThreshold.setText(getIntStr(tableModel.getValueAt(modelRow, 15)));
        txtStock.setText(getIntStr(tableModel.getValueAt(modelRow, 16)));

        lblFormStatus.setText(" Mode: Editing Product ID #" + selectedProductId + " - " + txtName.getText() + " (Category: " + cat + ")");
        lblFormStatus.setForeground(new Color(41, 128, 185));
    }

    private void addProduct() {
        try {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Saving Name / Product Name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtName.requestFocusInWindow();
                return;
            }

            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                code = "P" + System.currentTimeMillis(); // Auto-generate code
            }

            String category = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
            String unit = cmbUnitType.getSelectedItem() != null ? cmbUnitType.getSelectedItem().toString().trim() : "piece";
            String displayName = txtDisplayName.getText().trim();
            String tamilName = txtTamilName.getText().trim();
            String englishName = txtEnglishName.getText().trim();

            if (displayName.isEmpty()) displayName = !tamilName.isEmpty() ? tamilName : name;
            if (englishName.isEmpty()) englishName = name;

            double purchaseRate = parseDoubleSafe(txtPurchaseRate.getText());
            double lineRate = parseDoubleSafe(txtLineRate.getText());
            double wholePrice = parseDoubleSafe(txtWholePrice.getText());
            double wholeDis = parseDoubleSafe(txtWholeDis.getText());
            int wholeQty = parseIntSafe(txtWholeQtyThreshold.getText());
            double retailPrice = parseDoubleSafe(txtRetailPrice.getText());
            double retailDis = parseDoubleSafe(txtRetailDis.getText());
            int retailQty = parseIntSafe(txtRetailQtyThreshold.getText());
            int stock = parseIntSafe(txtStock.getText());
            if (stock == 0 && txtStock.getText().trim().isEmpty()) stock = 100;

            Product product = new Product(
                    0, code, name, stock, unit, tamilName,
                    purchaseRate, lineRate, wholePrice, wholeDis,
                    retailPrice, retailDis, wholeQty, retailQty,
                    category, displayName, englishName
            );

            if (productDAO.addProduct(product)) {
                JOptionPane.showMessageDialog(this, "Product '" + name + "' added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadCategories();
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add product. Please verify database connection.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to update.", "No Product Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Saving Name / Product Name cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                txtName.requestFocusInWindow();
                return;
            }

            String code = txtCode.getText().trim();
            String category = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
            String unit = cmbUnitType.getSelectedItem() != null ? cmbUnitType.getSelectedItem().toString().trim() : "piece";
            String displayName = txtDisplayName.getText().trim();
            String tamilName = txtTamilName.getText().trim();
            String englishName = txtEnglishName.getText().trim();

            if (displayName.isEmpty()) displayName = !tamilName.isEmpty() ? tamilName : name;
            if (englishName.isEmpty()) englishName = name;

            double purchaseRate = parseDoubleSafe(txtPurchaseRate.getText());
            double lineRate = parseDoubleSafe(txtLineRate.getText());
            double wholePrice = parseDoubleSafe(txtWholePrice.getText());
            double wholeDis = parseDoubleSafe(txtWholeDis.getText());
            int wholeQty = parseIntSafe(txtWholeQtyThreshold.getText());
            double retailPrice = parseDoubleSafe(txtRetailPrice.getText());
            double retailDis = parseDoubleSafe(txtRetailDis.getText());
            int retailQty = parseIntSafe(txtRetailQtyThreshold.getText());
            int stock = parseIntSafe(txtStock.getText());

            Product product = new Product(
                    selectedProductId, code, name, stock, unit, tamilName,
                    purchaseRate, lineRate, wholePrice, wholeDis,
                    retailPrice, retailDis, wholeQty, retailQty,
                    category, displayName, englishName
            );

            if (productDAO.updateProduct(product)) {
                JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadCategories();
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update product.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating product: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to delete.", "No Product Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = txtName.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this product?\n\nID: " + selectedProductId + "\nName: " + name,
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (productDAO.deleteProduct(selectedProductId)) {
                JOptionPane.showMessageDialog(this, "Product '" + name + "' deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadCategories();
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete product. It may be referenced in existing bills.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        selectedProductId = -1;
        txtCode.setText("");
        txtName.setText("");
        txtDisplayName.setText("");
        txtTamilName.setText("");
        txtEnglishName.setText("");
        txtPurchaseRate.setText("");
        txtLineRate.setText("");
        txtWholePrice.setText("");
        txtWholeDis.setText("");
        txtWholeQtyThreshold.setText("");
        txtRetailPrice.setText("");
        txtRetailDis.setText("");
        txtRetailQtyThreshold.setText("");
        txtStock.setText("100");

        if (cmbUnitType.getItemCount() > 0) cmbUnitType.setSelectedIndex(0);
        if (cmbCategory.getItemCount() > 0) cmbCategory.setSelectedIndex(0);

        productTable.clearSelection();
        lblFormStatus.setText(" Mode: Ready to Add New Product (Enter details below)");
        lblFormStatus.setForeground(new Color(46, 125, 50));
        txtCode.requestFocusInWindow();
    }

    private void handleExcelSync() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Do you want to sync/update products from the Excel file?\n(src/main/java/com/store/billing/assets/bill_list_tamil_print_names-5.xlsx)\n\nThis will update all categories, prices, display names and print names.",
                "Sync Excel Products",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            String path = "src/main/java/com/store/billing/assets/bill_list_tamil_print_names-5.xlsx";
            File f = new File(path);
            if (!f.exists()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Product Excel File (.xlsx)");
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    path = chooser.getSelectedFile().getAbsolutePath();
                } else {
                    return;
                }
            }

            final String finalPath = path;
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ExcelParser.syncExcelToDatabase(finalPath, "jdbc:sqlite:store_billing.db");
                    return null;
                }

                @Override
                protected void done() {
                    setCursor(Cursor.getDefaultCursor());
                    try {
                        get();
                        JOptionPane.showMessageDialog(InventoryPanel.this, "Excel synchronization completed successfully!", "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
                        loadCategories();
                        loadProducts();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(InventoryPanel.this, "Sync failed: " + ex.getMessage(), "Sync Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void setupEnterKeyNavigation() {
        txtCode.addActionListener(e -> {
            if (cmbCategory.isEditable()) {
                Component ed = cmbCategory.getEditor().getEditorComponent();
                if (ed != null) ed.requestFocusInWindow();
            } else {
                txtName.requestFocusInWindow();
            }
        });

        txtName.addActionListener(e -> txtDisplayName.requestFocusInWindow());
        txtDisplayName.addActionListener(e -> txtTamilName.requestFocusInWindow());
        txtTamilName.addActionListener(e -> txtEnglishName.requestFocusInWindow());
        txtEnglishName.addActionListener(e -> txtWholePrice.requestFocusInWindow());
        txtWholePrice.addActionListener(e -> txtRetailPrice.requestFocusInWindow());
        txtRetailPrice.addActionListener(e -> txtStock.requestFocusInWindow());
        txtStock.addActionListener(e -> {
            if (selectedProductId == -1) {
                addProduct();
            } else {
                updateProduct();
            }
        });
        
        setupArrowKeyNavigation();
    }

    private void setupArrowKeyNavigation() {
        com.store.billing.util.ArrowKeyNavigationUtil.bindGrid(new Component[][]{
            { cmbFilterCategory, txtSearch, txtSearch, btnResetFilter, btnSyncExcel },
            { txtCode, cmbCategory, txtName, cmbUnitType },
            { txtDisplayName, txtTamilName, txtEnglishName, txtStock },
            { txtPurchaseRate, txtLineRate, txtWholePrice, txtWholeDis },
            { txtWholeQtyThreshold, txtRetailPrice, txtRetailDis, txtRetailQtyThreshold },
            { btnClear, btnAdd, btnUpdate, btnDelete }
        });
    }

    private double parseDoubleSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return (int) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    private String getNumStr(Object obj) {
        if (obj == null) return "";
        try {
            double d = Double.parseDouble(obj.toString());
            return d > 0 ? String.format("%.2f", d) : "";
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String getIntStr(Object obj) {
        if (obj == null) return "";
        try {
            int i = Integer.parseInt(obj.toString());
            return i > 0 ? String.valueOf(i) : "";
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
