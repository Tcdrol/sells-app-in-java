import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

public class Admin extends JPanel {
    private JTextField productNameField, productPriceField, quantityField;
    private JButton addProductBtn;
    private JList<String> productList;
    private DefaultListModel<String> productListModel;
    private JComboBox<String> categoryComboBox;
    private Map<String, Double> products;
    private Map<String, Integer> inventory;
    private JFrame parentFrame;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton editProductBtn;
    private JButton deleteProductBtn;

    public Admin(Map<String, Double> products, Map<String, Integer> inventory, JFrame parentFrame) {
        this.products = products;
        this.inventory = inventory;
        this.parentFrame = parentFrame;
        initializePanel();
    }

    private void initializePanel() {
        this.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Admin Panel", 
            TitledBorder.CENTER, TitledBorder.TOP));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add logout button at the top
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            parentFrame.dispose();
            new Main();
        });
        logoutPanel.add(logoutButton);
        this.add(logoutPanel);

        // Add Search Panel
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        searchBtn = new JButton("Search");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        this.add(searchPanel);

        // Product Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Product Name
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Product Name:"), gbc);
        productNameField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(productNameField, gbc);

        // Product Price
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Product Price:"), gbc);
        productPriceField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(productPriceField, gbc);

        // Initial Quantity
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Initial Quantity:"), gbc);
        quantityField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(quantityField, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Category:"), gbc);
        categoryComboBox = new JComboBox<>(new String[]{"Electronics", "Food", "Clothing", "Other"});
        gbc.gridx = 1;
        inputPanel.add(categoryComboBox, gbc);

        // Initialize buttons first
        addProductBtn = new JButton("Add Product");
        editProductBtn = new JButton("Edit Selected");
        deleteProductBtn = new JButton("Delete Selected");

        // Add buttons to panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addProductBtn);
        buttonPanel.add(editProductBtn);
        buttonPanel.add(deleteProductBtn);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);

        this.add(inputPanel);

        // Product List
        productListModel = new DefaultListModel<>();
        productList = new JList<>(productListModel);
        productList.setBorder(BorderFactory.createTitledBorder("Product Inventory"));
        JScrollPane scrollPane = new JScrollPane(productList);
        scrollPane.setPreferredSize(new Dimension(300, 300));
        this.add(scrollPane);

        // Add all action listeners
        addActionListeners();
    }

    private void addActionListeners() {
        addProductBtn.addActionListener(e -> addProduct());
        editProductBtn.addActionListener(e -> editSelectedProduct());
        deleteProductBtn.addActionListener(e -> deleteSelectedProduct());
        searchBtn.addActionListener(e -> searchProduct());
        
        productList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = productList.getSelectedValue();
                if (selected != null) {
                    fillFieldsWithSelectedProduct(selected);
                }
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchProduct();
            }
        });
    }

    private void addProduct() {
        try {
            String name = productNameField.getText();
            double price = Double.parseDouble(productPriceField.getText());
            int quantity = Integer.parseInt(quantityField.getText());
            String category = (String) categoryComboBox.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame, "Please enter a product name!");
                return;
            }

            products.put(name, price);
            inventory.put(name, quantity);
            productListModel.addElement(String.format("[%s] %s - $%.2f (Stock: %d)", 
                category, name, price, quantity));

            // Save data after adding product
            Window ancestor = SwingUtilities.getWindowAncestor(this);
            if (ancestor instanceof Main) {
                ((Main) ancestor).saveAllData();
            }

            // Clear input fields
            productNameField.setText("");
            productPriceField.setText("");
            quantityField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(parentFrame, "Please enter valid numbers for price and quantity!");
        }
    }

    private void searchProduct() {
        String search = searchField.getText().toLowerCase();
        productListModel.clear();
        
        // Display all products from the products map
        for (Map.Entry<String, Double> entry : products.entrySet()) {
            String name = entry.getKey();
            double price = entry.getValue();
            int stock = inventory.get(name);
            
            if (name.toLowerCase().contains(search)) {
                String category = "Other"; // Default category
                // Try to find existing category for this product
                for (int i = 0; i < productListModel.size(); i++) {
                    String item = productListModel.getElementAt(i);
                    if (item.contains(name)) {
                        category = item.split("]")[0].substring(1);
                        break;
                    }
                }
                productListModel.addElement(String.format("[%s] %s - $%.2f (Stock: %d)", 
                    category, name, price, stock));
            }
        }
        
        // If search is empty, make sure all products are shown
        if (search.isEmpty()) {
            updateProductList();
        }
    }

    // Update the updateProductList method to show all products
    private void updateProductList() {
        productListModel.clear();
        for (Map.Entry<String, Double> entry : products.entrySet()) {
            String name = entry.getKey();
            double price = entry.getValue();
            int stock = inventory.get(name);
            productListModel.addElement(String.format("[Other] %s - $%.2f (Stock: %d)", 
                name, price, stock));
        }
    }

    private void fillFieldsWithSelectedProduct(String selected) {
        try {
            // Parse the selected item
            String[] parts = selected.split(" - ");
            String categoryAndName = parts[0];
            String priceStock = parts[1];
            
            // Extract category and name
            String category = categoryAndName.substring(1, categoryAndName.indexOf("]"));
            String name = categoryAndName.substring(categoryAndName.indexOf("]") + 2);
            
            // Extract price and stock
            double price = Double.parseDouble(priceStock.split("\\$")[1].split(" ")[0]);
            int stock = Integer.parseInt(priceStock.split("Stock: ")[1].replace(")", ""));
            
            // Fill the fields
            productNameField.setText(name);
            productPriceField.setText(String.format("%.2f", price));
            quantityField.setText(String.valueOf(stock));
            categoryComboBox.setSelectedItem(category);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentFrame, "Error parsing product details");
        }
    }

    private void editSelectedProduct() {
        String selected = productList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame, "Please select a product to edit!");
            return;
        }

        try {
            String oldName = selected.split("]")[1].trim().split(" -")[0];
            String newName = productNameField.getText();
            double newPrice = Double.parseDouble(productPriceField.getText());
            int newQuantity = Integer.parseInt(quantityField.getText());
            String newCategory = (String) categoryComboBox.getSelectedItem();

            // Remove old product
            products.remove(oldName);
            inventory.remove(oldName);

            // Add updated product
            products.put(newName, newPrice);
            inventory.put(newName, newQuantity);

            // Update the list
            searchProduct();

            // Save data after editing product
            Window ancestor = SwingUtilities.getWindowAncestor(this);
            if (ancestor instanceof Main) {
                ((Main) ancestor).saveAllData();
            }

            JOptionPane.showMessageDialog(parentFrame, "Product updated successfully!");
            
            // Clear input fields
            productNameField.setText("");
            productPriceField.setText("");
            quantityField.setText("");
            categoryComboBox.setSelectedIndex(0);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(parentFrame, "Please enter valid numbers for price and quantity!");
        }
    }

    private void deleteSelectedProduct() {
        String selected = productList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame, "Please select a product to delete!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(parentFrame, 
            "Are you sure you want to delete this product?", 
            "Confirm Deletion", 
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            String productName = selected.split("]")[1].trim().split(" -")[0];
            products.remove(productName);
            inventory.remove(productName);
            searchProduct();
            
            // Save data after deleting product
            Window ancestor = SwingUtilities.getWindowAncestor(this);
            if (ancestor instanceof Main) {
                ((Main) ancestor).saveAllData();
            }
            
            // Clear input fields
            productNameField.setText("");
            productPriceField.setText("");
            quantityField.setText("");
            categoryComboBox.setSelectedIndex(0);
            
            JOptionPane.showMessageDialog(parentFrame, "Product deleted successfully!");
        }
    }

    public DefaultListModel<String> getProductListModel() {
        return productListModel;
    }
} 