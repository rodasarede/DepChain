package com.sec.app;

import java.security.PrivateKey;
import java.security.PublicKey;

import java.util.List;
import java.util.concurrent.*;

import com.sec.app.util.KeyLoader;
import com.sec.app.util.CryptoUtils;

public class PerfectLinks {
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; // Store messages to resend
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates
    private DeliverCallback deliverCallback; // Callback to notify the main class

    private static final int retries = 5; // Number of retries before considering failure

    private final PrivateKey _privateKey;

    private final List<PublicKey> publicKeys;

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

        this._privateKey = KeyLoader.loadPrivateKey("keys/private_key_" + port + ".pem"); // TODO here with the id of
                                                                                          // the
                                                                                          // node
        this.publicKeys = KeyLoader.loadPublicKeys("keys/all_public_keys.pem");
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

        // generate mac
        try {
            PublicKey destPublicKey = publicKeys.get(Integer.parseInt(destIP.split("\\.")[3]) - 1); // TODO Extract node
                                                                                                    // ID from
            // IP
            String mac = CryptoUtils.generateMAC(_privateKey, destPublicKey, message);
            String authenticatedMsg = message + "|" + mac; // append mac
            new Thread(() -> {
                int retriesLeft = retries;
                while (sentMessages.containsKey(messageKey)) {
                    try {
                        if (retriesLeft == 0) {
                            System.out.println("Message not delivered after 5 retries");
                            break;
                        }
                        fairLossLinks.send(destIP, destPort, authenticatedMsg); // send authenticated msg
                        retriesLeft--;
                        Thread.sleep(1000); // Resend every second (adjust as needed)
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handle received messages from FairLossLinks
    private void onFairLossDeliver(String srcIP, int srcPort, String message) {
        String messageKey = srcIP + ":" + srcPort + ":" + message;

        String[] parts = message.split("\\|");
        if (parts.length != 2) {
            System.out.println("Invalid message format: " + message);
            return;

        }

        String originalMsg = parts[0];

        String receivedMac = parts[1];

        PublicKey destPublicKey = publicKeys.get(Integer.parseInt(srcIP.split("\\.")[3]) - 1); // TODO Extract node ID
                                                                                               // from
        // IP
        try {
            if (!CryptoUtils.verifyMAC(_privateKey, destPublicKey, originalMsg, receivedMac)) {
                System.out.println("MAC verification failed for message: " + originalMsg);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Deliver only if the message has not been delivered before
        if (!delivered.containsKey(messageKey)) {
            System.out.println("Fair loss Deliver from " + srcIP + ":" + srcPort + " -> " + message);
            delivered.put(messageKey, true); // Mark message as delivered

            if (originalMsg.startsWith("ACK:")) {
                // remove ACK: from message
                originalMsg = originalMsg.substring(4);
                System.out.println("stop resending message: " + message);
                stopResending(srcIP, srcPort, message);
                return;
            }
            // Send ACK back to the sender via a single message using fairloss
            try {
                String ackMessage = "ACK:" + originalMsg;
                String ackMAC = CryptoUtils.generateMAC(_privateKey, destPublicKey, ackMessage);
                fairLossLinks.send(srcIP, srcPort, ackMessage + "|" + ackMAC);

            } catch (Exception e) {
                e.printStackTrace();
            }
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

    // debug
    public void printSentMessages() {
        System.out.println("Sent Messages: ");
        for (String key : sentMessages.keySet()) {
            System.out.println(key);
        }
    }

}
