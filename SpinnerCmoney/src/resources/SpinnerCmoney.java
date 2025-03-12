import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SpinnerCmoney extends JFrame {
    private final ArrayList<String> options = new ArrayList<>();
    private final HashMap<String, Color> colorMap = new HashMap<>();
    private final JPanel wheelPanel;
    private int rotationAngle = 0;
    private Timer spinTimer;
    private boolean isSpinning = false;

    private void playBackgroundMusic() {
        try {
            File audioFile = new File("impending.wav"); 
            if (!audioFile.exists()) {
                System.out.println("oops, forgot the music");
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
    
            
            FloatControl volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue(-40.0f); 
    
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SpinnerCmoney() {
        setTitle("THE GRAND CHOOSER");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.DARK_GRAY);

        playBackgroundMusic();

        wheelPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.GRAY);
                drawWheel(g);
                drawPointer(g);
            }
        };
        add(wheelPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);
        JTextField optionField = new JTextField(10);
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton spinButton = new JButton("Spin");
        
        addButton.setBackground(new Color(144, 238, 144));
        removeButton.setBackground(new Color(255, 182, 193));
        spinButton.setBackground(new Color(173, 216, 230));

        addButton.addActionListener(e -> modifyOptions(optionField.getText(), true));
        removeButton.addActionListener(e -> modifyOptions(optionField.getText(), false));
        spinButton.addActionListener(e -> spinWheel());

        JLabel optionLabel = new JLabel("Option: ");
        optionLabel.setForeground(Color.WHITE);
        controlPanel.add(optionLabel);
        controlPanel.add(optionField);
        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        controlPanel.add(spinButton);
        add(controlPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void modifyOptions(String option, boolean add) {
        if (!option.trim().isEmpty()) {
            if (add) {
                options.add(option.trim());
                colorMap.put(option.trim(), new Color((int) (Math.random() * 0x1000000)));
            } else {
                options.remove(option.trim());
                colorMap.remove(option.trim());
            }
            wheelPanel.repaint();
        }
    }

    private void drawWheel(Graphics g) {
        if (options.isEmpty()) return;

        int diameter = Math.min(wheelPanel.getWidth(), wheelPanel.getHeight()) - 20;
        int x = (wheelPanel.getWidth() - diameter) / 2;
        int y = (wheelPanel.getHeight() - diameter) / 2;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arcAngle = 360 / options.size();
        int centerX = x + diameter / 2;
        int centerY = y + diameter / 2;

        for (int i = 0; i < options.size(); i++) {
            int startAngle = rotationAngle + i * arcAngle;
            g2d.setColor(colorMap.get(options.get(i)));
            g2d.fillArc(x, y, diameter, diameter, startAngle, arcAngle);
            g2d.setColor(Color.BLACK);
            drawRotatedText(g2d, options.get(i), centerX, centerY, startAngle + arcAngle / 2, diameter / 3);
        }
    }

    private void drawPointer(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.RED);
        int[] xPoints = {wheelPanel.getWidth() / 2 - 5, wheelPanel.getWidth() / 2 + 5, wheelPanel.getWidth() / 2};
        int[] yPoints = {10, 10, 30};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private void drawRotatedText(Graphics2D g2d, String text, int centerX, int centerY, int angle, int radius) {
        double radianAngle = Math.toRadians(-angle);
        int textX = centerX + (int) (Math.cos(radianAngle) * radius);
        int textY = centerY + (int) (Math.sin(radianAngle) * radius);

        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(textX, textY);
        g2d.rotate(radianAngle + Math.PI / 2);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, -fm.stringWidth(text) / 2, fm.getHeight() / 3);
        g2d.setTransform(originalTransform);
    }

    private void spinWheel() {
        if (options.isEmpty() || isSpinning)
            return;
        
        isSpinning = true;
        Random rand = new Random();
        int targetAngle = rand.nextInt(720) + 1800; 
        int decelerationPoint = targetAngle - 720; 
    
        spinTimer = new Timer(20, new ActionListener() {
            int currentAngle = 0;
            int currentSpeed = 30;
    
            public void actionPerformed(ActionEvent e) {
                if (currentAngle < decelerationPoint) {
                    currentSpeed = Math.max(10, currentSpeed - 1); 
                } else {
                    currentSpeed = Math.max(3, currentSpeed - 2); 
                }
    
                rotationAngle = (rotationAngle + currentSpeed) % 360;
                currentAngle += currentSpeed;
                wheelPanel.repaint();
    
                if (currentAngle >= targetAngle) {
                    spinTimer.stop();
                    isSpinning = false;
                }
            }
        });
    
        spinTimer.start();
    }

    private void showResult() {
        if (options.isEmpty()) return; 
    
        int normalizedAngle = (360 - (rotationAngle % 360)) % 360;
        int arcAngle = 360 / options.size();
        int winningIndex = normalizedAngle / arcAngle;
    
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpinnerCmoney::new);
    }
}
