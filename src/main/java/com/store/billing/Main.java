package com.store.billing;

import com.store.billing.ui.MainFrame;
import com.store.billing.util.DatabaseConnection;

import javax.swing.*;
import java.awt.Font;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Set Look and Feel to FlatLaf for a modern native appearance
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            
            // Set Nirmala UI font globally to support Tamil Unicode input and display across all components
            Font tamilFont = new Font("Nirmala UI", Font.PLAIN, 14);
            Font tamilBoldFont = new Font("Nirmala UI", Font.BOLD, 14);

            UIManager.put("defaultFont", tamilFont);
            UIManager.put("TextField.font", tamilFont);
            UIManager.put("TextArea.font", tamilFont);
            UIManager.put("TextPane.font", tamilFont);
            UIManager.put("Label.font", tamilFont);
            UIManager.put("Button.font", tamilBoldFont);
            UIManager.put("Table.font", tamilFont);
            UIManager.put("Table.selectionForeground", new java.awt.Color(33, 37, 41));
            UIManager.put("TableHeader.font", tamilBoldFont);
            UIManager.put("ComboBox.font", tamilFont);
            UIManager.put("List.font", tamilFont);
            UIManager.put("TitledBorder.font", tamilBoldFont);
            UIManager.put("TabbedPane.font", tamilBoldFont);
            UIManager.put("CheckBox.font", tamilFont);
            UIManager.put("RadioButton.font", tamilFont);
            UIManager.put("Spinner.font", tamilFont);

            // Set UI properties for better look
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize FlatLaf");
        }

        // Test Database connection on startup
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("Connected to the database successfully!");
            } else {
                System.err.println("Failed to connect to the database.");
                JOptionPane.showMessageDialog(null, 
                    "Could not connect to database. Please check your MySQL setup.", 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Database Connection Error: " + e.getMessage() + "\nMake sure MySQL is running and schema is created.", 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }

        // Launch Application UI
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Initializing UI components...");
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                mainFrame.toFront();
                mainFrame.requestFocus();
                System.out.println("Main window displayed successfully!");
            } catch (Throwable t) {
                System.err.println("CRITICAL ERROR during UI initialization:");
                t.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Error launching application:\n" + t.toString() + "\n" + t.getMessage(), 
                    "Application Launch Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
