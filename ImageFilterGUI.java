import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;

public class ImageFilterGUI extends JFrame {

    private File selectedImageFile;
    private Color positiveColor = new Color(0xFF, 0xFF, 0xFF);
    private Color negativeColor = new Color(0x00, 0x00, 0x00);

    private final JLabel imagePathLabel;
    private final JPanel positiveColorPanel;
    private final JPanel negativeColorPanel;
    private final JButton runButton;

    public ImageFilterGUI() {
        setTitle("Image Filter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Select Image
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        top.add(new JLabel("Image:"), c);
        c.gridx = 1;
        JButton selectImageBtn = new JButton("Select image...");
        selectImageBtn.addActionListener(e -> chooseImage());
        top.add(selectImageBtn, c);
        c.gridx = 2;
        c.weightx = 1;
        imagePathLabel = new JLabel(" No file selected");
        imagePathLabel.setForeground(Color.GRAY);
        top.add(imagePathLabel, c);

        // Row 1: Positive color
        c.gridy = 1;
        c.gridx = 0;
        c.weightx = 0;
        top.add(new JLabel("Positive:"), c);
        c.gridx = 1;
        positiveColorPanel = new JPanel();
        positiveColorPanel.setPreferredSize(new Dimension(80, 24));
        positiveColorPanel.setBackground(positiveColor);
        positiveColorPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JButton positiveBtn = new JButton("Choose color");
        positiveBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Positive color", positiveColor);
            if (chosen != null) {
                positiveColor = chosen;
                positiveColorPanel.setBackground(positiveColor);
            }
        });
        top.add(positiveBtn, c);
        c.gridx = 2;
        top.add(positiveColorPanel, c);

        // Row 2: Negative color
        c.gridy = 2;
        c.gridx = 0;
        top.add(new JLabel("Negative:"), c);
        c.gridx = 1;
        negativeColorPanel = new JPanel();
        negativeColorPanel.setPreferredSize(new Dimension(80, 24));
        negativeColorPanel.setBackground(negativeColor);
        negativeColorPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        JButton negativeBtn = new JButton("Choose color");
        negativeBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Negative color", negativeColor);
            if (chosen != null) {
                negativeColor = chosen;
                negativeColorPanel.setBackground(negativeColor);
            }
        });
        top.add(negativeBtn, c);
        c.gridx = 2;
        top.add(negativeColorPanel, c);

        add(top, BorderLayout.CENTER);

        runButton = new JButton("Run filter");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD, 14f));
        runButton.setEnabled(false);
        runButton.addActionListener(e -> runFilter());
        JPanel south = new JPanel();
        south.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        south.add(runButton);
        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an image");
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif", "bmp"));
        if (selectedImageFile != null && selectedImageFile.getParentFile() != null) {
            chooser.setCurrentDirectory(selectedImageFile.getParentFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = chooser.getSelectedFile();
            imagePathLabel.setText(" " + selectedImageFile.getName());
            imagePathLabel.setForeground(Color.BLACK);
            runButton.setEnabled(true);
        }
    }

    private void runFilter() {
        if (selectedImageFile == null || !selectedImageFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please select an image first.", "No image", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int posRGB = (positiveColor.getRed() << 16) | (positiveColor.getGreen() << 8) | positiveColor.getBlue();
        int negRGB = (negativeColor.getRed() << 16) | (negativeColor.getGreen() << 8) | negativeColor.getBlue();

        runButton.setEnabled(false);
        runButton.setText("Processing...");

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return ImageFilter.processImage(selectedImageFile, posRGB, negRGB);
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                runButton.setText("Run filter");
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        showResultWithSaveOption(result);
                    } else {
                        JOptionPane.showMessageDialog(ImageFilterGUI.this,
                                "Processing failed. Check that the Icons2 folder exists and the image is valid.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    runButton.setEnabled(true);
                    runButton.setText("Run filter");
                    JOptionPane.showMessageDialog(ImageFilterGUI.this,
                            "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showResultWithSaveOption(BufferedImage img) {
        setVisible(false);
        JFrame resultFrame = new JFrame("Filtered result");
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.setLayout(new BorderLayout());

        int maxW = 800, maxH = 600;
        int w = img.getWidth(), h = img.getHeight();
        if (w > maxW || h > maxH) {
            double scale = Math.min((double) maxW / w, (double) maxH / h);
            w = (int) (w * scale);
            h = (int) (h * scale);
        }
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        resultFrame.add(new JLabel(new ImageIcon(scaled)), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        JButton saveBtn = new JButton("Save...");
        JButton dontSaveBtn = new JButton("Don't save");
        saveBtn.addActionListener(e -> saveResultToFile(img, resultFrame));
        dontSaveBtn.addActionListener(e -> resultFrame.dispose());
        buttonPanel.add(saveBtn);
        buttonPanel.add(dontSaveBtn);
        resultFrame.add(buttonPanel, BorderLayout.SOUTH);

        resultFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                setVisible(true);
            }
        });

        resultFrame.pack();
        resultFrame.setLocationRelativeTo(null);
        resultFrame.setVisible(true);
    }

    private void saveResultToFile(BufferedImage img, JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save filtered image");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        String baseName = selectedImageFile != null
                ? selectedImageFile.getName().replaceFirst("[.][^.]+$", "") + "Filtered"
                : "filtered";
        chooser.setSelectedFile(new File(baseName + ".png"));
        if (selectedImageFile != null && selectedImageFile.getParentFile() != null) {
            chooser.setCurrentDirectory(selectedImageFile.getParentFile());
        }
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) {
                path += ".png";
                file = new File(path);
            }
            try {
                ImageIO.write(img, "png", file);
                JOptionPane.showMessageDialog(parent, "Saved to:\n" + path, "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent, "Could not save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new ImageFilterGUI().setVisible(true);
        });
    }
}
