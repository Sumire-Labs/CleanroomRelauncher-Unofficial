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
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class RelauncherGUI extends JDialog {

    private final ResourceBundle resourceBundle;

    static {
        try {
            if (!java.awt.GraphicsEnvironment.isHeadless()) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
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
                component instanceof JComboBox) {
            Dimension size = component.getPreferredSize();
            component.setPreferredSize(new Dimension((int) (size.width * scale), (int) (size.height * scale)));
            component.setMaximumSize(new Dimension((int) (size.width * scale), (int) (size.height * scale)));
        } else if (component instanceof JButton) {
            // Buttons are handled by font scaling and padding, no need for explicit size scaling here
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
        return new RelauncherGUI(new SupportingFrame(resourceBundle.getString("window.title"), imageIcon), eligibleReleases, consumer);
    }

    public CleanroomRelease selected;
    public FugueRelease selectedFugue;
    public String javaPath, javaArgs, maxMemory, initialMemory;

    private JFrame frame;
    private int initialWidth;
    private int initialHeight;
    private JLabel statusLabel; // New status label

    private RelauncherGUI(SupportingFrame frame, List<CleanroomRelease> eligibleReleases, Consumer<RelauncherGUI> consumer) {
        super(frame, frame.getTitle(), true);
        this.resourceBundle = ResourceBundle.getBundle("messages");
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

        // Main content panel for tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // Version Selection Tab
        JPanel versionSelectionPanel = new JPanel();
        versionSelectionPanel.setLayout(new BoxLayout(versionSelectionPanel, BoxLayout.Y_AXIS));
        versionSelectionPanel.add(this.initializeCleanroomPicker(eligibleReleases));
        versionSelectionPanel.add(this.initializeFuguePicker());
        tabbedPane.addTab(resourceBundle.getString("tab.version_selection"), versionSelectionPanel);

        // Java Settings Tab
        JPanel javaSettingsPanel = this.initializeJavaPicker(); // initializeJavaPicker will return a panel with its own layout
        tabbedPane.addTab(resourceBundle.getString("tab.java_settings"), javaSettingsPanel);

        // Memory and Arguments Tab
        JPanel memoryArgsPanel = new JPanel();
        memoryArgsPanel.setLayout(new BoxLayout(memoryArgsPanel, BoxLayout.Y_AXIS));
        memoryArgsPanel.add(this.initializeMemoryPanel());
        memoryArgsPanel.add(this.initializeArgsPanel());
        tabbedPane.addTab(resourceBundle.getString("tab.memory_arguments"), memoryArgsPanel);

        JLabel cleanroomLogo = new JLabel(new ImageIcon(frame.getIconImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        cleanroomLogo.setAlignmentX(Component.CENTER_ALIGNMENT); // Center the logo

        // Main dialog layout
        this.setLayout(new BorderLayout());
        this.add(cleanroomLogo, BorderLayout.NORTH);
        this.add(tabbedPane, BorderLayout.CENTER);
        this.statusLabel = new JLabel("", SwingConstants.CENTER);
        this.statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(this.initializeRelaunchPanel(), BorderLayout.NORTH);
        southPanel.add(this.statusLabel, BorderLayout.SOUTH);
        this.add(southPanel, BorderLayout.SOUTH);
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
        JLabel title = new JLabel(resourceBundle.getString("cleanroom.select_version"));
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
        JLabel title = new JLabel(resourceBundle.getString("fugue.select_version"));
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

        // Loading indicator
        JLabel loadingLabel = new JLabel(resourceBundle.getString("fugue.loading"));
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dropdown.add(loadingLabel, BorderLayout.SOUTH);

        // Fetch Fugue releases in a background thread
        new SwingWorker<List<FugueRelease>, Void>() {
            @Override
            protected List<FugueRelease> doInBackground() throws Exception {
                return GlobalDownloader.fetchFugueReleases();
            }

            @Override
            protected void done() {
                dropdown.remove(loadingLabel); // Remove loading indicator
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
                        title.setText(resourceBundle.getString("fugue.no_releases"));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    CleanroomRelauncher.LOGGER.error("Error fetching Fugue releases for UI: " + e.getMessage(), e);
                    fugueBox.setEnabled(false);
                    title.setText(resourceBundle.getString("fugue.error_fetching"));
                }
                dropdown.revalidate();
                dropdown.repaint();
            }
        }.execute();

        return fuguePicker;
    }

    private JPanel initializeJavaPicker() {
        JPanel javaPicker = new JPanel();
        javaPicker.setLayout(new BoxLayout(javaPicker, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical stacking
        javaPicker.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));

        // 1. Java Path Input Panel
        JPanel pathInputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel(resourceBundle.getString("java.select_path"));
        JTextField text = new JTextField(100);
        text.setToolTipText(resourceBundle.getString("java.tooltip.path"));
        text.setText(javaPath);
        JButton browse = new JButton(resourceBundle.getString("button.browse"));

        pathInputPanel.add(title, BorderLayout.NORTH);
        JPanel textAndBrowsePanel = new JPanel(new BorderLayout(5, 0));
        textAndBrowsePanel.add(text, BorderLayout.CENTER);
        textAndBrowsePanel.add(browse, BorderLayout.EAST);
        pathInputPanel.add(textAndBrowsePanel, BorderLayout.CENTER);
        javaPicker.add(pathInputPanel);
        javaPicker.add(Box.createRigidArea(new Dimension(0, 10))); // Add some vertical space

        // 2. Java Version Dropdown Panel
        JPanel versionDropdownPanel = new JPanel(new BorderLayout(5, 0));
        versionDropdownPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
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
        versionDropdownPanel.add(versionBox, BorderLayout.CENTER);
        javaPicker.add(versionDropdownPanel);
        javaPicker.add(Box.createRigidArea(new Dimension(0, 10))); // Add some vertical space

        // 3. Options Panel (Auto-Detect, Test, Reset)
        JPanel optionsPanel = new JPanel(); // Use FlowLayout by default for buttons
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.X_AXIS)); // Explicitly set BoxLayout for horizontal
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Adjust padding
        JButton autoDetect = new JButton(resourceBundle.getString("button.auto_detect"));
        JButton test = new JButton(resourceBundle.getString("button.test"));
        JButton reset = new JButton(resourceBundle.getString("button.reset"));
        optionsPanel.add(autoDetect);
        optionsPanel.add(Box.createRigidArea(new Dimension(5, 0))); // Spacer
        optionsPanel.add(test);
        optionsPanel.add(Box.createRigidArea(new Dimension(5, 0))); // Spacer
        optionsPanel.add(reset);
        javaPicker.add(optionsPanel);

        // Add listeners (keep existing logic)
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
                showMessage(resourceBundle.getString("message.no_java_selected"), MessageType.WARNING);
                return;
            }
            File javaFile = new File(javaPath);
            if (!javaFile.exists()) {
                showMessage(resourceBundle.getString("message.invalid_java_path"), MessageType.ERROR);
                return;
            }
            JDialog testing = new JDialog(this, resourceBundle.getString("message.java_test_title"), true);
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
                    showMessage(javaInstalls.size() + resourceBundle.getString("message.java_found") + (javaInstalls.isEmpty() ? "" : ":\n" + javaInstalls.stream().map(install -> install.vendor() + " " + install.version() + " (" + install.executable(true).getAbsolutePath() + ")").collect(Collectors.joining("\n"))), MessageType.INFO);
                    autoDetect.setEnabled(true);

                    if (!javaInstalls.isEmpty()) {
                        CleanroomRelauncher.LOGGER.info("Detected Java installs: {}", javaInstalls.stream().map(JavaInstall::version).collect(Collectors.toList()));
                        versionModel.removeAllElements();
                        for (JavaInstall install : javaInstalls) {
                            versionModel.addElement(install);
                        }
                        // Select the newest Java version
                        JavaInstall newestJava = javaInstalls.get(javaInstalls.size() - 1);
                        versionBox.setSelectedItem(newestJava);
                        javaPath = newestJava.executable(true).getAbsolutePath();
                        text.setText(javaPath);
                        versionDropdownPanel.setVisible(true); // Use versionDropdownPanel
                    } else {
                        CleanroomRelauncher.LOGGER.info("No Java 21+ installs detected.");
                        versionDropdownPanel.setVisible(false); // Use versionDropdownPanel
                    }
                }

            }.execute();

        });

        reset.addActionListener(e -> {
            text.setText(""); // Clear the text field
            javaPath = null; // Reset the javaPath variable
            versionDropdownPanel.setVisible(false); // Hide the version dropdown
            versionModel.removeAllElements(); // Clear dropdown elements
        });

        return javaPicker;
    }

    private JPanel initializeMemoryPanel() {
        JPanel memoryPanel = new JPanel();
        memoryPanel.setLayout(new BoxLayout(memoryPanel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical stacking
        memoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

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

        JSlider maxMemorySlider = new JSlider(JSlider.HORIZONTAL, 1024, maxSliderValue, 2048);
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
        JLabel initialMemoryTitle = new JLabel(resourceBundle.getString("memory.initial_title"));
        initialMemoryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider initialMemorySlider = new JSlider(JSlider.HORIZONTAL, 256, maxSliderValue, 512); // Use maxSliderValue for consistency
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

        JButton resetMemory = new JButton(resourceBundle.getString("button.reset_memory"));
        resetMemory.addActionListener(e -> {
            maxMemorySlider.setValue(2048); // Default max memory
            initialMemorySlider.setValue(512); // Default initial memory
        });
        memoryPanel.add(resetMemory); // Add Reset Memory button

        // Helper method for memory validation
        Consumer<JTextField> memoryValidator = (textField) -> {
            validateMemorySettings(maxMemoryTextField, initialMemoryTextField);
        };

        // Add listeners to trigger validation
        maxMemorySlider.addChangeListener(e -> {
            int value = maxMemorySlider.getValue();
            maxMemoryTextField.setText(String.valueOf(value));
            maxMemory = String.valueOf(value);
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
            initialMemory = String.valueOf(value);
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
        validateMemorySettings(maxMemoryTextField, initialMemoryTextField);

        return memoryPanel;
    }

    private void validateMemorySettings(JTextField maxMemField, JTextField initialMemField) {
        try {
            int max = Integer.parseInt(maxMemField.getText());
            int initial = Integer.parseInt(initialMemField.getText());

            if (initial > max) {
                initialMemField.setBackground(new Color(255, 200, 200)); // Light red for warning
            } else {
                initialMemField.setBackground(UIManager.getColor("TextField.background")); // Reset to default
            }
        } catch (NumberFormatException e) {
            // If parsing fails, keep default background or handle as invalid input
            initialMemField.setBackground(UIManager.getColor("TextField.background"));
        }
    }

    private JPanel initializeArgsPanel() {
        // Main Panel
        JPanel argsPanel = new JPanel(new BorderLayout(0, 0));
        argsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel(resourceBundle.getString("args.title"));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField text = new JTextField(100);
        text.setToolTipText(resourceBundle.getString("args.tooltip.extra"));
        text.setText(javaArgs);
        listenToTextFieldUpdate(text, t -> javaArgs = t.getText());

        argsPanel.add(title, BorderLayout.NORTH);
        argsPanel.add(text, BorderLayout.CENTER);

        return argsPanel;
    }

    private JPanel initializeRelaunchPanel() {
        JPanel relaunchButtonPanel = new JPanel();

        JButton relaunchButton = new JButton(resourceBundle.getString("button.relaunch"));
        relaunchButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        relaunchButton.addActionListener(e -> {
            if (selected == null) {
                showMessage(resourceBundle.getString("message.cleanroom_not_selected"), MessageType.ERROR);
                return;
            }
            if (javaPath == null) {
                showMessage(resourceBundle.getString("message.java_not_selected"), MessageType.ERROR);
                return;
            }

            // Validate memory settings before saving
            try {
                int max = Integer.parseInt(maxMemory);
                int initial = Integer.parseInt(initialMemory);
                if (initial > max) {
                    showMessage(resourceBundle.getString("message.initial_memory_exceeds_max"), MessageType.ERROR);
                    return;
                }
            } catch (NumberFormatException e) {
                showMessage(resourceBundle.getString("message.invalid_memory_format"), MessageType.ERROR);
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
            showMessage(resourceBundle.getString("message.settings_saved"), MessageType.INFO);
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
                validateJavaPath(text);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textConsumer.accept(text);
                validateJavaPath(text);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textConsumer.accept(text);
                validateJavaPath(text);
            }
        });
    }

    private void validateJavaPath(JTextField javaPathField) {
        String path = javaPathField.getText();
        if (path == null || path.trim().isEmpty()) {
            javaPathField.setBackground(UIManager.getColor("TextField.background")); // Reset to default
            return;
        }
        File javaFile = new File(path);
        if (javaFile.exists() && javaFile.isFile() && javaFile.canExecute()) {
            javaPathField.setBackground(new Color(200, 255, 200)); // Light green for valid
        } else {
            javaPathField.setBackground(new Color(255, 200, 200)); // Light red for invalid
        }
    }

    

    private Runnable testJavaAndReturn() {
        try {
            JavaInstall javaInstall = JavaUtils.parseInstall(javaPath);
            if (javaInstall.version().major() < 21) {
                CleanroomRelauncher.LOGGER.fatal("Java 21+ needed, user specified Java {} instead", javaInstall.version());
                return () -> showMessage(MessageFormat.format(resourceBundle.getString("message.java_old_version"), javaInstall.version().major()), MessageType.ERROR);
            }
            CleanroomRelauncher.LOGGER.info("Java {} specified from {}", javaInstall.version().major(), javaPath);
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.fatal("Failed to execute Java for testing", e);
            return () -> showMessage(MessageFormat.format(resourceBundle.getString("message.java_test_failed"), e.getMessage()), MessageType.ERROR);
        }
        return null;
    }

    private void testJava() {
        try {
            JavaInstall javaInstall = JavaUtils.parseInstall(javaPath);
            if (javaInstall.version().major() < 21) {
                CleanroomRelauncher.LOGGER.fatal("Java 21+ needed, user specified Java {} instead", javaInstall.version());
                showMessage(MessageFormat.format(resourceBundle.getString("message.java_old_version"), javaInstall.version().major()), MessageType.ERROR);
                return;
            }
            CleanroomRelauncher.LOGGER.info("Java {} specified from {}", javaInstall.version().major(), javaPath);
            showMessage(MessageFormat.format(resourceBundle.getString("message.java_test_successful_detail"), javaInstall.vendor(), javaInstall.version(), javaPath), MessageType.INFO);
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.fatal("Failed to execute Java for testing", e);
            showMessage("Failed to test Java (more information in console): " + e.getMessage(), MessageType.ERROR);
        }
    }

    private void showMessage(String message, MessageType type) {
        statusLabel.setText(message);
        switch (type) {
            case INFO:
                statusLabel.setForeground(Color.BLACK);
                break;
            case WARNING:
                statusLabel.setForeground(Color.ORANGE);
                break;
            case ERROR:
                statusLabel.setForeground(Color.RED);
                break;
        }
        // Clear message after a few seconds
        new Timer(5000, (e) -> {
            statusLabel.setText("");
            statusLabel.setForeground(Color.BLACK);
        }).start();
    }

    private enum MessageType {
        INFO, WARNING, ERROR
    }

}