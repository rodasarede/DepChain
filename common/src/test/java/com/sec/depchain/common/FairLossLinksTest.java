package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class FairLossLinksTest {

    private FairLossLinks fairLossLinks;
    private int testPort = 5000;
    private String testMessage = "Hello, World!";
    private String testIP = "127.0.0.1";
    private boolean messageDelivered = false;
    private String deliveredMessage = "";
    private String deliveredSrcIP = "";
    private int deliveredSrcPort = 0;

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
    void testSendMessageToDifferentPort() throws Exception {
        int differentPort = 5001;
        DatagramSocket receiverSocket = new DatagramSocket(differentPort);

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
    void testClose() throws Exception {
        // Close the FairLossLinks instance
        fairLossLinks.close();

        // Try to send a message after closing
        assertThrows(Exception.class, () -> {
            fairLossLinks.send(testIP, testPort, testMessage);
        });
    }
}