package main;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageBubble extends JPanel {
    private String message;
    private LocalDateTime timestamp;

    public MessageBubble(String message, LocalDateTime timestamp) {
        setLayout(new BorderLayout());
        setOpaque(false);

        // Padding around text
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        setAlignmentX(LEFT_ALIGNMENT);

        // Main message label
        JLabel messageLabel = new JLabel("<html>" + message + "</html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(new Color(66, 66, 66)); // dark gray text
        add(messageLabel, BorderLayout.CENTER);

        // Footer panel for time/date
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        JLabel timeLabel = new JLabel(timestamp.format(timeFormatter));
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        JLabel dateLabel = new JLabel(timestamp.format(dateFormatter));
        dateLabel.setFont(new Font("Arial", Font.ITALIC, 10));

        footer.add(timeLabel, BorderLayout.WEST);
        footer.add(dateLabel, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0xF3E5F5));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);


        g2.setColor(new Color(200, 200, 200));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension pref = super.getPreferredSize();
        int maxWidth = 400;
        pref.width = Math.min(pref.width, maxWidth);
        return pref;
    }
}


