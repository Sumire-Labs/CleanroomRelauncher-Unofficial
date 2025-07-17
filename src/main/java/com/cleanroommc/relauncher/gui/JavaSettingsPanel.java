package com.cleanroommc.relauncher.gui;

import com.cleanroommc.javautils.JavaUtils;
import com.cleanroommc.javautils.api.JavaInstall;
import com.cleanroommc.javautils.spi.JavaLocator;
import com.cleanroommc.platformutils.Platform;
import com.cleanroommc.relauncher.CleanroomRelauncher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class JavaSettingsPanel extends JPanel {

    private final ResourceBundle resourceBundle;
    private JTextField javaPathTextField;
    private DefaultComboBoxModel<JavaInstall> versionModel;
    private JComboBox<JavaInstall> versionBox;
    private JPanel versionDropdownPanel;
    private Consumer<String> javaPathConsumer;
    private BiConsumer<String, RelauncherGUI.MessageType> messageConsumer;

    public JavaSettingsPanel(ResourceBundle resourceBundle, String initialJavaPath, Consumer<String> javaPathConsumer, BiConsumer<String, RelauncherGUI.MessageType> messageConsumer) {
        this.resourceBundle = resourceBundle;
        this.javaPathConsumer = javaPathConsumer;
        this.messageConsumer = messageConsumer;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical stacking
        setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));

        // 1. Java Path Input Panel
        JPanel pathInputPanel = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel(resourceBundle.getString("java.select_path"));
        javaPathTextField = new JTextField(100);
        javaPathTextField.setToolTipText(resourceBundle.getString("java.tooltip.path"));
        javaPathTextField.setText(initialJavaPath);
        JButton browse = new JButton(resourceBundle.getString("button.browse"));

        pathInputPanel.add(title, BorderLayout.NORTH);
        JPanel textAndBrowsePanel = new JPanel(new BorderLayout(5, 0));
        textAndBrowsePanel.add(javaPathTextField, BorderLayout.CENTER);
        textAndBrowsePanel.add(browse, BorderLayout.EAST);
        pathInputPanel.add(textAndBrowsePanel, BorderLayout.CENTER);
        add(pathInputPanel);
        add(Box.createRigidArea(new Dimension(0, 10))); // Add some vertical space

        // 2. Java Version Dropdown Panel
        versionDropdownPanel = new JPanel(new BorderLayout(5, 0));
        versionDropdownPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        versionBox = new JComboBox<>();
        versionModel = new DefaultComboBoxModel<>();
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
                javaPathTextField.setText(javaInstall.executable(true).getAbsolutePath());
            }
        });
        versionDropdownPanel.add(versionBox, BorderLayout.CENTER);
        add(versionDropdownPanel); // Add to main vertical panel
        add(Box.createRigidArea(new Dimension(0, 10))); // Add some vertical space

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
        add(optionsPanel);

        // Add listeners (keep existing logic)
        listenToTextFieldUpdate(javaPathTextField, t -> javaPathConsumer.accept(t.getText()));

        browse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Find Java Executable");
            if (!javaPathTextField.getText().isEmpty()) {
                File currentFile = new File(javaPathTextField.getText());
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
                javaPathTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        test.addActionListener(e -> {
            String javaPath = javaPathTextField.getText();
            if (javaPath.isEmpty()) {
                messageConsumer.accept(resourceBundle.getString("message.no_java_selected"), RelauncherGUI.MessageType.WARNING);
                return;
            }
            File javaFile = new File(javaPath);
            if (!javaFile.exists()) {
                messageConsumer.accept(resourceBundle.getString("message.invalid_java_path"), RelauncherGUI.MessageType.ERROR);
                return;
            }
            testJava(javaPath);
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
                    messageConsumer.accept(javaInstalls.size() + resourceBundle.getString("message.java_found") + (javaInstalls.isEmpty() ? "" : ":\n" + javaInstalls.stream().map(install -> install.vendor() + " " + install.version() + " (" + install.executable(true).getAbsolutePath() + ")").collect(Collectors.joining("\n"))), RelauncherGUI.MessageType.INFO);
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
                        javaPathConsumer.accept(newestJava.executable(true).getAbsolutePath());
                        javaPathTextField.setText(newestJava.executable(true).getAbsolutePath());
                        versionDropdownPanel.setVisible(true); // Use versionDropdownPanel
                    } else {
                        CleanroomRelauncher.LOGGER.info("No Java 21+ installs detected.");
                        versionDropdownPanel.setVisible(false); // Use versionDropdownPanel
                    }
                }

            }.execute();

        });

        reset.addActionListener(e -> {
            javaPathTextField.setText(""); // Clear the text field
            javaPathConsumer.accept(null); // Reset the javaPath variable
            versionDropdownPanel.setVisible(false); // Hide the version dropdown
            versionModel.removeAllElements(); // Clear dropdown elements
        });

        // Initial validation call
        validateJavaPath(javaPathTextField);
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

    public String getJavaPath() {
        return javaPathTextField.getText();
    }

    public boolean testJava(String javaPath) {
        try {
            JavaInstall javaInstall = JavaUtils.parseInstall(javaPath);
            if (javaInstall.version().major() < 21) {
                CleanroomRelauncher.LOGGER.fatal("Java 21+ needed, user specified Java {} instead", javaInstall.version());
                messageConsumer.accept(MessageFormat.format(resourceBundle.getString("message.java_old_version"), javaInstall.version().major()), RelauncherGUI.MessageType.ERROR);
                return false;
            }
            CleanroomRelauncher.LOGGER.info("Java {} specified from {}", javaInstall.version().major(), javaPath);
            messageConsumer.accept(MessageFormat.format(resourceBundle.getString("message.java_test_successful_detail"), javaInstall.vendor(), javaInstall.version(), javaPath), RelauncherGUI.MessageType.INFO);
            return true;
        } catch (IOException e) {
            CleanroomRelauncher.LOGGER.fatal("Failed to execute Java for testing", e);
            messageConsumer.accept("Failed to test Java (more information in console): " + e.getMessage(), RelauncherGUI.MessageType.ERROR);
            return false;
        }
    }
}
