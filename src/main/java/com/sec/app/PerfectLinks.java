package com.sec.app;
import java.util.concurrent.*;

public class PerfectLinks {
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; // Store messages to resend
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates
    private DeliverCallback deliverCallback; // Callback to notify the main class

    public interface DeliverCallback {
        void deliverReceivedMessage(String srcIP, int srcPort, String message);
    }

    public PerfectLinks(int port) throws Exception {
        this.fairLossLinks = new FairLossLinks(port);
        this.sentMessages = new ConcurrentHashMap<>();
        this.delivered = new ConcurrentHashMap<>(); // Initialize delivered set

        // Register callback to receive messages from FairLossLinks
        this.fairLossLinks.setDeliverCallback(this::onFairLossDeliver);

        // Start listening for messages
        this.fairLossLinks.deliver();
    }

    // Set the callback to notify when a message is delivered
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    // Send a message Perfectly (keep resending)
    public void send(String destIP, int destPort, String message) {
        String messageKey = destIP + ":" + destPort + ":" + message;
        sentMessages.put(messageKey, true);

        // Resend indefinitely (until process crashes)
        new Thread(() -> {
            while (sentMessages.containsKey(messageKey)) {
                try {
                    fairLossLinks.send(destIP, destPort, message);
                    Thread.sleep(1000); // Resend every second (adjust as needed)
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }// wait for ACK //exponential backoff mandar uma vez e esperar. se o ack n chegar passado 1 segundo volto a mandar mensagem e depois espero exponecialmente. Message ids
        }).start();
    }

    // Handle received messages from FairLossLinks
    private void onFairLossDeliver(String srcIP, int srcPort, String message) {
        String messageKey = srcIP + ":" + srcPort + ":" + message;

        // Deliver only if the message has not been delivered before
        if (!delivered.containsKey(messageKey)) {
            delivered.put(messageKey, true); // Mark message as delivered

            if (deliverCallback != null) {
                deliverCallback.deliverReceivedMessage(srcIP, srcPort, message);
            }
        }
    }

    // Stop resending a message (if needed)
    public void stopResending(String destIP, int destPort, String message) {
        String messageKey = destIP + ":" + destPort + ":" + message;
        sentMessages.remove(messageKey);
    }
}
