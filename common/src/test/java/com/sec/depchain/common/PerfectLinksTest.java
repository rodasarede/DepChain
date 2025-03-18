package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;

class PerfectLinksTest {
    private PerfectLinks sender;
    private PerfectLinks receiver;
    private final int senderId = 1;
    private final int receiverId = 2;
    private BlockingQueue<String> receivedMessages;

    @BeforeEach
    void setUp() throws Exception {
        receivedMessages = new LinkedBlockingQueue<>();
        sender = new PerfectLinks(senderId);
        receiver = new PerfectLinks(receiverId);
        
        receiver.setDeliverCallback((nodeId, message) -> receivedMessages.offer(message));
    }

    @AfterEach
    void tearDown() {
        sender.close();
        receiver.close();
    }

    @Test
    void testPL1_Validity() throws Exception {
        String testMessage = "<append:a>";
        sender.send(receiverId, testMessage);
        
        String received = receivedMessages.poll(5, TimeUnit.SECONDS);
        System.out.println(received);
        String receivedMessage = received.split("\\|")[2];
        assertEquals(testMessage, receivedMessage, "Message should be delivered correctly");
    }

    @Test
    // Test for no duplication on appends
    void testPL2_NoDuplication() throws Exception {
        String testMessage = "<append:a>";
        sender.send(receiverId, testMessage);
        
        String received1 = receivedMessages.poll(5, TimeUnit.SECONDS);
        String received2 = receivedMessages.poll(1, TimeUnit.SECONDS);
        
        String receivedMessage1 = received1.split("\\|")[2];
        assertEquals(testMessage, receivedMessage1, "First delivery should match");
        assertNull(received2, "No duplicate messages should be delivered");
    }
}
