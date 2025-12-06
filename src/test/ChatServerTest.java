package test;

import main.ChatServer;
import main.ClientHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ChatServerTest {

    private ChatServer chatServer;

    @BeforeEach
    public void setup() {
        chatServer = new ChatServer();
    }

    // Dummy Socket to simulate closed/open state.
    static class DummySocket extends Socket {
        private boolean closed;

        public DummySocket(boolean closed) {
            this.closed = closed;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    // Dummy ClientHandler that bypasses real network I/O.
    static class DummyClientHandler extends ClientHandler {
        private List<String> messages = new ArrayList<>();
        private boolean socketClosed = false;
        private final String userId;
        private final String displayName;
        private final String clientIP = "127.0.0.1";
        private final int clientPort = 12345;

        public DummyClientHandler(String userId, String displayName, ChatServer server) {
            // Pass a dummy socket to the superclass.
            super(new DummySocket(false), server);
            this.userId = userId;
            this.displayName = displayName;
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setSocketClosed(boolean closed) {
            socketClosed = closed;
        }

        @Override
        public Socket getSocket() {
            return new DummySocket(socketClosed);
        }

        @Override
        public String getUserId() {
            return userId;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getClientIP() {
            return clientIP;
        }

        @Override
        public int getClientPort() {
            return clientPort;
        }
    }

    @Test
    public void testAddClientFirstClientBecomesCoordinator() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);

        // First client should be notified as coordinator.
        assertTrue(client1.getMessages().contains("✅ You are the **Coordinator** of this chat."));
        // And a broadcast message is sent.
        boolean hasJoinMessage = client1.getMessages().stream().anyMatch(msg -> msg.contains("has joined the chat"));
        assertTrue(hasJoinMessage);
    }

    @Test
    public void testAddClientSecondClientGetsCoordinatorInfo() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Second client should be informed about the current coordinator.
        boolean hasCoordinatorMessage = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("Current coordinator: Alice"));
        assertTrue(hasCoordinatorMessage);
    }

    @Test
    public void testRemoveClientBroadcastsLeftMessage() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.removeClient("u1");

        // After removal, expect no active users.
        assertEquals("No active users.", chatServer.getUserDetails());
    }

    @Test
    public void testAssignNewCoordinatorAfterRemoval() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Remove the current coordinator.
        chatServer.removeClient("u1");

        // Bob should be promoted as coordinator.
        boolean newCoordinatorMsg = client2.getMessages().stream()
                .anyMatch(msg -> msg.equals("/BECOME_COORDINATOR"));
        assertTrue(newCoordinatorMsg);
    }

    @Test
    public void testGetUserDetails() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        String details = chatServer.getUserDetails();
        assertTrue(details.contains("Alice"));
        assertTrue(details.contains("Bob"));
        assertTrue(details.contains("u1"));
        assertTrue(details.contains("u2"));
    }

    @Test
    public void testHandleRequestDetailsCoordinatorAvailable() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Bob requests details; Alice (coordinator) should receive a request.
        chatServer.handleRequestDetails("u2");
        boolean requestMsg = client1.getMessages().stream()
                .anyMatch(msg -> msg.contains("is requesting user details"));
        assertTrue(requestMsg);
    }

    @Test
    public void testHandleRequestDetailsNoCoordinatorNonExisting() throws Exception {
        ChatServer chatServer = new ChatServer();
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Force coordinator to a user ID not in the clients map.
        Field coordinatorField = ChatServer.class.getDeclaredField("coordinator");
        coordinatorField.setAccessible(true);
        coordinatorField.set(chatServer, "nonExistingUser");

        chatServer.handleRequestDetails("u2");
        boolean noCoordinatorMsg = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("No coordinator available"));
        assertTrue(noCoordinatorMsg, "Expected a message indicating no coordinator is available.");
    }

    @Test
    public void testHandleRequestDetailsNoCoordinatorNull() throws Exception {
        ChatServer chatServer = new ChatServer();
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Force the coordinator field to null.
        Field coordinatorField = ChatServer.class.getDeclaredField("coordinator");
        coordinatorField.setAccessible(true);
        coordinatorField.set(chatServer, null);

        chatServer.handleRequestDetails("u2");
        boolean noCoordinatorMsg = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("No coordinator available"));
        assertTrue(noCoordinatorMsg, "Expected a message indicating no coordinator is available.");
    }

    @Test
    public void testSendPrivateMessageValid() {
        DummyClientHandler sender = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler recipient = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, sender);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, recipient);

        chatServer.sendPrivateMessage("u1", "u2", "127.0.0.1", 12346, "Hello Bob!");

        boolean recipientMsg = recipient.getMessages().stream()
                .anyMatch(msg -> msg.contains("[From Alice]") && msg.contains("Hello Bob!"));
        boolean senderMsg = sender.getMessages().stream()
                .anyMatch(msg -> msg.contains("[To Bob]") && msg.contains("Hello Bob!"));
        assertTrue(recipientMsg);
        assertTrue(senderMsg);
    }

    @Test
    public void testSendPrivateMessageInvalidRecipient() {
        DummyClientHandler sender = new DummyClientHandler("u1", "Alice", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, sender);

        chatServer.sendPrivateMessage("u1", "u2", "127.0.0.1", 12346, "Hello Bob!");
        boolean errorMsg = sender.getMessages().stream()
                .anyMatch(msg -> msg.contains("User not found"));
        assertTrue(errorMsg);
    }

    @Test
    public void testSendUserDetailsNonCoordinator() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Bob (not the coordinator) tries to send user details.
        chatServer.sendUserDetails("u2");
        boolean errorMsg = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("Only the coordinator can send user details"));
        assertTrue(errorMsg);
    }

    @Test
    public void testSendUserDetailsByCoordinator() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Coordinator (Alice) sends user details.
        chatServer.sendUserDetails("u1");
        boolean broadcastAlice = client1.getMessages().stream()
                .anyMatch(msg -> msg.contains("[User Details]"));
        boolean broadcastBob = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("[User Details]"));
        assertTrue(broadcastAlice);
        assertTrue(broadcastBob);
    }

    @Test
    public void testBroadcastMessage() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        chatServer.broadcastMessage("Test broadcast");
        boolean msgAlice = client1.getMessages().stream()
                .anyMatch(msg -> msg.equals("Test broadcast"));
        boolean msgBob = client2.getMessages().stream()
                .anyMatch(msg -> msg.equals("Test broadcast"));
        assertTrue(msgAlice);
        assertTrue(msgBob);
    }

    @Test
    public void testCheckHeartbeatRemovesClosedClients() {
        DummyClientHandler client1 = new DummyClientHandler("u1", "Alice", chatServer);
        DummyClientHandler client2 = new DummyClientHandler("u2", "Bob", chatServer);
        chatServer.addClient("u1", "Alice", "127.0.0.1", 12345, client1);
        chatServer.addClient("u2", "Bob", "127.0.0.1", 12346, client2);

        // Simulate client1’s socket being closed.
        client1.setSocketClosed(true);
        chatServer.checkHeartbeat();

        // Client1 should be removed.
        String details = chatServer.getUserDetails();
        assertFalse(details.contains("Alice"));

        // Client2 should have received a removal broadcast.
        boolean removalMsg = client2.getMessages().stream()
                .anyMatch(msg -> msg.contains("was removed due to inactivity"));
        assertTrue(removalMsg);
    }
}

