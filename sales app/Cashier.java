import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.util.Properties;

public class Cashier extends JPanel {
    private JTextField searchField;
    private JButton searchBtn, addToCartBtn, removeFromCartBtn, clearCartBtn, checkoutBtn;
    private JList<String> productList, cartList;
    private DefaultListModel<String> productListModel, cartListModel;
    private Map<String, Double> products;
    private Map<String, Integer> inventory;
    private Map<String, Integer> cartQuantities;
    private double total;
    private JLabel totalLabel;
    private JSpinner quantitySpinner;
    private ArrayList<String> salesHistory;
    private JTextArea salesHistoryArea;
    private JFrame parentFrame;

    public Cashier(Map<String, Double> products, Map<String, Integer> inventory, 
                  DefaultListModel<String> productListModel, JFrame parentFrame) {
        this.products = products;
        this.inventory = inventory;
        this.productListModel = productListModel;
        this.parentFrame = parentFrame;
        this.cartQuantities = new HashMap<>();
        this.salesHistory = new ArrayList<>();
        this.total = 0;
        initializePanel();
    }

    private void initializePanel() {
        this.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Cashier Panel", 
            TitledBorder.CENTER, TitledBorder.TOP));
        
        // Change to BorderLayout for main panel
        this.setLayout(new BorderLayout());

        // Create left panel for existing components
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Add logout button at the top
        JPanel logoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            parentFrame.dispose();
            new Main();
        });
        logoutPanel.add(logoutButton);
        leftPanel.add(logoutPanel);

        // Search Panel
        JPanel searchPanel = new JPanel();
        searchField = new JTextField(20);
        searchBtn = new JButton("Search");
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        leftPanel.add(searchPanel);

        // Cart Controls
        JPanel cartControlPanel = new JPanel();
        addToCartBtn = new JButton("Add to Cart");
        removeFromCartBtn = new JButton("Remove Selected");
        clearCartBtn = new JButton("Clear Cart");
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        cartControlPanel.add(new JLabel("Quantity:"));
        cartControlPanel.add(quantitySpinner);
        cartControlPanel.add(addToCartBtn);
        cartControlPanel.add(removeFromCartBtn);
        cartControlPanel.add(clearCartBtn);
        leftPanel.add(cartControlPanel);

        // Cart
        cartListModel = new DefaultListModel<>();
        cartList = new JList<>(cartListModel);
        cartList.setBorder(BorderFactory.createTitledBorder("Shopping Cart"));
        leftPanel.add(new JScrollPane(cartList));

        // Total and Checkout
        JPanel checkoutPanel = new JPanel();
        totalLabel = new JLabel("Total: $0.00");
        checkoutBtn = new JButton("Checkout");
        checkoutPanel.add(totalLabel);
        checkoutPanel.add(checkoutBtn);
        leftPanel.add(checkoutPanel);

        // Sales History
        salesHistoryArea = new JTextArea();
        salesHistoryArea.setEditable(false);
        JScrollPane historyScroll = new JScrollPane(salesHistoryArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Sales History"));
        leftPanel.add(historyScroll);

        // Create right panel for product list
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Available Products"));
        productList = new JList<>(productListModel);
        JScrollPane productScroll = new JScrollPane(productList);
        rightPanel.add(productScroll, BorderLayout.CENTER);

        // Add both panels to a split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.6); // Left panel gets 60% of the space
        
        // Add split pane to main panel
        this.add(splitPane, BorderLayout.CENTER);

        addActionListeners();
        
        // Initialize product list
        updateProductList();
    }

    private void addActionListeners() {
        searchBtn.addActionListener(e -> searchProduct());
        addToCartBtn.addActionListener(e -> addToCart());
        removeFromCartBtn.addActionListener(e -> removeFromCart());
        clearCartBtn.addActionListener(e -> clearCart());
        checkoutBtn.addActionListener(e -> checkout());

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchProduct();
            }
        });
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

    private void addToCart() {
        String selected = productList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(parentFrame, "Please select a product first!");
            return;
        }

        String productName = selected.split(" - ")[0];
        productName = productName.substring(productName.indexOf("] ") + 2);
        int quantity = (int) quantitySpinner.getValue();

        if (inventory.get(productName) < quantity) {
            JOptionPane.showMessageDialog(parentFrame, "Not enough stock available!");
            return;
        }

        // Update inventory
        inventory.put(productName, inventory.get(productName) - quantity);
        
        // Update cart
        cartListModel.addElement(String.format("%s x%d - $%.2f", 
            productName, quantity, products.get(productName) * quantity));
        cartQuantities.put(productName, cartQuantities.getOrDefault(productName, 0) + quantity);
        
        // Update total
        total += products.get(productName) * quantity;
        updateTotal();
        
        // Refresh product list
        searchProduct();
    }

    private void removeFromCart() {
        int selectedIndex = cartList.getSelectedIndex();
        if (selectedIndex != -1) {
            String cartItem = cartList.getSelectedValue();
            String[] parts = cartItem.split(" x");
            String productName = parts[0];
            int quantity = Integer.parseInt(parts[1].split(" -")[0]);

            // Return items to inventory
            inventory.put(productName, inventory.get(productName) + quantity);
            
            // Update total
            total -= products.get(productName) * quantity;
            updateTotal();

            cartListModel.remove(selectedIndex);
            cartQuantities.remove(productName);
            
            // Refresh product list
            searchProduct();
        }
    }

    private void clearCart() {
        // Return all items to inventory
        for (int i = 0; i < cartListModel.size(); i++) {
            String cartItem = cartListModel.get(i);
            String[] parts = cartItem.split(" x");
            String productName = parts[0];
            int quantity = Integer.parseInt(parts[1].split(" -")[0]);
            inventory.put(productName, inventory.get(productName) + quantity);
        }

        cartListModel.clear();
        cartQuantities.clear();
        total = 0;
        updateTotal();
        searchProduct();
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total: $%.2f", total));
    }

    private void checkout() {
        if (cartListModel.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "Cart is empty!");
            return;
        }

        // Generate receipt
        StringBuilder receipt = new StringBuilder();
        receipt.append("=== SALES RECEIPT ===\n");
        receipt.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
        
        for (int i = 0; i < cartListModel.size(); i++) {
            receipt.append(cartListModel.get(i)).append("\n");
        }
        
        receipt.append("\nTotal Amount: $").append(String.format("%.2f", total));
        
        // Add to sales history
        salesHistory.add(receipt.toString());
        updateSalesHistory();

        // Show receipt
        JOptionPane.showMessageDialog(parentFrame, receipt.toString());

        // Save data after successful checkout
        Main mainWindow = (Main) SwingUtilities.getWindowAncestor(this);
        if (mainWindow != null) {
            mainWindow.saveAllData();
        }

        // Clear cart
        cartListModel.clear();
        cartQuantities.clear();
        total = 0;
        updateTotal();
    }

    private void updateSalesHistory() {
        StringBuilder history = new StringBuilder();
        for (String sale : salesHistory) {
            history.append(sale).append("\n\n");
        }
        salesHistoryArea.setText(history.toString());
    }

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
} 