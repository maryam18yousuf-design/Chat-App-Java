package test;

import main.Client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientTest {

    @Test
    public void testClientStart() throws Exception {
        // 1. Set up a dummy server on an ephemeral port.
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        final String[] receivedClientName = new String[1];

        // Start a server thread that accepts the client connection.
        Thread serverThread = new Thread(() -> {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                // Read the first line sent by the client (client name).
                String line = in.readLine();
                receivedClientName[0] = line;
                // Send a welcome message to the client.
                out.println("Welcome " + line);
                // Pause a bit before closing so the client can read the message.
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // 2. Prepare simulated console input for the Client.
        // Provide a single line "Test message" so that the client's console loop terminates.
        String consoleInput = "Test message\n";
        ByteArrayInputStream testIn = new ByteArrayInputStream(consoleInput.getBytes());
        InputStream originalIn = System.in;
        System.setIn(testIn);

        // 3. Capture System.out to verify the client's printed output.
        ByteArrayOutputStream testOut = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(testOut));

        // 4. Create and start the client. It will connect to our dummy server.
        Client client = new Client("127.0.0.1", port, "TestClient");
        Thread clientThread = new Thread(client::start);
        clientThread.start();

        // Wait for both the client and server threads to finish.
        clientThread.join(5000);
        serverThread.join(5000);

        // 5. Wait a short time to allow the client's reader thread to process the welcome message.
        long startTime = System.currentTimeMillis();
        while (!testOut.toString().contains("Welcome TestClient") &&
                System.currentTimeMillis() - startTime < 3000) {
            Thread.sleep(100);
        }

        // Restore original System.in and System.out.
        System.setIn(originalIn);
        System.setOut(originalOut);

        // 6. Verify that the dummy server received "TestClient" as the client name.
        assertEquals("TestClient", receivedClientName[0], "Dummy server should receive client name 'TestClient'");

        // Verify that the client's output contains the welcome message from the server.
        String clientOutput = testOut.toString();
        assertTrue(clientOutput.contains("Welcome TestClient"),
                "Expected client output to contain 'Welcome TestClient'. Actual output: " + clientOutput);
    }
}

