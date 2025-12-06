package test;

import main.ChatWindow;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

public class ChatWindowTest {

    @Test
    public void testBecomeCoordinatorUpdatesUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ChatWindow chatWindow = new ChatWindow();
            // Initialize the chat UI components
            chatWindow.createChatUI();

            // Simulate the coordinator state change.
            chatWindow.becomeCoordinator();

            // After becoming coordinator, the "Send Details" and "Check Active" buttons should be visible.
            assertTrue(chatWindow.isSendDetailsButtonVisible(), "Send Details button should be visible");
            assertTrue(chatWindow.isCheckActiveButtonVisible(), "Check Active button should be visible");
        });
    }
}

