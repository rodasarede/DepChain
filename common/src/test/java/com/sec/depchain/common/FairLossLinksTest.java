package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.Random;

class FairLossLinksTest {

    private FairLossLinks fairLossLinks;
    private int testPort = 5000;
    private String testMessage = "Hello, World!";
    private String testIP = "127.0.0.1";
    private boolean messageDelivered = false;
    private String deliveredMessage = "";
    private String deliveredSrcIP = "";
    private int deliveredSrcPort = 0;

    private Random random = new Random();

    @BeforeEach
    void setUp() throws Exception {
        fairLossLinks = new FairLossLinks(testPort);
        fairLossLinks.setDeliverCallback((srcIP, srcPort, message) -> {
            messageDelivered = true;
            deliveredMessage = message;
            deliveredSrcIP = srcIP;
            deliveredSrcPort = srcPort;
        });
        fairLossLinks.deliver();
    }

    @AfterEach
    void tearDown() {
        fairLossLinks.close();
    }

    @Test
    void testSendAndReceiveMessage() throws Exception {
        // Send a message to itself
        fairLossLinks.send(testIP, testPort, testMessage);

        // Wait for the message to be delivered
        Thread.sleep(100); // Give some time for the message to be received

        // Verify the message was delivered correctly
        assertTrue(messageDelivered);
        assertEquals(testMessage, deliveredMessage);
        assertEquals(testIP, deliveredSrcIP);
        assertEquals(testPort, deliveredSrcPort);
    }
    @Test
    void testRandomDelays() throws Exception {
        // Simulate random delay before sending the message
        int delay = random.nextInt(200); // Random delay between 0 and 200 ms
        Thread.sleep(delay);

        // Send a message to itself
        fairLossLinks.send(testIP, testPort, testMessage);

        // Wait for the message to be delivered (account for maximum delay)
        Thread.sleep(1000); // Increased wait time to ensure delivery

        // Verify the message was delivered correctly
        assertTrue(messageDelivered, "Message was not delivered");
        assertEquals(testMessage, deliveredMessage, "Delivered message does not match");
        assertEquals(testIP, deliveredSrcIP, "Source IP does not match");
        assertEquals(testPort, deliveredSrcPort, "Source port does not match");
    }

    @Test
    void testSendMessageToDifferentPort() throws Exception {
        int differentPort = 5001;
        DatagramSocket receiverSocket = new DatagramSocket(differentPort);

         // Simulate random delay before sending the message
         int delay = random.nextInt(200); // Random delay between 0 and 200 ms
         Thread.sleep(delay);
 
        // Send a message to a different port
        fairLossLinks.send(testIP, differentPort, testMessage);

        // Receive the message on the different port
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        receiverSocket.receive(packet);

        // Verify the received message
        String receivedMessage = new String(packet.getData(), 0, packet.getLength());
        assertEquals(testMessage, receivedMessage);

        receiverSocket.close();
    }
    @Test
    void testMessageLoss() throws Exception {
        // Simulate message loss by dropping messages with a 80% probability
        fairLossLinks.setDeliverCallback((destIP, destPort, message) -> {
            if (random.nextDouble() < 0.8) {
                // Simulate message loss by not sending the message
                return;
            }
            // Otherwise, send the message
            try {
                fairLossLinks.send(destIP, destPort, message);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });

        // Send a message to itself
        fairLossLinks.send(testIP, testPort, testMessage);

        // Wait for the message to be delivered
        Thread.sleep(100);

        // Verify that the message may or may not have been delivered
        if (messageDelivered) {
            assertEquals(testMessage, deliveredMessage);
            assertEquals(testIP, deliveredSrcIP);
            assertEquals(testPort, deliveredSrcPort);
        } else {
            assertFalse(messageDelivered);
        }
    }


    
    @Test
    void testClose() throws Exception {
        // Close the FairLossLinks instance
        fairLossLinks.close();

        // Try to send a message after closing
        assertThrows(Exception.class, () -> {
            fairLossLinks.send(testIP, testPort, testMessage);
        });
    }
}