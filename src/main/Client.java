package main;

import java.io.*;
import java.net.*;

public class Client {
    private final String serverIp;
    private final int serverPort;
    private final String clientName;

    public Client(String serverIp, int serverPort, String clientName) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.clientName = clientName;
    }

    public void start() {
        try (Socket socket = new Socket(serverIp, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("[main.Client] Connected to the server.");
            out.println(clientName);

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("[main.Client] Connection lost.");
                }
            }).start();

            String userMessage;
            while ((userMessage = console.readLine()) != null) {
                out.println(userMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java main.Client <server_ip> <server_port> <client_name>");
            return;
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String clientName = args[2];

        Client client = new Client(serverIp, serverPort, clientName);
        client.start();
    }
}