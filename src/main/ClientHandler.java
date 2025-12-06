package main;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String userId;
    private String displayName;
    private String clientIP;
    private int clientPort;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String userDetails = in.readLine();
            if (userDetails != null) {
                String[] parts = userDetails.split(" ", 2);
                userId = parts[0];
                displayName = (parts.length > 1) ? parts[1] : "Unknown";
                clientIP = socket.getInetAddress().getHostAddress();
                clientPort = socket.getPort();
                server.addClient(userId, displayName, clientIP, clientPort, this);
            }
            listenForMessages();
        } catch (IOException e) {
            System.out.println("[Error] Connection issue with " + displayName);
        } finally {
            server.removeClient(userId);
            closeConnection();
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("@")) { // Detect private messages
                    String[] parts = message.split(" ", 2);
                    if (parts.length >= 2) {
                        String recipientData = parts[0].substring(1); // Remove "@"
                        String privateMessage = parts[1];
                        String[] recipientParts = recipientData.split("-");
                        if (recipientParts.length == 3) {
                            String recipientId = recipientParts[0];
                            String recipientIP = recipientParts[1];
                            int recipientPort = Integer.parseInt(recipientParts[2]);
                            server.sendPrivateMessage(userId, recipientId, recipientIP, recipientPort, privateMessage);
                        } else {
                            sendMessage("⚠️ Incorrect private message format. Use **@UserID-IP-Port message**.");
                        }
                    }
                } else if (message.equals("/SEND_DETAILS")) {
                    server.sendUserDetails(userId);
                } else if (message.equals("/REQUEST_DETAILS")) {
                    server.handleRequestDetails(userId);
                } else if (message.equals("/CHECK_ACTIVE")) {
                    server.checkHeartbeat(); // Coordinator triggers heartbeat check
                } else {
                    server.broadcastMessage(displayName + ": " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("[Error] Lost connection with " + displayName);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public Socket getSocket() {
        return socket;
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("[Error] Closing connection for " + displayName);
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClientIP() {
        return clientIP;
    }

    public int getClientPort() {
        return clientPort;
    }
}

