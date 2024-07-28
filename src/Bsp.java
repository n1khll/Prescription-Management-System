import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.poi.xwpf.usermodel.*;
public class Bsp extends JFrame {
    private JComboBox<String> categoryComboBox;
    private JButton selectCategoryButton;
    private JButton selectMedicineButton;
    private JButton cancelButton;
    private JTable medicineTable;
    private DefaultTableModel tableModel;
    private Connection connection;
    private int selectedCategoryID;
    private ArrayList<Map<String, String>> selectedMedicines = new ArrayList<>();
    public Bsp() {
        setTitle("Medicine Selection");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        // Initialize components
        categoryComboBox = new JComboBox<>();
        selectCategoryButton = new JButton("Select Category");
        selectMedicineButton = new JButton("Select Medicine");
        cancelButton = new JButton("Cancel");
        tableModel = new DefaultTableModel();
        tableModel.addColumn("Medicine ID");
        tableModel.addColumn("Medicine Name");
        tableModel.addColumn("Frequency");
        medicineTable = new JTable(tableModel);
        // Add components to the frame
        JPanel topPanel = new JPanel();
        topPanel.add(categoryComboBox);
        topPanel.add(selectCategoryButton);
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(medicineTable), BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(selectMedicineButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);
        // Connect to the database
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:C:" +
                    "//sqlite//proj.db");
            populateCategoryComboBox();
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            System.exit(1);
        }
        // Set action listener for select category button
        selectCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectCategory();
            }
        });
        // Set action listener for select medicine button
        selectMedicineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectMedicine();
            }
        });
        // Set action listener for cancel button
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displaySelectedMedicines();
                exportToDocx();
            }
        });
    }
    private void populateCategoryComboBox() {
        try {
            Statement statement = connection.createStatement();
            ResultSet categoriesResult = statement.executeQuery("SELECT * FROM Categories");
            while (categoriesResult.next()) {
                categoryComboBox.addItem(categoriesResult.getInt("CategoryID") + ". " +
                        categoriesResult.getString("CategoryName"));
            }
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
    }
    private void selectCategory() {
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        selectedCategoryID = Integer.parseInt(selectedCategory.split("\\.")[0].trim());
        displayMedicinesForCategory(selectedCategoryID);
    }
    private void selectMedicine() {
        int selectedMedicineID = Integer.parseInt(JOptionPane.showInputDialog("Enter the MedicineID" +
                " of the medicine you want to select:"));
        try {
            PreparedStatement checkMedicineCategoryStatement = connection.prepareStatement("SELECT" +
                    " CategoryID FROM Medicines WHERE MedicineID = ?");
            checkMedicineCategoryStatement.setInt(1, selectedMedicineID);
            ResultSet checkMedicineCategoryResult = checkMedicineCategoryStatement.executeQuery();
            if (checkMedicineCategoryResult.next()) {
                int medicineCategoryID = checkMedicineCategoryResult.getInt("CategoryID");
                if (medicineCategoryID != selectedCategoryID) {
                    JOptionPane.showMessageDialog(this, "Invalid choice." +
                            " The selected medicine does not belong to the chosen category.", "Error"
                            , JOptionPane.ERROR_MESSAGE);
                } else {
                    if (isMedicineInStock(selectedMedicineID)) {
                        displaySelectedMedicine(selectedMedicineID);
                    } else {
                        JOptionPane.showMessageDialog(this, "The selected medicine" +
                                " is not in stock.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid MedicineID." +
                        " Please enter a valid MedicineID.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching medicine details: " + e.getMessage());
        }
    }
    private boolean isMedicineInStock(int medicineID) {
        try {
            PreparedStatement stockStatement = connection.prepareStatement("SELECT Quantity" +
                    " FROM Stock WHERE MedicineID = ?");
            stockStatement.setInt(1, medicineID);
            ResultSet stockResult = stockStatement.executeQuery();
            if (stockResult.next()) {
                int quantity = stockResult.getInt("Quantity");
                return quantity > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stock quantity: " + e.getMessage());
        }
        return false;
    }
    private void displayMedicinesForCategory(int categoryId) {
        tableModel.setRowCount(0);
        try {
            PreparedStatement medicinesStatement = connection.prepareStatement("SELECT * FROM" +
                    " Medicines WHERE CategoryID = ?");
            medicinesStatement.setInt(1, categoryId);
            ResultSet medicinesResult = medicinesStatement.executeQuery();

            while (medicinesResult.next()) {
                int medicineID = medicinesResult.getInt("MedicineID");
                String medicineName = medicinesResult.getString("MedicineName");
                int quantity = getStockQuantity(medicineID);
                if (quantity > 0) {
                    tableModel.addRow(new Object[]{medicineID, medicineName, getFrequency(medicineID)});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching medicines: " + e.getMessage());
        }
    }
    private void displaySelectedMedicine(int medicineID) {
        try {
            PreparedStatement medicineInfoStatement = connection.prepareStatement("SELECT MedicineName," +
                    " Frequency FROM Medicines WHERE MedicineID = ?");
            medicineInfoStatement.setInt(1, medicineID);
            ResultSet medicineInfoResult = medicineInfoStatement.executeQuery();
            if (medicineInfoResult.next()) {
                String medicineName = medicineInfoResult.getString("MedicineName");
                String frequency = medicineInfoResult.getString("Frequency");
                selectedMedicines.add(Map.of("MedicineName", medicineName, "Frequency", frequency));
                JOptionPane.showMessageDialog(this, "You have selected: " + medicineName,
                        "Medicine Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching medicine details: " + e.getMessage());
        }
    }
    private void displaySelectedMedicines() {
        JFrame selectedMedicinesFrame = new JFrame("Selected Medicines");
        selectedMedicinesFrame.setSize(400, 300);
        selectedMedicinesFrame.setLayout(new BorderLayout());
        JTable selectedMedicinesTable = new JTable();
        DefaultTableModel selectedMedicinesModel = new DefaultTableModel();
        selectedMedicinesModel.addColumn("Medicine Name");
        selectedMedicinesModel.addColumn("Frequency");
        for (Map<String, String> medicineInfo : selectedMedicines) {
            selectedMedicinesModel.addRow(new Object[]{medicineInfo.get("MedicineName"),
                    medicineInfo.get("Frequency")});
        }
        selectedMedicinesTable.setModel(selectedMedicinesModel);
        selectedMedicinesFrame.add(new JScrollPane(selectedMedicinesTable), BorderLayout.CENTER);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedMedicinesFrame.dispose();
            }
        });
        selectedMedicinesFrame.add(closeButton, BorderLayout.SOUTH);
        selectedMedicinesFrame.setVisible(true);
    }
    private int getStockQuantity(int medicineID) {
        try {
            PreparedStatement stockStatement = connection.prepareStatement("SELECT Quantity" +
                    " FROM Stock WHERE MedicineID = ?");
            stockStatement.setInt(1, medicineID);
            ResultSet stockResult = stockStatement.executeQuery();
            if (stockResult.next()) {
                return stockResult.getInt("Quantity");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching stock quantity: " + e.getMessage());
        }
        return 0;
    }
    private String getFrequency(int medicineID) {
        try {
            PreparedStatement frequencyStatement = connection.prepareStatement("SELECT Frequency" +
                    " FROM Medicines WHERE MedicineID = ?");
            frequencyStatement.setInt(1, medicineID);
            ResultSet frequencyResult = frequencyStatement.executeQuery();
            if (frequencyResult.next()) {
                return frequencyResult.getString("Frequency");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching frequency: " + e.getMessage());
        }
        return "";
    }
    private void exportToDocx() {
        try (FileOutputStream outputStream = new FileOutputStream("prescription.docx")) {
            XWPFDocument document = new XWPFDocument();
            XWPFTable table = document.createTable(selectedMedicines.size() + 1, 2);
            table.setWidth("100%");
            table.setCellMargins(100, 100, 100, 100);
            table.getRow(0).getCell(0).setText("Medicine Name");
            table.getRow(0).getCell(1).setText("Frequency (times/day)");
            for (int i = 0; i < selectedMedicines.size(); i++) {
                Map<String, String> medicineInfo = selectedMedicines.get(i);
                XWPFTableRow row = table.getRow(i + 1);
                row.getCell(0).setText(medicineInfo.get("MedicineName"));
                row.getCell(1).setText(medicineInfo.get("Frequency"));
            }
            document.write(outputStream);
            System.out.println("Prescription exported successfully to 'prescription.docx'");
        } catch (IOException e) {
            System.err.println("Error exporting prescription: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Bsp().setVisible(true);
            }
        });
    }
}
