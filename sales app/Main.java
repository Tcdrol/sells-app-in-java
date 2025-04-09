import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.io.*;
import java.util.Properties;

public class Main {
    private JFrame frame;
    private Map<String, Double> products;
    private Map<String, Integer> inventory;
    private JFrame loginFrame;
    private Map<String, String> userPasswords;
    private static final String DATA_DIR = "data";
    private static final String PRODUCTS_FILE = "data/products.dat";
    private static final String INVENTORY_FILE = "data/inventory.dat";
    private static final String USERS_FILE = "data/users.properties";

    public Main() {
        // Create data directory if it doesn't exist
        new File(DATA_DIR).mkdirs();
        
        products = new HashMap<>();
        inventory = new HashMap<>();
        userPasswords = new HashMap<>();
        
        // Load data from files
        loadData();
        
        // If no users exist, create default ones
        if (userPasswords.isEmpty()) {
            userPasswords.put("admin", "admin123");
            userPasswords.put("cashier", "cash123");
            saveUsers();
        }
        
        showLoginScreen();
    }

    private void loadData() {
        loadUsers();
        loadProducts();
        loadInventory();
    }

    private void loadUsers() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(USERS_FILE)) {
            props.load(fis);
            props.forEach((key, value) -> userPasswords.put((String)key, (String)value));
        } catch (IOException e) {
            System.out.println("No existing users file found. Will create new one.");
        }
    }

    private void saveUsers() {
        Properties props = new Properties();
        props.putAll(userPasswords);
        try (FileOutputStream fos = new FileOutputStream(USERS_FILE)) {
            props.store(fos, "User Credentials");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving users data!");
        }
    }

    private void loadProducts() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(PRODUCTS_FILE))) {
            @SuppressWarnings("unchecked")
            Map<String, Double> loadedProducts = (Map<String, Double>) ois.readObject();
            products.putAll(loadedProducts);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing products file found. Will create new one.");
        }
    }

    private void saveProducts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PRODUCTS_FILE))) {
            oos.writeObject(products);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving products data!");
        }
    }

    private void loadInventory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(INVENTORY_FILE))) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> loadedInventory = (Map<String, Integer>) ois.readObject();
            inventory.putAll(loadedInventory);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing inventory file found. Will create new one.");
        }
    }

    private void saveInventory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(INVENTORY_FILE))) {
            oos.writeObject(inventory);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error saving inventory data!");
        }
    }

    public void saveAllData() {
        saveUsers();
        saveProducts();
        saveInventory();
    }

    private void showLoginScreen() {
        loginFrame = new JFrame("Login - Sales Management System");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 200);
        loginFrame.setLayout(new BorderLayout());
        loginFrame.setLocationRelativeTo(null);

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridLayout(4, 1, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPasswordField passwordField = new JPasswordField();
        JButton adminButton = new JButton("Login as Admin");
        JButton cashierButton = new JButton("Login as Cashier");

        adminButton.addActionListener(e -> {
            if (verifyPassword("admin", passwordField.getPassword())) {
                loginFrame.dispose();
                initializeGUI("admin");
            } else {
                JOptionPane.showMessageDialog(loginFrame, 
                    "Invalid password for admin", 
                    "Login Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cashierButton.addActionListener(e -> {
            if (verifyPassword("cashier", passwordField.getPassword())) {
                loginFrame.dispose();
                initializeGUI("cashier");
            } else {
                JOptionPane.showMessageDialog(loginFrame, 
                    "Invalid password for cashier", 
                    "Login Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        loginPanel.add(new JLabel("Select your role:", SwingConstants.CENTER));
        loginPanel.add(new JLabel("Enter Password:", SwingConstants.CENTER));
        loginPanel.add(passwordField);
        loginPanel.add(new JPanel() {{
            setLayout(new GridLayout(1, 2, 5, 0));
            add(adminButton);
            add(cashierButton);
        }});

        loginFrame.add(loginPanel);
        loginFrame.setVisible(true);
    }

    private boolean verifyPassword(String role, char[] password) {
        String correctPassword = userPasswords.get(role);
        return correctPassword != null && correctPassword.equals(new String(password));
    }

    private void initializeGUI(String role) {
        frame = new JFrame("Sales Management System - " + role.toUpperCase());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLayout(new GridLayout(1, 2));
        frame.setLocationRelativeTo(null);

        // Add window listener to save data before closing
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveAllData();
                frame.dispose();
                System.exit(0);
            }
        });

        Admin adminPanel = new Admin(products, inventory, frame);
        Cashier cashierPanel = new Cashier(products, inventory, 
                                         adminPanel.getProductListModel(), frame);

        if (role.equals("admin")) {
            frame.add(adminPanel);
        } else {
            frame.add(cashierPanel);
        }

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new Main());
    }
}

