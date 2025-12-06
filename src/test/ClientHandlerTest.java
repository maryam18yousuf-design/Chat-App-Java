package test;

import main.ChatServer;
import main.ClientHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandlerTest {

    // Dummy ChatServer that records calls to key methods.
    static class DummyChatServer extends ChatServer {
        public List<String> addedClients = new ArrayList<>();
        public List<String> removedClients = new ArrayList<>();
        public List<String> broadcastMessages = new ArrayList<>();

        @Override
        public synchronized void addClient(String userId, String displayName, String clientIP, int clientPort, ClientHandler handler) {
            addedClients.add(displayName);
        }

        @Override
        public synchronized void removeClient(String userId) {
            removedClients.add(userId);
        }

        @Override
        public synchronized void broadcastMessage(String message) {
            broadcastMessages.add(message);
        }
    }

    // TestSocket simulates a Socket using in-memory streams.
    static class TestSocket extends Socket {
        private ByteArrayInputStream in;
        private ByteArrayOutputStream out;
        private boolean closed = false;

        public TestSocket(String input) {
            this.in = new ByteArrayInputStream(input.getBytes());
            this.out = new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return in;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return out;
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public int getPort() {
            return 12345;
        }

        public String getOutput() {
            return out.toString();
        }
    }

    // Test the complete run() behavior of ClientHandler.
    // The simulated client input:
    // 1. Sends "u1 Alice" as user details.
    // 2. Sends "Hello world" as a broadcast message.
    // 3. Then the stream ends (simulating client disconnect).
    @Test
    public void testClientHandlerRun() throws Exception {
        String input = "u1 Alice\nHello world\n";
        TestSocket testSocket = new TestSocket(input);
        DummyChatServer dummyServer = new DummyChatServer();
        ClientHandler clientHandler = new ClientHandler(testSocket, dummyServer);

        // Run the client handler (it will process input and then exit).
        clientHandler.run();

        // Verify that addClient was called with "Alice".
        assertTrue(dummyServer.addedClients.contains("Alice"), "Expected Alice to be added as a client.");
        // Verify that broadcastMessage was called with "Alice: Hello world".
        assertTrue(dummyServer.broadcastMessages.contains("Alice: Hello world"),
                "Expected broadcast of 'Alice: Hello world'.");
        // Verify that removeClient was called with "u1".
        assertTrue(dummyServer.removedClients.contains("u1"), "Expected user ID 'u1' to be removed.");
        // Verify that the socket was closed.
        assertTrue(testSocket.isClosed(), "Expected the socket to be closed.");
    }

    // Test that sendMessage() writes the correct output to the socket.
    @Test
    public void testSendMessage() throws Exception {
        String input = "u1 Alice\n"; // Provide user details.
        TestSocket testSocket = new TestSocket(input);
        DummyChatServer dummyServer = new DummyChatServer();
        ClientHandler clientHandler = new ClientHandler(testSocket, dummyServer);

        // Instead of calling run() (which would initialize and then close the socket),
        // we manually initialize the PrintWriter 'out' using reflection.
        PrintWriter writer = new PrintWriter(testSocket.getOutputStream(), true);
        Field outField = ClientHandler.class.getDeclaredField("out");
        outField.setAccessible(true);
        outField.set(clientHandler, writer);

        // Now call sendMessage().
        clientHandler.sendMessage("Test message");

        String output = testSocket.getOutput().trim();
        assertEquals("Test message", output, "Expected output to match the sent message.");
    }
}
