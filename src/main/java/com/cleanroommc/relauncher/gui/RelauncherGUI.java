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
import java.text.MessageFormat;

public class RelauncherGUI extends JDialog {

    private static ResourceBundle resourceBundle;

    static {
        resourceBundle = ResourceBundle.getBundle("messages");
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

    public static RelauncherGUI show(List<CleanroomRelease> eligibleReleases, String initialJavaPath, String initialMaxMemory, String initialInitialMemory) {
        ImageIcon imageIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(RelauncherGUI.class.getResource("/cleanroom-relauncher.png")));
        return new RelauncherGUI(new SupportingFrame(resourceBundle.getString("window.title"), imageIcon), eligibleReleases, initialJavaPath, initialMaxMemory, initialInitialMemory);
    }

    public CleanroomRelease selected;
    public FugueRelease selectedFugue;
    public String javaArgs;
    private MemorySettingsPanel memorySettingsPanel;
    private JavaSettingsPanel javaSettingsPanel;

    private JFrame frame;
    private int initialWidth;
    private int initialHeight;
    private JLabel statusLabel; // New status label

    public JavaSettingsPanel getJavaSettingsPanel() {
        return javaSettingsPanel;
    }

    public MemorySettingsPanel getMemorySettingsPanel() {
        return memorySettingsPanel;
    }

    private RelauncherGUI(SupportingFrame frame, List<CleanroomRelease> eligibleReleases, String initialJavaPath, String initialMaxMemory, String initialInitialMemory) {
        super(frame, frame.getTitle(), true);
        this.frame = frame;

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
        this.javaSettingsPanel = new JavaSettingsPanel(resourceBundle, initialJavaPath, path -> {}, (msg, type) -> showMessage(msg, type));
        tabbedPane.addTab(resourceBundle.getString("tab.java_settings"), this.javaSettingsPanel);

        // Memory and Arguments Tab
        JPanel memoryArgsPanel = new JPanel();
        memoryArgsPanel.setLayout(new BoxLayout(memoryArgsPanel, BoxLayout.Y_AXIS));
        this.memorySettingsPanel = new MemorySettingsPanel(resourceBundle, initialMaxMemory, initialInitialMemory);
        memoryArgsPanel.add(this.memorySettingsPanel);
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

    

    

    private JPanel initializeArgsPanel() {
        // Main Panel
        JPanel argsPanel = new JPanel(new BorderLayout(0, 0));
        argsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel(resourceBundle.getString("args.title"));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField text = new JTextField(100);
        text.setToolTipText(resourceBundle.getString("args.tooltip.extra"));
        text.setText(javaArgs);

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
            if (this.javaSettingsPanel.getJavaPath() == null || this.javaSettingsPanel.getJavaPath().isEmpty()) {
                showMessage(resourceBundle.getString("message.java_not_selected"), MessageType.ERROR);
                return;
            }

            // Validate memory settings before saving
            if (!this.memorySettingsPanel.validateMemorySettings()) {
                showMessage(resourceBundle.getString("message.initial_memory_exceeds_max"), MessageType.ERROR);
                return;
            }

            if (!this.javaSettingsPanel.testJava(this.javaSettingsPanel.getJavaPath())) {
                return;
            }
            CleanroomRelauncher.CONFIG.setCleanroomVersion(selected != null ? selected.name : null);
            CleanroomRelauncher.CONFIG.setJavaExecutablePath(this.javaSettingsPanel.getJavaPath());
            CleanroomRelauncher.CONFIG.setJavaArguments(javaArgs);
            CleanroomRelauncher.CONFIG.setMaxMemory(this.memorySettingsPanel.getMaxMemory());
            CleanroomRelauncher.CONFIG.setInitialMemory(this.memorySettingsPanel.getInitialMemory());
            CleanroomRelauncher.CONFIG.save();
            showMessage(resourceBundle.getString("message.settings_saved"), MessageType.INFO);
            frame.dispose();
        });
        relaunchButtonPanel.add(relaunchButton);

        return relaunchButtonPanel;
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

    public enum MessageType {
        INFO, WARNING, ERROR
    }

}