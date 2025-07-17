package com.cleanroommc.relauncher.gui;

import com.cleanroommc.relauncher.CleanroomRelauncher;
import com.sun.management.OperatingSystemMXBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MemorySettingsPanel extends JPanel {

    private final ResourceBundle resourceBundle;
    private JTextField maxMemoryTextField;
    private JTextField initialMemoryTextField;
    private JSlider maxMemorySlider;
    private JSlider initialMemorySlider;
    private JLabel validationIconLabel;

    public MemorySettingsPanel(ResourceBundle resourceBundle, String initialMaxMemory, String initialInitialMemory) {
        this.resourceBundle = resourceBundle;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Max Memory (Xmx)
        JLabel maxMemoryTitle = new JLabel(resourceBundle.getString("memory.max_title"));
        maxMemoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Determine max slider value based on detected memory, or a reasonable default/cap
        int maxSliderValue = 32768; // Default to 32GB if detection fails or for initial setup
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long totalPhysicalMemoryBytes = osBean.getTotalPhysicalMemorySize();
            long detectedMemoryMB = totalPhysicalMemoryBytes / (1024 * 1024);
            // Cap the slider max at a reasonable value, e.g., 64GB, or use detected memory if less
            maxSliderValue = (int) Math.min(detectedMemoryMB, 65536); // Cap at 64GB
            if (maxSliderValue < 4096) { // Ensure a minimum of 4GB for the slider max
                maxSliderValue = 4096;
            }
        } catch (Exception e) {
            CleanroomRelauncher.LOGGER.warn("Failed to detect total physical memory, using default max for slider: " + e.getMessage());
        }

        maxMemorySlider = new JSlider(JSlider.HORIZONTAL, 1024, maxSliderValue, 2048);
        maxMemorySlider.setToolTipText(resourceBundle.getString("memory.tooltip.max"));
        maxMemorySlider.setMajorTickSpacing(maxSliderValue / 4); // Adjust major tick spacing dynamically
        maxMemorySlider.setMinorTickSpacing(maxSliderValue / 16); // Adjust minor tick spacing dynamically
        maxMemorySlider.setPaintTicks(true);
        maxMemorySlider.setPaintLabels(true); // Enable default labels
        maxMemorySlider.setSnapToTicks(true);

        // Create a Hashtable to store the labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(1024, new JLabel("1GB"));
        labelTable.put(2048, new JLabel("2GB"));
        labelTable.put(4096, new JLabel("4GB"));
        labelTable.put(8192, new JLabel("8GB"));
        labelTable.put(16384, new JLabel("16GB"));
        labelTable.put(32768, new JLabel("32GB"));
        maxMemorySlider.setLabelTable(labelTable);

        maxMemoryTextField = new JTextField(String.valueOf(maxMemorySlider.getValue()));
        maxMemoryTextField.setColumns(5); // Adjust column width as needed
        maxMemoryTextField.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel maxMemoryInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxMemoryInputPanel.add(maxMemorySlider);
        maxMemoryInputPanel.add(maxMemoryTextField);
        maxMemoryInputPanel.add(new JLabel("MB"));

        if (initialMaxMemory != null && !initialMaxMemory.isEmpty()) {
            try {
                int initialValue = Integer.parseInt(initialMaxMemory);
                if (initialValue >= 1024 && initialValue <= 32768) {
                    maxMemorySlider.setValue(initialValue);
                }
            } catch (NumberFormatException ignored) {
                // Use default if parsing fails
            }
        } else {
            // Set initialMaxMemory to default slider value if not provided
            initialMaxMemory = String.valueOf(maxMemorySlider.getValue());
        }

        // Initial Memory (Xms)
        JLabel initialMemoryTitle = new JLabel(resourceBundle.getString("memory.initial_title"));
        initialMemoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        initialMemorySlider = new JSlider(JSlider.HORIZONTAL, 256, maxSliderValue, 512); // Use maxSliderValue for consistency
        initialMemorySlider.setToolTipText(resourceBundle.getString("memory.tooltip.initial"));
        initialMemorySlider.setMajorTickSpacing(maxSliderValue / 4); // Adjust major tick spacing dynamically
        initialMemorySlider.setMinorTickSpacing(maxSliderValue / 16); // Adjust minor tick spacing dynamically
        initialMemorySlider.setPaintTicks(true);
        initialMemorySlider.setPaintLabels(true); // Enable default labels
        initialMemorySlider.setSnapToTicks(true);

        // Create a Hashtable to store the labels
        Hashtable<Integer, JLabel> initialLabelTable = new Hashtable<>();
        initialLabelTable.put(256, new JLabel("256MB"));
        initialLabelTable.put(512, new JLabel("512MB"));
        initialLabelTable.put(1024, new JLabel("1GB"));
        initialLabelTable.put(2048, new JLabel("2GB"));
        initialLabelTable.put(4096, new JLabel("4GB"));
        initialMemorySlider.setLabelTable(initialLabelTable);

        initialMemoryTextField = new JTextField(String.valueOf(initialMemorySlider.getValue()));
        initialMemoryTextField.setColumns(5); // Adjust column width as needed
        initialMemoryTextField.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel initialMemoryInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        initialMemoryInputPanel.add(initialMemorySlider);
        initialMemoryInputPanel.add(initialMemoryTextField);
        initialMemoryInputPanel.add(new JLabel("MB"));
        validationIconLabel = new JLabel("");
        validationIconLabel.setFont(validationIconLabel.getFont().deriveFont(Font.BOLD, 16f)); // Make icon larger
        initialMemoryInputPanel.add(validationIconLabel);

        if (initialInitialMemory != null && !initialInitialMemory.isEmpty()) {
            try {
                int initialValue = Integer.parseInt(initialInitialMemory);
                if (initialValue >= 256 && initialValue <= 32768) {
                    initialMemorySlider.setValue(initialValue);
                }
            } catch (NumberFormatException ignored) {
                // Use default if parsing fails
            }
        } else {
            // Set initialInitialMemory to default slider value if not provided
            initialInitialMemory = String.valueOf(initialMemorySlider.getValue());
        }

        // Add components to memoryPanel
        add(maxMemoryTitle);
        add(maxMemoryInputPanel);
        add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        add(initialMemoryTitle);
        add(initialMemoryInputPanel);

        JButton resetMemory = new JButton(resourceBundle.getString("button.reset_memory"));
        resetMemory.addActionListener(e -> {
            maxMemorySlider.setValue(2048); // Default max memory
            initialMemorySlider.setValue(512); // Default initial memory
        });
        add(resetMemory); // Add Reset Memory button

        // Helper method for memory validation
        Consumer<JTextField> memoryValidator = (textField) -> {
            validateMemorySettings();
        };

        // Add listeners to trigger validation
        maxMemorySlider.addChangeListener(e -> {
            int value = maxMemorySlider.getValue();
            maxMemoryTextField.setText(String.valueOf(value));
            memoryValidator.accept(maxMemoryTextField); // Validate on slider change
        });

        maxMemoryTextField.addActionListener(e -> {
            try {
                int value = Integer.parseInt(maxMemoryTextField.getText());
                if (value >= maxMemorySlider.getMinimum() && value <= maxMemorySlider.getMaximum()) {
                    maxMemorySlider.setValue(value);
                } else {
                    maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
                }
            } catch (NumberFormatException ex) {
                maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
            }
            memoryValidator.accept(maxMemoryTextField); // Validate on text field action
        });

        maxMemoryTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    int value = Integer.parseInt(maxMemoryTextField.getText());
                    if (value >= maxMemorySlider.getMinimum() && value <= maxMemorySlider.getMaximum()) {
                        maxMemorySlider.setValue(value);
                    } else {
                        maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
                    }
                } catch (NumberFormatException ex) {
                    maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
                }
                memoryValidator.accept(maxMemoryTextField); // Validate on focus lost
            }
        });

        initialMemorySlider.addChangeListener(e -> {
            int value = initialMemorySlider.getValue();
            initialMemoryTextField.setText(String.valueOf(value));
            memoryValidator.accept(initialMemoryTextField); // Validate on slider change
        });

        initialMemoryTextField.addActionListener(e -> {
            try {
                int value = Integer.parseInt(initialMemoryTextField.getText());
                if (value >= initialMemorySlider.getMinimum() && value <= initialMemorySlider.getMaximum()) {
                    initialMemorySlider.setValue(value);
                } else {
                    initialMemoryTextField.setText(String.valueOf(initialMemorySlider.getValue()));
                }
            } catch (NumberFormatException ex) {
                initialMemoryTextField.setText(String.valueOf(initialMemorySlider.getValue()));
            }
            memoryValidator.accept(initialMemoryTextField); // Validate on text field action
        });

        initialMemoryTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    int value = Integer.parseInt(initialMemoryTextField.getText());
                    if (value >= initialMemorySlider.getMinimum() && value <= initialMemorySlider.getMaximum()) {
                        initialMemorySlider.setValue(value);
                    } else {
                        initialMemoryTextField.setText(String.valueOf(initialMemorySlider.getValue()));
                    }
                } catch (NumberFormatException ex) {
                    initialMemoryTextField.setText(String.valueOf(initialMemorySlider.getValue()));
                }
                memoryValidator.accept(initialMemoryTextField); // Validate on focus lost
            }
        });

        // Initial validation call
        validateMemorySettings();
    }

    public String getMaxMemory() {
        return maxMemoryTextField.getText();
    }

    public String getInitialMemory() {
        return initialMemoryTextField.getText();
    }

    public boolean validateMemorySettings() {
        try {
            int max = Integer.parseInt(maxMemoryTextField.getText());
            int initial = Integer.parseInt(initialMemoryTextField.getText());

            if (initial > max) {
                initialMemoryTextField.setBackground(UIManager.getColor("TextField.background")); // Reset to default
                validationIconLabel.setText("❌"); // Cross mark
                validationIconLabel.setForeground(new Color(200, 0, 0)); // Dark red
                return false;
            } else {
                initialMemoryTextField.setBackground(UIManager.getColor("TextField.background")); // Reset to default
                validationIconLabel.setText("✅"); // Checkmark
                validationIconLabel.setForeground(new Color(0, 150, 0)); // Dark green
                return true;
            }
        } catch (NumberFormatException e) {
            initialMemoryTextField.setBackground(UIManager.getColor("TextField.background")); // Reset to default
            validationIconLabel.setText("❌"); // Cross mark
            validationIconLabel.setForeground(new Color(200, 0, 0)); // Dark red
            return false;
        }
    }
}
