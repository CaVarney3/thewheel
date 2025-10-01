import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import javax.sound.sampled.*;
import java.io.File;

public class SpinnerCmoney extends JFrame {
    private final ArrayList<String> options = new ArrayList<>();
    private final HashMap<String, Color> colorMap = new HashMap<>();
    private final JPanel wheelPanel;
    private int rotationAngle = 0;
    private Timer spinTimer;
    private boolean isSpinning = false;

    // overlay fields
    private String overlayText = null;
    private boolean showOverlay = false;
    private Timer overlayTimer;

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
        getContentPane().setBackground(Color.BLACK);

        playBackgroundMusic();

        wheelPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.BLACK);
                drawWheel(g);
                drawPointer(g);
                drawOverlay(g); // overlay drawn on top of wheel
            }
        };
        wheelPanel.setBackground(Color.BLACK);
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
            String key = option.trim();
            if (add) {
                options.add(key);
                colorMap.put(key, new Color((int) (Math.random() * 0x1000000)));
            } else {
                options.remove(key);
                colorMap.remove(key);
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
        int w = wheelPanel.getWidth();
        int[] xPoints = {w / 2 - 8, w / 2 + 8, w / 2};
        int[] yPoints = {12, 12, 34};
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
        g2d.setFont(new Font("Times New Roman", Font.BOLD, 18));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, -fm.stringWidth(text) / 2, fm.getHeight() / 3);
        g2d.setTransform(originalTransform);
    }

    /**
     * Easing function: easeOutCubic (fast start, slow end)
     * f(t) = 1 - (1 - t)^3
     */
    private double easeOutCubic(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    private void spinWheel() {
        if (options.isEmpty() || isSpinning)
            return;

        // clear any overlay when a new spin begins
        if (overlayTimer != null && overlayTimer.isRunning()) {
            overlayTimer.stop();
        }
        showOverlay = false;
        overlayText = null;
        wheelPanel.repaint();

        isSpinning = true;
        Random rand = new Random();

        // keep random total rotation similar to what you had (5-7 turns)
        final int totalRotationDegrees = rand.nextInt(720) + 1800; // 1800..2520

        // duration controls how long the spin takes (ms). tuned reasonable defaults:
        final int minDuration = 2200;      // base duration in ms
        final int extraDuration = rand.nextInt(1600); // 0..1599 ms extra randomness
        final int durationMs = minDuration + extraDuration;

        final long startTime = System.currentTimeMillis();
        final int startRotation = rotationAngle; // starting rotation snapshot

        // use a ~60Hz timer for smooth animation
        final int frameDelay = 16;

        spinTimer = new Timer(frameDelay, null);
        spinTimer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                double elapsed = now - startTime;
                double t = Math.min(1.0, elapsed / durationMs);

                double eased = easeOutCubic(t);
                int currentTotal = (int) Math.round(eased * totalRotationDegrees);
                rotationAngle = (startRotation + currentTotal) % 360;

                wheelPanel.repaint();

                if (t >= 1.0) {
                    spinTimer.stop();
                    isSpinning = false;
                    showResult();
                }
            }
        });

        spinTimer.start();
    }

    /**
     * Correct winner calculation:
     * - The wheel's slices are drawn using startAngle = rotationAngle + i*arcAngle
     * - The pointer is at the top (12 o'clock). In Java's arc coordinates top == 90 degrees.
     * - Convert the absolute pointer angle into the wheel-local angle by subtracting rotationAngle.
     * - The local angle / arcAngle gives the winning slice index.
     */
    private void showResult() {
        if (options.isEmpty()) return;

        int arcAngle = 360 / options.size();

        // Absolute angle (in Java arc coordinates) where the pointer points:
        final int pointerAbsAngle = 90; // 12 o'clock

        // Convert the absolute pointer angle to the wheel-local angle (0..359)
        int localAngle = (pointerAbsAngle - rotationAngle) % 360;
        if (localAngle < 0) localAngle += 360;

        int winningIndex = localAngle / arcAngle;

        // clamp as safety
        winningIndex = Math.max(0, Math.min(winningIndex, options.size() - 1));
        String winner = options.get(winningIndex);

        // prepare overlay
        overlayText = winner + "!";
        showOverlay = true;
        wheelPanel.repaint();

        // auto-hide overlay after 2.5 seconds
        overlayTimer = new Timer(2500, e -> {
            showOverlay = false;
            overlayText = null;
            wheelPanel.repaint();
            overlayTimer.stop();
        });
        overlayTimer.setRepeats(false);
        overlayTimer.start();
    }

    private void drawOverlay(Graphics g) {
        if (!showOverlay || overlayText == null) return;

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // compute wheel bounds so overlay centers over the wheel (not the whole panel)
        int diameter = Math.min(wheelPanel.getWidth(), wheelPanel.getHeight()) - 20;
        int x = (wheelPanel.getWidth() - diameter) / 2;
        int y = (wheelPanel.getHeight() - diameter) / 2;
        int centerX = x + diameter / 2;
        int centerY = y + diameter / 2;

        // Text only — no box, no panel dim. scale font to wheel size
        Font textFont = new Font("Times New Roman", Font.BOLD, Math.max(28, diameter / 7));
        g2d.setFont(textFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(overlayText);
        int textHeight = fm.getHeight();

        // position slightly above wheel center
        int tx = centerX - textWidth / 2;
        int ty = centerY - textHeight / 2 - diameter / 8 + fm.getAscent();

        // draw a subtle shadow for readability
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.drawString(overlayText, tx + 3, ty + 3);

        // draw main text
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2d.setColor(Color.WHITE);
        g2d.drawString(overlayText, tx, ty);

        g2d.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpinnerCmoney::new);
    }
}
