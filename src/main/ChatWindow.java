package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.UUID;

public class ChatWindow {
    private JFrame frame, loginFrame;
    private JPanel chatPanel;
    private JTextField messageField;
    private PrintWriter out;
    private BufferedReader in;
    private String displayName, userId;
    private Socket socket;
    private boolean isCoordinator = false;

    // Buttons
    private JButton sendButton, privateChatButton, logoutButton;
    private JButton requestDetailsButton, sendDetailsButton, checkActiveButton;
    private JButton emojiButton;
    private Timer coordinatorTimer;

    public ChatWindow() {
        createLoginUI();
    }

    private void createLoginUI() {
        loginFrame = new JFrame("Login - Chat App");
        loginFrame.setSize(400, 250);
        loginFrame.setLayout(new GridLayout(4, 2, 5, 5));

        JTextField displayNameField = new JTextField();
        JTextField ipField = new JTextField("192.168.0.42");
        JTextField portField = new JTextField("49191");

        loginFrame.add(new JLabel("Display Name:"));
        loginFrame.add(displayNameField);
        loginFrame.add(new JLabel("Server IP:"));
        loginFrame.add(ipField);
        loginFrame.add(new JLabel("Port:"));
        loginFrame.add(portField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            displayName = displayNameField.getText().trim();
            String serverIP = ipField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            if (displayName.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "Enter a valid name!");
                return;
            }
            connectToServer(serverIP, port);
            loginFrame.dispose();
        });

        loginFrame.add(new JLabel());
        loginFrame.add(loginButton);

        loginButton.setBackground(new Color(0xB39DDB)); // Pastel purple
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("Arial", Font.BOLD, 12));
        loginButton.setBorder(new RoundBorder(12));

        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setVisible(true);
    }

    private void connectToServer(String serverIP, int port) {
        try {
            socket = new Socket(serverIP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            userId = UUID.randomUUID().toString().substring(0, 6);
            out.println(userId + " " + displayName);  // send user details

            createChatUI();
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void createChatUI() {
        frame = new JFrame("Chat - " + displayName);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        frame.getContentPane().setBackground(new Color(0xF0F0F0));

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setOpaque(true);
        chatPanel.setBackground(new Color(0xF0F0F0));
        JScrollPane scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(0xF0F0F0));
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(new Color(0xF0F0F0));
        messageField = new JTextField(25);

        sendButton = new JButton("Send");
        styleButton(sendButton, new Color(0x80DEEA));

        privateChatButton = new JButton("Private Message");
        styleButton(privateChatButton, new Color(0x80DEEA));

        logoutButton = new JButton("Logout");
        styleButton(logoutButton, new Color(0xFF8A80));

        requestDetailsButton = new JButton("Request User Details");
        styleButton(requestDetailsButton, new Color(0xB39DDB));

        sendDetailsButton = new JButton("Send User Details");
        styleButton(sendDetailsButton, new Color(0xB39DDB));
        sendDetailsButton.setVisible(false);

        checkActiveButton = new JButton("Check Active Members");
        styleButton(checkActiveButton, new Color(0xB39DDB));
        checkActiveButton.setVisible(false);

        emojiButton = new JButton("Emoji");
        styleButton(emojiButton, new Color(0xF48FB1));
        emojiButton.addActionListener(e -> showEmojiPopup());

        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        privateChatButton.addActionListener(e -> openPrivateMessageDialog());
        logoutButton.addActionListener(e -> logout());
        requestDetailsButton.addActionListener(e -> {
            if (out != null) {
                out.println("/REQUEST_DETAILS");
                addMessageToChatPanel("📩 Requesting user details from coordinator...", LocalDateTime.now());
            }
        });
        sendDetailsButton.addActionListener(e -> {
            if (isCoordinator && out != null) {
                out.println("/SEND_DETAILS");
                addMessageToChatPanel("📜 Sending all user details...", LocalDateTime.now());
            }
        });
        checkActiveButton.addActionListener(e -> checkActiveMembers());

        bottomPanel.add(privateChatButton);
        bottomPanel.add(emojiButton);
        bottomPanel.add(requestDetailsButton);
        bottomPanel.add(messageField);
        bottomPanel.add(sendButton);
        bottomPanel.add(sendDetailsButton);
        bottomPanel.add(checkActiveButton);
        bottomPanel.add(logoutButton);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // Helper method to style buttons consistently
    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBorder(new RoundBorder(10));
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(120, 30));
    }

    private void showEmojiPopup() {
        JPopupMenu emojiMenu = new JPopupMenu();
        // Added more emojis to the list
        String[] emojis = {
                "😀", "😂", "😍", "😢", "👍", "🙏",
                "😎", "😉", "🤔", "😇", "😁", "😜",
                "🤗", "😡", "😭", "🤩", "🥳", "😏"
        };
        for (String emoji : emojis) {
            JMenuItem item = new JMenuItem(emoji);
            item.addActionListener(e -> {
                messageField.setText(messageField.getText() + emoji);
            });
            emojiMenu.add(item);
        }
        emojiMenu.show(messageField, messageField.getWidth() / 2, messageField.getHeight() / 2);
    }

    private void addMessageToChatPanel(String message, LocalDateTime timestamp) {
        MessageBubble bubble = new MessageBubble(message, timestamp);
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension pref = bubble.getPreferredSize();
        int maxWidth = 400;
        if (pref.width > maxWidth) {
            pref.width = maxWidth;
        }
        bubble.setMaximumSize(new Dimension(pref.width, Integer.MAX_VALUE));
        chatPanel.add(bubble);
        chatPanel.add(Box.createVerticalStrut(5));
        chatPanel.revalidate();
        chatPanel.repaint();
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }

    private void openPrivateMessageDialog() {
        JTextField recipientIdField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField portField = new JTextField();
        JTextField messageField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("User ID:"));
        panel.add(recipientIdField);
        panel.add(new JLabel("IP Address:"));
        panel.add(ipField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Message:"));
        panel.add(messageField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Send Private Message", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String recipientId = recipientIdField.getText().trim();
            String recipientIP = ipField.getText().trim();
            String portText = portField.getText().trim();
            String msg = messageField.getText().trim();
            if (!recipientId.isEmpty() && !recipientIP.isEmpty() && !portText.isEmpty() && !msg.isEmpty()) {
                try {
                    int recipientPort = Integer.parseInt(portText.trim());
                    sendPrivateMessage(recipientId, recipientIP, recipientPort, msg);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(frame, "Invalid port number!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendPrivateMessage(String recipientId, String recipientIP, int recipientPort, String msg) {
        String formattedMessage = "@" + recipientId + "-" + recipientIP + "-" + recipientPort + " " + msg;
        System.out.println("[DEBUG] Sending private message: " + formattedMessage);
        out.println(formattedMessage);
        addMessageToChatPanel("🔒 [To " + recipientId + " (" + recipientIP + ":" + recipientPort + ")]: " + msg, LocalDateTime.now());
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String receivedMessage = message;
                SwingUtilities.invokeLater(() -> {
                    addMessageToChatPanel(receivedMessage, LocalDateTime.now());

                    if (receivedMessage.equals("/BECOME_COORDINATOR") ||
                            receivedMessage.contains("✅ You are the **Coordinator**")) {
                        becomeCoordinator();
                    }

                    if (receivedMessage.startsWith("📢 New coordinator:")) {
                        if (!receivedMessage.contains(displayName)) {
                            isCoordinator = false;
                            sendDetailsButton.setVisible(false);
                            checkActiveButton.setVisible(false);
                            requestDetailsButton.setVisible(true);
                            addMessageToChatPanel("📝 " + receivedMessage, LocalDateTime.now());
                        }
                    }

                    logoutButton.setVisible(true);

                    if (receivedMessage.startsWith("@PRIVATE")) {
                        String[] parts = receivedMessage.split(" ", 4);
                        if (parts.length >= 4) {
                            String senderId = parts[1];
                            String privateMessage = parts[3];
                            addMessageToChatPanel("🔒 [From " + senderId + "]: " + privateMessage, LocalDateTime.now());
                        }
                    }
                });
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> addMessageToChatPanel("⚠️ Disconnected from server.", LocalDateTime.now()));
        }
    }

    public void becomeCoordinator() {
        isCoordinator = true;
        sendDetailsButton.setVisible(true);
        checkActiveButton.setVisible(true);
        requestDetailsButton.setVisible(false);
        addMessageToChatPanel("👑 You are now the Coordinator!", LocalDateTime.now());
        startCoordinatorCheck();
        frame.revalidate();
        frame.repaint();
    }

    private void checkActiveMembers() {
        if (out != null) {
            out.println("/CHECK_ACTIVE");
            addMessageToChatPanel("👀 Checking active members...", LocalDateTime.now());
            resetCoordinatorTimer();
        }
    }

    private void resetCoordinatorTimer() {
        if (coordinatorTimer != null) {
            coordinatorTimer.restart();
        }
    }

    private void startCoordinatorCheck() {
        if (coordinatorTimer != null) {
            coordinatorTimer.stop();
        }
        coordinatorTimer = new Timer(60000, (ActionEvent e) -> {
            if (isCoordinator) {
                addMessageToChatPanel("⚠️ Reminder: Click 'Check Active Members' within 60 seconds!", LocalDateTime.now());
                Timer timeoutTimer = new Timer(60000, (ActionEvent evt) -> {
                    if (isCoordinator) {
                        addMessageToChatPanel("⏳ Timeout: You didn't check active members. Logging out...", LocalDateTime.now());
                        logout();
                    }
                });
                timeoutTimer.setRepeats(false);
                timeoutTimer.start();
            }
        });
        coordinatorTimer.setRepeats(false);
        coordinatorTimer.setInitialDelay(0);
        coordinatorTimer.start();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                out.println("/LOGOUT");
                socket.close();
                if (coordinatorTimer != null) {
                    coordinatorTimer.stop();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            frame.dispose();
        }
    }

    public boolean isSendDetailsButtonVisible() {
        return sendDetailsButton.isVisible();
    }

    public boolean isCheckActiveButtonVisible() {
        return checkActiveButton.isVisible();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatWindow::new);
    }
}
