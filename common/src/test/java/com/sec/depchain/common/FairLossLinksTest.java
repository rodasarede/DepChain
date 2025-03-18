package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;


public class FairLossLinksTest {
    private FairLossLinks sender;
    private FairLossLinks receiver;
    private CopyOnWriteArrayList<String> receivedMessages;
    private static final int TEST_PORT = 5000;
    private static final int TIMEOUT = 5; // seconds
    
    @BeforeEach
    public void setup() throws Exception {
        receivedMessages = new CopyOnWriteArrayList<>();
        receiver = new FairLossLinks(TEST_PORT);
        receiver.setDeliverCallback((srcIP, srcPort, message) -> receivedMessages.add(message));
        receiver.deliver();
        sender = new FairLossLinks(TEST_PORT + 1);
    }
    
    @AfterEach
    public void teardown() {
        System.out.println("After each");
        sender.close();
        receiver.close();
        receivedMessages.clear();
    }
    
    @Test
    // test simple message delivery
    public void testFairLoss() throws Exception {
        String message = "Hello";
        int sendCount = 50;
        
        for (int i = 0; i < sendCount; i++) {
            sender.send("127.0.0.1", TEST_PORT, message);
            Thread.sleep(10); 
        }
        
        Thread.sleep(2000); 
        assertTrue(receivedMessages.size() == sendCount, "Messages should be received");
    }
    
    @Test
    // test for no duplication
    public void testFiniteDuplication() throws Exception {
        String message = "TestMessage";
        int sendCount = 10;
        
        for (int i = 0; i < sendCount; i++) {
            sender.send("127.0.0.1", TEST_PORT, message);
            Thread.sleep(10);
        }
        
        Thread.sleep(2000);
        assertTrue(receivedMessages.size() <= sendCount, "Messages should not be received more than sent");
    }
    
    @Test
    // test for no creation
    public void testNoCreation() throws Exception {
        new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT * 1000);
            } catch (InterruptedException ignored) {
            }
        }).start();
        
        assertEquals(0, receivedMessages.size(), "No messages should be delivered unless sent");
    }
}
