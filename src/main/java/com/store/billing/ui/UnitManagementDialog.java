package com.store.billing.ui;

import com.store.billing.dao.UnitDAO;
import com.store.billing.model.Unit;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class UnitManagementDialog extends JDialog {

    private final UnitDAO unitDAO = new UnitDAO();
    private final Runnable onUnitsChanged;

    private JTable unitTable;
    private DefaultTableModel tableModel;
    private JTextField txtNameEn;
    private JTextField txtNameTa;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;
    private int selectedUnitId = -1;

    private static final Font TAMIL_FONT = new Font("Nirmala UI", Font.PLAIN, 14);
    private static final Font BOLD_FONT = new Font("Nirmala UI", Font.BOLD, 13);

    public UnitManagementDialog(Window owner, Runnable onUnitsChanged) {
        super(owner, "Manage Units (அலகுகள் மேலாண்மை)", ModalityType.APPLICATION_MODAL);
        this.onUnitsChanged = onUnitsChanged;
        initComponents();
        loadUnits();
        setSize(650, 520);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        JLabel lblTitle = new JLabel("Unit Management / அலகுகள் மேலாண்மை");
        lblTitle.setFont(new Font("Nirmala UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(25, 118, 210));
        JLabel lblSub = new JLabel("Add or update product units with both English and Tamil names for accurate billing & printing.");
        lblSub.setFont(new Font("Nirmala UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // Center Panel: Split into Input Form and Table
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder("Unit Details / அலகு விவரங்கள்");
        tb.setTitleFont(new Font("Nirmala UI", Font.BOLD, 12));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                tb,
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // English Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblEn = new JLabel("English Unit (ஆங்கிலம்) *:");
        lblEn.setFont(BOLD_FONT);
        formPanel.add(lblEn, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtNameEn = new JTextField();
        txtNameEn.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        txtNameEn.setPreferredSize(new Dimension(160, 32));
        formPanel.add(txtNameEn, gbc);

        // Tamil Name
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        JLabel lblTa = new JLabel("Tamil Unit (தமிழ்) *:");
        lblTa.setFont(BOLD_FONT);
        formPanel.add(lblTa, gbc);

        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtNameTa = new JTextField();
        txtNameTa.setFont(TAMIL_FONT);
        txtNameTa.setPreferredSize(new Dimension(160, 32));
        formPanel.add(txtNameTa, gbc);

        // Buttons row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnAdd = new JButton("Add Unit (சேர்க்க)");
        btnAdd.setFont(BOLD_FONT);
        btnAdd.setBackground(new Color(46, 125, 50));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> addUnit());

        btnUpdate = new JButton("Update (மாற்றுக)");
        btnUpdate.setFont(BOLD_FONT);
        btnUpdate.setBackground(new Color(21, 101, 192));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setEnabled(false);
        btnUpdate.addActionListener(e -> updateUnit());

        btnDelete = new JButton("Delete (நீக்குக)");
        btnDelete.setFont(BOLD_FONT);
        btnDelete.setBackground(new Color(198, 40, 40));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteUnit());

        btnClear = new JButton("Clear (அழிக்க)");
        btnClear.setFont(BOLD_FONT);
        btnClear.addActionListener(e -> clearForm());

        btnRow.add(btnAdd);
        btnRow.add(btnUpdate);
        btnRow.add(btnDelete);
        btnRow.add(btnClear);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        formPanel.add(btnRow, gbc);

        centerPanel.add(formPanel, BorderLayout.NORTH);

        // Table Panel
        String[] cols = { "ID", "Unit (English / ஆங்கிலம்)", "Unit (Tamil / தமிழ்)" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        unitTable = new JTable(tableModel);
        unitTable.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        unitTable.setRowHeight(28);
        unitTable.getTableHeader().setFont(BOLD_FONT);
        unitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        unitTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        unitTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        unitTable.getColumnModel().getColumn(2).setPreferredWidth(250);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        unitTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer tamilCellRenderer = new DefaultTableCellRenderer();
        tamilCellRenderer.setFont(new Font("Nirmala UI", Font.PLAIN, 14));
        unitTable.getColumnModel().getColumn(1).setCellRenderer(tamilCellRenderer);
        unitTable.getColumnModel().getColumn(2).setCellRenderer(tamilCellRenderer);

        unitTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = unitTable.getSelectedRow();
                if (row >= 0) {
                    populateFormFromRow(row);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(unitTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Bottom Close Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Close (மூடுக)");
        btnClose.setFont(BOLD_FONT);
        btnClose.addActionListener(e -> dispose());
        bottomPanel.add(btnClose);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadUnits() {
        tableModel.setRowCount(0);
        List<Unit> units = unitDAO.getAllUnits();
        for (Unit u : units) {
            tableModel.addRow(new Object[]{
                u.getId(),
                u.getNameEn(),
                u.getNameTa()
            });
        }
    }

    private void populateFormFromRow(int row) {
        selectedUnitId = (int) tableModel.getValueAt(row, 0);
        txtNameEn.setText((String) tableModel.getValueAt(row, 1));
        txtNameTa.setText((String) tableModel.getValueAt(row, 2));
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void clearForm() {
        selectedUnitId = -1;
        txtNameEn.setText("");
        txtNameTa.setText("");
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        unitTable.clearSelection();
        txtNameEn.requestFocusInWindow();
    }

    private void addUnit() {
        String en = txtNameEn.getText().trim();
        String ta = txtNameTa.getText().trim();

        if (en.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter English unit name (e.g. roll, bundle, piece)", "Input Required", JOptionPane.WARNING_MESSAGE);
            txtNameEn.requestFocusInWindow();
            return;
        }
        if (ta.isEmpty()) {
            ta = en; // fallback to English if Tamil is empty
        }

        boolean success = unitDAO.addUnit(en, ta);
        if (success) {
            loadUnits();
            clearForm();
            if (onUnitsChanged != null) {
                onUnitsChanged.run();
            }
            JOptionPane.showMessageDialog(this, "Unit added successfully!\nஅலகு வெற்றிகரமாக சேர்க்கப்பட்டது!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add unit. The unit might already exist.\nஅலகு சேர்ப்பதில் தோல்வி. ஏற்கனவே இருக்கலாம்.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUnit() {
        if (selectedUnitId <= 0) return;
        String en = txtNameEn.getText().trim();
        String ta = txtNameTa.getText().trim();

        if (en.isEmpty()) {
            JOptionPane.showMessageDialog(this, "English unit name cannot be empty.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ta.isEmpty()) {
            ta = en;
        }

        boolean success = unitDAO.updateUnit(selectedUnitId, en, ta);
        if (success) {
            loadUnits();
            clearForm();
            if (onUnitsChanged != null) {
                onUnitsChanged.run();
            }
            JOptionPane.showMessageDialog(this, "Unit updated successfully!\nஅலகு வெற்றிகரமாக மாற்றப்பட்டது!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update unit.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUnit() {
        if (selectedUnitId <= 0) return;
        int opt = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete unit '" + txtNameEn.getText() + "'?\nஇந்த அலகை நிச்சயம் நீக்க விரும்புகிறீர்களா?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (opt == JOptionPane.YES_OPTION) {
            boolean success = unitDAO.deleteUnit(selectedUnitId);
            if (success) {
                loadUnits();
                clearForm();
                if (onUnitsChanged != null) {
                    onUnitsChanged.run();
                }
                JOptionPane.showMessageDialog(this, "Unit deleted successfully!\nஅலகு வெற்றிகரமாக நீக்கப்பட்டது!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete unit.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
