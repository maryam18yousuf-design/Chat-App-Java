package main;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 49191;
    // Tracks connected users
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private String coordinator;

    public void start() {
        try {
            InetAddress wifiIP = InetAddress.getByName("192.168.0.42");
            ServerSocket serverSocket = new ServerSocket(PORT, 50, wifiIP);
            System.out.println("[main.Server] Listening on: " + wifiIP.getHostAddress() + ":" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // New instance of ClientHandler created
                System.out.println("[main.Server] New client connected from "
                        + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler); // allows each client to run on its own thread
            }
        } catch (IOException e) {
            System.out.println("[Error] Unable to start server: " + e.getMessage());
        }
    }

    public synchronized void addClient(String userId, String displayName, String clientIP, int clientPort, ClientHandler handler) {
        clients.put(userId, handler);
        System.out.println("[main.Server] " + displayName + " connected on Port: " + clientPort);

        if (coordinator == null) {
            coordinator = userId;
            handler.sendMessage("✅ You are the **Coordinator** of this chat.");
        } else {
            handler.sendMessage("📝 Current coordinator: " + clients.get(coordinator).getDisplayName());
        }
        broadcastMessage("📢 " + displayName + " has joined the chat.");
    }

    public synchronized void removeClient(String userId) {
        ClientHandler removedClient = clients.remove(userId);
        if (removedClient != null) {
            broadcastMessage("📢 " + removedClient.getDisplayName() + " has left the chat.");
        }
        if (coordinator != null && coordinator.equals(userId)) {
            assignNewCoordinator();
        }
    }

    private void assignNewCoordinator() {
        if (!clients.isEmpty()) {
            coordinator = clients.keySet().iterator().next();
            ClientHandler newCoordinator = clients.get(coordinator);
            if (newCoordinator != null) {
                newCoordinator.sendMessage("/BECOME_COORDINATOR");
                broadcastMessage("📢 New coordinator: " + newCoordinator.getDisplayName());
            }
        } else {
            coordinator = null;
        }
    }

    public synchronized String getUserDetails() {
        if (clients.isEmpty()) return "No active users.";
        StringBuilder details = new StringBuilder("📜 Active Users:\n");
        for (ClientHandler client : clients.values()) {
            details.append("🔹 Name: ").append(client.getDisplayName())
                    .append(" | ID: ").append(client.getUserId())
                    .append(" | IP: ").append(client.getClientIP())
                    .append(" | Port: ").append(client.getClientPort())
                    .append("\n");
        }
        return details.toString();
    }

    public void handleRequestDetails(String requesterId) {
        ClientHandler requester = clients.get(requesterId);
        if (requester == null) return;
        if (coordinator == null) {
            requester.sendMessage("⚠️ No coordinator available to provide details.");
            return;
        }
        ClientHandler coordinatorHandler = clients.get(coordinator);
        if (coordinatorHandler != null) {
            coordinatorHandler.sendMessage("📩 User " + requester.getDisplayName()
                    + " is requesting user details. Click 'Send Details' to approve.");
        } else {
            requester.sendMessage("⚠️ No coordinator available to provide details.");
        }
    }


    public void sendPrivateMessage(String senderId, String recipientId, String recipientIP, int recipientPort, String message) {
        ClientHandler recipient = clients.get(recipientId);
        ClientHandler sender = clients.get(senderId);
        if (recipient != null && sender != null) {
            recipient.sendMessage("🔒 [From " + sender.getDisplayName() + "]: " + message);
            sender.sendMessage("🔒 [To " + recipient.getDisplayName() + "]: " + message);
        } else {
            if (sender != null) {
                sender.sendMessage("⚠️ User not found or incorrect format. Use **@UserID-IP-Port message**.");
            }
        }
    }

    public void sendUserDetails(String requesterId) {
        ClientHandler requester = clients.get(requesterId);
        if (requester == null) return;
        if (!requester.getUserId().equals(coordinator)) {
            requester.sendMessage("⚠️ Only the coordinator can send user details.");
            return;
        }
        String userDetails = getUserDetails();
        broadcastMessage("📜 [User Details]\n" + userDetails);
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    public synchronized void checkHeartbeat() {
        for (Iterator<String> it = clients.keySet().iterator(); it.hasNext(); ) {
            String uid = it.next();
            ClientHandler client = clients.get(uid);
            if (client != null && client.getSocket().isClosed()) {
                it.remove();
                broadcastMessage("📢 " + client.getDisplayName() + " was removed due to inactivity.");
                System.out.println("[main.Server] Removed inactive user: " + client.getDisplayName());
                if (uid.equals(coordinator)) {
                    assignNewCoordinator();
                }
            }
        }
        broadcastMessage("📜 Updated Active Users:\n" + getUserDetails());
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
