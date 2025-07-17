package com.cleanroommc.relauncher.gui;

import com.cleanroommc.javautils.JavaUtils;
import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.spi.JavaLocator;
import com.cleanroommc.platformutils.Platform;
import com.cleanroommc.relauncher.CleanroomRelauncher;
import com.cleanroommc.relauncher.download.CleanroomRelease;
import com.cleanroommc.relauncher.download.FugueRelease;
import com.cleanroommc.relauncher.download.GlobalDownloader;
import com.cleanroommc.relauncher.CleanroomRelauncher;
import net.minecraftforge.fml.cleanroomrelauncher.ExitVMBypass;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class RelauncherGUI extends JDialog {

    static {
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            }
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }

    private static void scaleComponent(Component component, float scale) {
        scaleSize(component, scale);
        scaleFont(component, scale);
        scalePadding(component, scale);

        component.revalidate();
        component.repaint();

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                scaleComponent(child, scale);
            }
        }
    }

    private static void scalePadding(Component component, float scale) {
        if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            Insets margin = button.getMargin();
            if (margin != null) {
                button.setMargin(new Insets(
                        (int) (margin.top * scale),
                        (int) (margin.left * scale),
                        (int) (margin.bottom * scale),
                        (int) (margin.right * scale)
                ));
            }
        } else if (component instanceof JTextField) {
            JTextField textField = (JTextField) component;
            Insets margin = textField.getMargin();
            if (margin != null) {
                textField.setMargin(new Insets(
                        (int) (margin.top * scale),
                        (int) (margin.left * scale),
                        (int) (margin.bottom * scale),
                        (int) (margin.right * scale)
                ));
            }
        } else if (component instanceof JComboBox) {
            JComboBox<?> comboBox = (JComboBox<?>) component;
            Insets margin = comboBox.getInsets();
            if (margin != null) {
                comboBox.setBorder(BorderFactory.createEmptyBorder(
                        (int) (margin.top * scale),
                        (int) (margin.left * scale),
                        (int) (margin.bottom * scale),
                        (int) (margin.right * scale)
                ));
            }
        } else if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            Insets margin = label.getInsets();
            if (margin != null) {
                label.setBorder(BorderFactory.createEmptyBorder(
                        (int) (margin.top * scale),
                        (int) (margin.left * scale),
                        (int) (margin.bottom * scale),
                        (int) (margin.right * scale)
                ));
            }
        } else if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            Border existingBorder = panel.getBorder();

            Insets margin = existingBorder instanceof EmptyBorder ?
                    ((EmptyBorder) existingBorder).getBorderInsets()
                    : new Insets(0, 0, 0, 0);

            panel.setBorder(BorderFactory.createEmptyBorder(
                    (int) (margin.top * scale),
                    (int) (margin.left * scale),
                    (int) (margin.bottom * scale),
                    (int) (margin.right * scale)
            ));
        }
    }

    private static void scaleFont(Component component, float scale) {
        if (component instanceof JLabel ||
                component instanceof JButton ||
                component instanceof JTextField ||
                component instanceof JComboBox) {
            Font font = component.getFont();
            if (font != null) {
                component.setFont(font.deriveFont(font.getSize() * scale));
            }
        }
    }

    private static void scaleSize(Component component, float scale) {
        // scaling rect
        if (component instanceof JTextField ||
                component instanceof JButton ||
                component instanceof JComboBox) {
            Dimension size = component.getPreferredSize();
            component.setPreferredSize(new Dimension((int) (size.width * scale) + 10, (int) (size.height * scale)));
            component.setMaximumSize(new Dimension((int) (size.width * scale) + 10, (int) (size.height * scale)));
        } else if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            Icon icon = label.getIcon();
            if (icon instanceof ImageIcon) {
                ImageIcon imageIcon = (ImageIcon) icon;
                Image image = imageIcon.getImage();
                if (image != null) {
                    Image scaledImage = image.getScaledInstance(
                            (int) (imageIcon.getIconWidth() * scale),
                            (int) (imageIcon.getIconHeight() * scale),
                            Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaledImage));
                }
            }
        }
    }

    public static RelauncherGUI show(List<CleanroomRelease> eligibleReleases, Consumer<RelauncherGUI> consumer) {
        ImageIcon imageIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(RelauncherGUI.class.getResource("/cleanroom-relauncher.png")));
        return new RelauncherGUI(new SupportingFrame("Cleanroom Relaunch Configuration", imageIcon), eligibleReleases, consumer);
    }

    public CleanroomRelease selected;
    public FugueRelease selectedFugue;
    public String javaPath, javaArgs, maxMemory, initialMemory;

    private JFrame frame;
    private int initialWidth;
    private int initialHeight;

    private RelauncherGUI(SupportingFrame frame, List<CleanroomRelease> eligibleReleases, Consumer<RelauncherGUI> consumer) {
        super(frame, frame.getTitle(), true);
        this.frame = frame;

        consumer.accept(this);

        this.setIconImage(frame.getIconImage());

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                RelauncherGUI.this.requestFocusInWindow();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                selected = null;
                frame.dispose();

                CleanroomRelauncher.LOGGER.info("No Cleanroom releases were selected, instance is dismissed.");
                ExitVMBypass.exit(0);
            }
        });
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setAlwaysOnTop(true);

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screen = env.getDefaultScreenDevice();
        Rectangle rect = screen.getDefaultConfiguration().getBounds();
        int width = rect.width / 3;
        int height = (int) (width / 1.25f);
        int x = (rect.width - width) / 2;
        int y = (rect.height - height) / 2;
        this.setLocation(x, y);

        this.initialWidth = width;
        this.initialHeight = height;

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Theme selection
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel themeLabel = new JLabel("Theme:");
        JComboBox<String> themeComboBox = new JComboBox<>(new String[]{"Light", "Dark"});
        themeComboBox.setSelectedItem("Light"); // Default to Light
        themeComboBox.addActionListener(e -> {
            try {
                if ("Dark".equals(themeComboBox.getSelectedItem())) {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                }
                SwingUtilities.updateComponentTreeUI(this);
                SwingUtilities.updateComponentTreeUI(frame); // Update the parent frame too
                this.pack(); // Repack to adjust sizes if needed
                this.setSize(initialWidth, initialHeight); // Reset to initial size
            } catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
        });
        themePanel.add(themeLabel);
        themePanel.add(themeComboBox);
        mainPanel.add(themePanel);

        JLabel cleanroomLogo = new JLabel(new ImageIcon(frame.getIconImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));

        JPanel cleanroomPickerPanel = this.initializeCleanroomPicker(eligibleReleases);
        mainPanel.add(cleanroomPickerPanel);

        JPanel fuguePickerPanel = this.initializeFuguePicker();
        mainPanel.add(fuguePickerPanel);

        JPanel javaPickerPanel = this.initializeJavaPicker();
        mainPanel.add(javaPickerPanel);

        JPanel memoryPanel = this.initializeMemoryPanel();
        mainPanel.add(memoryPanel);

        JPanel argsPanel = this.initializeArgsPanel();
        mainPanel.add(argsPanel);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(cleanroomLogo, BorderLayout.NORTH);
        contentPanel.add(mainPanel, BorderLayout.SOUTH);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(contentPanel, gbc);

        JPanel relaunchButtonPanel = this.initializeRelaunchPanel();

        this.add(wrapper, BorderLayout.NORTH);
        this.add(relaunchButtonPanel, BorderLayout.SOUTH);
        float scale = rect.width / 1463f;
        scaleComponent(this, scale);

        this.pack();
        this.setSize(width, height);
        this.setVisible(true);
        this.setAutoRequestFocus(true);
    }

    private JPanel initializeCleanroomPicker(List<CleanroomRelease> eligibleReleases) {
        // Main Panel
        JPanel cleanroomPicker = new JPanel(new BorderLayout(5, 0));
        cleanroomPicker.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel select = new JPanel();
        select.setLayout(new BoxLayout(select, BoxLayout.Y_AXIS));
        cleanroomPicker.add(select);

        // Title label
        JLabel title = new JLabel("Select Cleanroom Version:");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(title);
        select.add(Box.createRigidArea(new Dimension(0, 5)));

        // Create dropdown panel
        JPanel dropdown = new JPanel(new BorderLayout(5, 5));
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(dropdown);

        // Create the dropdown with release versions
        JComboBox<CleanroomRelease> releaseBox = new JComboBox<>();
        DefaultComboBoxModel<CleanroomRelease> releaseModel = new DefaultComboBoxModel<>();
        for (CleanroomRelease release : eligibleReleases) {
            releaseModel.addElement(release);
        }
        releaseBox.setModel(releaseModel);
        releaseBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CleanroomRelease) {
                    setText(((CleanroomRelease) value).name);
                }
                return this;
            }
        });
        releaseBox.setSelectedItem(selected);
        releaseBox.setMaximumRowCount(5);
        releaseBox.addActionListener(e -> selected = (CleanroomRelease) releaseBox.getSelectedItem());
        dropdown.add(releaseBox, BorderLayout.CENTER);

        return cleanroomPicker;
    }

    private JPanel initializeFuguePicker() {
        // Main Panel
        JPanel fuguePicker = new JPanel(new BorderLayout(5, 0));
        fuguePicker.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel select = new JPanel();
        select.setLayout(new BoxLayout(select, BoxLayout.Y_AXIS));
        fuguePicker.add(select);

        // Title label
        JLabel title = new JLabel("Select Fugue Version (Optional):");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(title);
        select.add(Box.createRigidArea(new Dimension(0, 5)));

        // Create dropdown panel
        JPanel dropdown = new JPanel(new BorderLayout(5, 5));
        dropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        select.add(dropdown);

        // Create the dropdown with Fugue versions
        JComboBox<FugueRelease> fugueBox = new JComboBox<>();
        DefaultComboBoxModel<FugueRelease> fugueModel = new DefaultComboBoxModel<>();
        fugueBox.setModel(fugueModel);
        fugueBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FugueRelease) {
                    setText(((FugueRelease) value).name);
                }
                return this;
            }
        });
        fugueBox.setSelectedItem(null); // No default selection
        fugueBox.setMaximumRowCount(5);
        fugueBox.addActionListener(e -> selectedFugue = (FugueRelease) fugueBox.getSelectedItem());
        dropdown.add(fugueBox, BorderLayout.CENTER);

        // Fetch Fugue releases in a background thread
        new SwingWorker<List<FugueRelease>, Void>() {
            @Override
            protected List<FugueRelease> doInBackground() throws Exception {
                return GlobalDownloader.fetchFugueReleases();
            }

            @Override
            protected void done() {
                try {
                    List<FugueRelease> releases = get();
                    if (!releases.isEmpty()) {
                        fugueModel.removeAllElements();
                        fugueModel.addElement(null); // Option for no Fugue
                        for (FugueRelease release : releases) {
                            fugueModel.addElement(release);
                        }
                        fugueBox.setSelectedItem(null); // Select "no Fugue" by default
                    } else {
                        fugueBox.setEnabled(false); // Disable if no releases found
                        title.setText("Select Fugue Version (Optional - No releases found)");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    CleanroomRelauncher.LOGGER.error("Error fetching Fugue releases for UI: " + e.getMessage(), e);
                    fugueBox.setEnabled(false);
                    title.setText("Select Fugue Version (Optional - Error fetching releases)");
                }
            }
        }.execute();

        return fuguePicker;
    }

    private JPanel initializeJavaPicker() {
        // Main Panel
        JPanel javaPicker = new JPanel(new BorderLayout(5, 0));
        javaPicker.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));

        // Select Panel
        JPanel selectPanel = new JPanel(new BorderLayout(5, 5));
        selectPanel.setLayout(new BoxLayout(selectPanel, BoxLayout.Y_AXIS));
        JPanel subSelectPanel = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel("Select Java Executable Path (Java 21+ required):");
        JTextField text = new JTextField(100);
        text.setToolTipText("Path to the Java executable (java.exe or java)");
        text.setText(javaPath);
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout(5, 0));
        northPanel.add(title, BorderLayout.NORTH);
        subSelectPanel.add(northPanel, BorderLayout.CENTER);
        subSelectPanel.add(text, BorderLayout.CENTER);
        // JButton browse = new JButton(UIManager.getIcon("FileView.directoryIcon"));
        JButton browse = new JButton("Browse");
        subSelectPanel.add(browse, BorderLayout.EAST);
        selectPanel.add(subSelectPanel);
        javaPicker.add(selectPanel);

        // Java Version Dropdown
        JPanel versionDropdown = new JPanel(new BorderLayout(5, 0));
        versionDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<JavaInstall> versionBox = new JComboBox<>();
        DefaultComboBoxModel<JavaInstall> versionModel = new DefaultComboBoxModel<>();
        versionBox.setModel(versionModel);
        versionBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof JavaInstall) {
                    JavaInstall javaInstall = (JavaInstall) value;
                    setText(javaInstall.vendor() + " " + javaInstall.version());
                }
                return this;
            }
        });
        versionBox.setSelectedItem(null);
        versionBox.setMaximumRowCount(10);
        versionBox.addActionListener(e -> {
            if (versionBox.getSelectedItem() != null) {
                JavaInstall javaInstall = (JavaInstall) versionBox.getSelectedItem();
                javaPath = javaInstall.executable(true).getAbsolutePath();
                text.setText(javaPath);
            }
        });
        versionDropdown.add(versionBox, BorderLayout.CENTER);
        versionDropdown.setVisible(false);
        northPanel.add(versionDropdown, BorderLayout.CENTER);

        // Options Panel
        JPanel options = new JPanel(new BorderLayout(5, 0));
        options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
        options.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        selectPanel.add(options);
        // JButton download = new JButton("Download");
        JButton autoDetect = new JButton("Auto-Detect");
        JButton test = new JButton("Test");
        JButton reset = new JButton("Reset"); // New Reset button
        options.add(autoDetect);
        options.add(test);
        options.add(reset); // Add Reset button

        reset.addActionListener(e -> {
            text.setText(""); // Clear the text field
            javaPath = null; // Reset the javaPath variable
            versionDropdown.setVisible(false); // Hide the version dropdown
            versionModel.removeAllElements(); // Clear dropdown elements
        });

        listenToTextFieldUpdate(text, t -> javaPath = t.getText());

        browse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Find Java Executable");
            if (!text.getText().isEmpty()) {
                File currentFile = new File(text.getText());
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    fileChooser.setCurrentDirectory(currentFile.getParentFile());
                }
            }
            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    }
                    if (file.isFile()) {
                        return !Platform.current().isWindows() || file.getName().endsWith(".exe");
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return Platform.current().isWindows() ? "Java Executable (*.exe)" : "Java Executable";
                }
            };
            fileChooser.setFileFilter(filter);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                text.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        test.addActionListener(e -> {
            String javaPath = text.getText();
            if (javaPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a Java executable first.", "No Java Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File javaFile = new File(javaPath);
            if (!javaFile.exists()) {
                JOptionPane.showMessageDialog(this, "The selected Java executable does not exist.", "Invalid Java Executable Path", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JDialog testing = new JDialog(this, "Testing Java Executable", true);
            testing.setLocationRelativeTo(this);

            this.testJava();
        });

        autoDetect.addActionListener(e -> {
            String original = autoDetect.getText();
            autoDetect.setText("Detecting");
            autoDetect.setEnabled(false);

            AtomicInteger dotI = new AtomicInteger(0);
            String[] dots = { ".", "..", "..." };
            Timer timer = new Timer(400, te -> {
                autoDetect.setText("Detecting" + dots[dotI.get()]);
                dotI.set((dotI.get() + 1) % dots.length);
            });
            timer.start();

            new SwingWorker<List<JavaInstall>, Void>() {

                List<JavaInstall> javaInstalls = Collections.emptyList();

                @Override
                protected List<JavaInstall> doInBackground() {
                    this.javaInstalls = JavaLocator.locators().parallelStream()
                            .map(JavaLocator::all)
                            .flatMap(Collection::stream)
                            .filter(javaInstall -> javaInstall.version().major() >= 21)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());
                    return this.javaInstalls;
                }

                @Override
                protected void done() {
                    timer.stop();
                    autoDetect.setText(original);
                    JOptionPane.showMessageDialog(RelauncherGUI.this, javaInstalls.size() + " Java 21+ Installs Found!", "Auto-Detection Finished", JOptionPane.INFORMATION_MESSAGE);
                    autoDetect.setEnabled(true);

                    if (!javaInstalls.isEmpty()) {
                        versionModel.removeAllElements();
                        for (JavaInstall install : javaInstalls) {
                            versionModel.addElement(install);
                        }
                        versionDropdown.setVisible(true);
                    }
                }

            }.execute();

        });

        return javaPicker;
    }

    private JPanel initializeMemoryPanel() {
        JPanel memoryPanel = new JPanel();
        memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical stacking
        memoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Max Memory (Xmx)
        JLabel maxMemoryTitle = new JLabel("Allocate Max Memory (MB):");
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

        JSlider maxMemorySlider = new JSlider(JSlider.HORIZONTAL, 1024, maxSliderValue, 2048);
        maxMemorySlider.setToolTipText("Maximum memory allocation for the game (Xmx)");
        maxMemorySlider.setMajorTickSpacing(maxSliderValue / 4); // Adjust major tick spacing dynamically
        maxMemorySlider.setMinorTickSpacing(maxSliderValue / 16); // Adjust minor tick spacing dynamically
        maxMemorySlider.setPaintTicks(true);
        maxMemorySlider.setPaintLabels(false); // Disable default labels to avoid overlap
        maxMemorySlider.setSnapToTicks(true);

        JTextField maxMemoryTextField = new JTextField(String.valueOf(maxMemorySlider.getValue()));
        maxMemoryTextField.setColumns(5); // Adjust column width as needed
        maxMemoryTextField.setHorizontalAlignment(SwingConstants.RIGHT);

        maxMemorySlider.addChangeListener(e -> {
            int value = maxMemorySlider.getValue();
            maxMemoryTextField.setText(String.valueOf(value));
            maxMemory = String.valueOf(value);
        });

        maxMemoryTextField.addActionListener(e -> {
            try {
                int value = Integer.parseInt(maxMemoryTextField.getText());
                if (value >= maxMemorySlider.getMinimum() && value <= maxMemorySlider.getMaximum()) {
                    maxMemorySlider.setValue(value);
                } else {
                    // Optionally, show an error or revert to previous valid value
                    maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
                }
            } catch (NumberFormatException ex) {
                // Optionally, show an error or revert to previous valid value
                maxMemoryTextField.setText(String.valueOf(maxMemorySlider.getValue()));
            }
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
            }
        });

        JPanel maxMemoryInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maxMemoryInputPanel.add(maxMemorySlider);
        maxMemoryInputPanel.add(maxMemoryTextField);
        maxMemoryInputPanel.add(new JLabel("MB"));

        if (maxMemory != null && !maxMemory.isEmpty()) {
            try {
                int initialValue = Integer.parseInt(maxMemory);
                if (initialValue >= 1024 && initialValue <= 32768) {
                    maxMemorySlider.setValue(initialValue);
                }
            } catch (NumberFormatException ignored) {
                // Use default if parsing fails
            }
        } else {
            maxMemory = String.valueOf(maxMemorySlider.getValue());
        }

        // Initial Memory (Xms)
        JLabel initialMemoryTitle = new JLabel("Allocate Initial Memory (MB):");
        initialMemoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider initialMemorySlider = new JSlider(JSlider.HORIZONTAL, 256, maxSliderValue, 512); // Use maxSliderValue for consistency
        initialMemorySlider.setToolTipText("Initial memory allocation for the game (Xms)");
        initialMemorySlider.setMajorTickSpacing(maxSliderValue / 4); // Adjust major tick spacing dynamically
        initialMemorySlider.setMinorTickSpacing(maxSliderValue / 16); // Adjust minor tick spacing dynamically
        initialMemorySlider.setPaintTicks(true);
        initialMemorySlider.setPaintLabels(false); // Disable default labels to avoid overlap
        initialMemorySlider.setSnapToTicks(true);

        JTextField initialMemoryTextField = new JTextField(String.valueOf(initialMemorySlider.getValue()));
        initialMemoryTextField.setColumns(5); // Adjust column width as needed
        initialMemoryTextField.setHorizontalAlignment(SwingConstants.RIGHT);

        initialMemorySlider.addChangeListener(e -> {
            int value = initialMemorySlider.getValue();
            initialMemoryTextField.setText(String.valueOf(value));
            initialMemory = String.valueOf(value);
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
            }
        });

        JPanel initialMemoryInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        initialMemoryInputPanel.add(initialMemorySlider);
        initialMemoryInputPanel.add(initialMemoryTextField);
        initialMemoryInputPanel.add(new JLabel("MB"));

        if (initialMemory != null && !initialMemory.isEmpty()) {
            try {
                int initialValue = Integer.parseInt(initialMemory);
                if (initialValue >= 256 && initialValue <= 32768) {
                    initialMemorySlider.setValue(initialValue);
                }
            } catch (NumberFormatException ignored) {
                // Use default if parsing fails
            }
        } else {
            initialMemory = String.valueOf(initialMemorySlider.getValue());
        }


        // Add components to memoryPanel
        memoryPanel.add(maxMemoryTitle);
        memoryPanel.add(maxMemoryInputPanel);
        memoryPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        memoryPanel.add(initialMemoryTitle);
        memoryPanel.add(initialMemoryInputPanel);

        JButton resetMemory = new JButton("Reset Memory");
        resetMemory.addActionListener(e -> {
            maxMemorySlider.setValue(2048); // Default max memory
            initialMemorySlider.setValue(512); // Default initial memory
        });
        memoryPanel.add(resetMemory); // Add Reset Memory button

        return memoryPanel;
    }

    private JPanel initializeArgsPanel() {
        // Main Panel
        JPanel argsPanel = new JPanel(new BorderLayout(0, 0));
        argsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel("Add Java Arguments:");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField text = new JTextField(100);
        text.setToolTipText("Additional Java arguments to pass to the game JVM");
        text.setText(javaArgs);
        listenToTextFieldUpdate(text, t -> javaArgs = t.getText());

        argsPanel.add(title, BorderLayout.NORTH);
        argsPanel.add(text, BorderLayout.CENTER);

        return argsPanel;
    }

    private JPanel initializeRelaunchPanel() {
        JPanel relaunchButtonPanel = new JPanel();

        JButton relaunchButton = new JButton("Relaunch with Cleanroom");
        relaunchButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        relaunchButton.addActionListener(e -> {
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select a Cleanroom version in order to relaunch.", "Cleanroom Release Not Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (javaPath == null) {
                JOptionPane.showMessageDialog(this, "Please provide a valid Java Executable in order to relaunch.", "Java Executable Not Selected", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Runnable test = this.testJavaAndReturn();
            if (test != null) {
                test.run();
                return;
            }
            CleanroomRelauncher.CONFIG.setCleanroomVersion(selected != null ? selected.name : null);
            CleanroomRelauncher.CONFIG.setJavaExecutablePath(javaPath);
            CleanroomRelauncher.CONFIG.setJavaArguments(javaArgs);
            CleanroomRelauncher.CONFIG.setMaxMemory(maxMemory);
            CleanroomRelauncher.CONFIG.setInitialMemory(initialMemory);
            CleanroomRelauncher.CONFIG.save();
            frame.dispose();
        });
        relaunchButtonPanel.add(relaunchButton);

        return relaunchButtonPanel;
    }

    private void listenToTextFieldUpdate(JTextField text, Consumer<JTextField> textConsumer) {
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                textConsumer.accept(text);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textConsumer.accept(text);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textConsumer.accept(text);
            }
        });
    }

    

    private Runnable testJavaAndReturn() {
        try {
            JavaInstall javaInstall = JavaUtils.parseInstall(javaPath);
            if (javaInstall.version().major() < 21) {
                CleanroomRelauncher.LOGGER.fatal("Java 21+ needed, user specified Java {} instead", javaInstall.version());
                return () -> JOptionPane.showMessageDialog(this, "Java 21 is the minimum version for Cleanroom. Currently, Java " + javaInstall.version().major() + " is selected.", "Old Java Version", JOptionPane.ERROR_MESSAGE);
            }
            CleanroomRelauncher.LOGGER.info("Java {} specified from {}", javaInstall.version().major(), javaPath);
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.fatal("Failed to execute Java for testing", e);
            return () -> JOptionPane.showMessageDialog(this, "Failed to test Java (more information in console): " + e.getMessage(), "Java Test Failed", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void testJava() {
        try {
            JavaInstall javaInstall = JavaUtils.parseInstall(javaPath);
            if (javaInstall.version().major() < 21) {
                CleanroomRelauncher.LOGGER.fatal("Java 21+ needed, user specified Java {} instead", javaInstall.version());
                JOptionPane.showMessageDialog(this, "Java 21 is the minimum version for Cleanroom. Currently, Java " + javaInstall.version().major() + " is selected.", "Old Java Version", JOptionPane.ERROR_MESSAGE);
                return;
            }
            CleanroomRelauncher.LOGGER.info("Java {} specified from {}", javaInstall.version().major(), javaPath);
            JOptionPane.showMessageDialog(this, "Java executable is working correctly!", "Java Test Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.fatal("Failed to execute Java for testing", e);
            JOptionPane.showMessageDialog(this, "Failed to test Java (more information in console): " + e.getMessage(), "Java Test Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

}