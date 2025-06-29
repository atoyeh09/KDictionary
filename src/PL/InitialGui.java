package PL;

import javax.swing.*;
import BLL.FascadeBo;
import java.awt.*;

public class InitialGui {

    private JFrame iniFrame;
    private JTextField nameField;
    private JButton folderButton;
    private JButton continueButton;
    private String selectedFolder;
    private final String defaultFolder = "resources";

    public InitialGui() {
        iniFrame = new JFrame("Folder Selection");
        iniFrame.setSize(400, 200);
        iniFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        iniFrame.setLocationRelativeTo(null);
        iniFrame.setLayout(new FlowLayout());

        JLabel nameLabel = new JLabel("Enter Initial:");
        nameField = new JTextField(10);
        folderButton = new JButton("Select Folder");
        continueButton = new JButton("Continue");

        selectedFolder = null;

        iniFrame.setVisible(true);

        folderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int result = folderChooser.showOpenDialog(iniFrame);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFolder = folderChooser.getSelectedFile().toString();
                JOptionPane.showMessageDialog(
                        iniFrame,
                        "Selected Folder: " + selectedFolder,
                        "Folder Selection",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        continueButton.addActionListener(e -> {
            if (selectedFolder == null || selectedFolder.isEmpty()) {
                JOptionPane.showMessageDialog(
                        iniFrame,
                        "No folder selected. Using default folder: " + defaultFolder,
                        "Default Folder",
                        JOptionPane.WARNING_MESSAGE
                );
                selectedFolder = defaultFolder;
            }

            iniFrame.dispose();
            new DictionaryGui(new FascadeBo(selectedFolder), selectedFolder);
        });

        iniFrame.add(nameLabel);
        iniFrame.add(nameField);
        iniFrame.add(folderButton);
        iniFrame.add(continueButton);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InitialGui());
    }
}
