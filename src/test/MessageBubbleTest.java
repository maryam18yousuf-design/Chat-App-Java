package test;

import main.MessageBubble;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MessageBubbleTest {

    @Test
    public void testMessageDisplayedInLabel() {
        String testMsg = "Hello world!";
        LocalDateTime now = LocalDateTime.now();
        MessageBubble bubble = new MessageBubble(testMsg, now);

        // Traverse the component hierarchy to find a JLabel containing the test message.
        boolean found = false;
        for (Component comp : bubble.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText().contains(testMsg)) {
                    found = true;
                    break;
                }
            } else if (comp instanceof JPanel) {
                // Check nested components, e.g. in the footer panel.
                for (Component child : ((JPanel) comp).getComponents()) {
                    if (child instanceof JLabel) {
                        JLabel label = (JLabel) child;
                        if (label.getText().contains(testMsg)) {
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(found, "Expected the message to be displayed in one of the labels within MessageBubble");
    }
}

