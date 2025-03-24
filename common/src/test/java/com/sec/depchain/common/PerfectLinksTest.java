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

    // @Test
    // // eventually deliver a message
    // // hard to test because we have no way of knowing receiver is correct and even if it is correct there can be delays
    // void testPL1_Validity() throws Exception {
    //     String testMessage = "<append:a>";
    //     sender.send(receiverId, testMessage);
        
    //     String received = receivedMessages.poll(5, TimeUnit.SECONDS);
    //     System.out.println(received);
    //     String receivedMessage = received.split("\\|")[2];
    //     assertEquals(testMessage, receivedMessage, "Message should be delivered correctly");
    // }

    // @Test
    // // Test for at most once
    // TODO need to change perfect links in order to be possible send a message with a seqNumber of choice
    // to show that the message is not delivered twice, no duplication/ REPLAY
    // void testPL2_NoDuplication() throws Exception {
    //     String testMessage = "<append-request:a>";
    //     sender.send(receiverId, testMessage);
        
    //     String received1 = receivedMessages.poll(5, TimeUnit.SECONDS);
    //     sender.send(receiverId, testMessage);
    //     String received2 = receivedMessages.poll(1, TimeUnit.SECONDS);
        
        
    //     assertEquals(testMessage, received1, "First delivery should match");
    //     assertNull(received2, "No duplicate messages should be delivered");
    // }
}
