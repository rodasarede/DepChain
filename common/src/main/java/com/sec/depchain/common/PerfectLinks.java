package com.sec.depchain.common;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;

public class PerfectLinks {
    private DeliverCallback deliverCallbackCollect; // Callback to deliver to the class above
    private DeliverCallback deliverCallback; // Callback to deliver to the class above
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; // Store messages to resend //TODO nao podemos
                                                                   // guardar mensagens infinitamente -> mudar para
                                                                   // sequence number
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates
    private static SystemMembership systemMembership;
    private final int nodeId;
    private final int port;
    private int seqNumber = 0;

    private final PrivateKey privateKey;

    public interface DeliverCallback {
        void deliver(int NodeId, String message);
    }

    // TODO sequence number in order to make in-order delivery
    public PerfectLinks(int nodeId) throws Exception {
        systemMembership = new SystemMembership(
                Constants.PROPERTIES_PATH);
        this.port = getPort(nodeId);
        System.out.println("I'm on port " + this.port);
        this.fairLossLinks = new FairLossLinks(this.port);
        this.sentMessages = new ConcurrentHashMap<>();
        this.delivered = new ConcurrentHashMap<>(); // Initialize delivered set
        this.nodeId = nodeId;

        // Register callback to receive messages from FairLossLinks
        this.fairLossLinks.setDeliverCallback(this::onFairLossDeliver);

        // Start listening for messages
        this.fairLossLinks.deliver();

        this.privateKey = KeyLoader.loadPrivateKey(
                "../common/src/main/java/com/sec/depchain/resources/keys/private_key_" + this.nodeId + ".pem");
    }

    // Set the callback to notify when a message is delivered
    public void setDeliverCallbackCollect(DeliverCallback callback) {
        this.deliverCallbackCollect = callback;
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    // Send a message Perfectly (keep resending)
    public void send(int destId, String message) {
        String destIP = getIP(destId);
        int destPort = getPort(destId);
        String messageKey = destId + ":" + message;
        sentMessages.put(messageKey, true);

        seqNumber++;
        String messageWithId = nodeId + "|" + seqNumber + "|" + message;
        // Resend indefinitely (until process crashes)
        PublicKey destPublicKey = this.systemMembership.getPublicKey(destId);
        try {
            // mac
            String mac = CryptoUtils.generateMAC(privateKey, destPublicKey, messageWithId);

            // tampering mac
            /*
             * 
             * String tamperedMac = mac.substring(0, mac.length() - 1) + "b";
             * System.out.println("original mac: " + mac);
             * System.out.println("tampered mac: " + tamperedMac);
             * mac = tamperedMac;
             * 
             */

            AtomicLong timeout = new AtomicLong(1000);
            String authenticatedMsg = messageWithId + "|" + mac; // append mac
            new Thread(() -> {
                while (sentMessages.containsKey(messageKey)) {
                    try {
                        fairLossLinks.send(destIP, destPort, authenticatedMsg); // send authenticated msg

                        Thread.sleep(timeout.get()); // Resend every second (adjust as needed)
                        timeout.set((long) (1.5 * timeout.get())); // flexible
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

        String[] parts = message.split("\\|");
        if (parts.length != 4) {
            System.out.println("Invalid message format: " + message);
            return;

        }
        String messageWithId = parts[0] + "|" + parts[1] + "|" + parts[2];

        String originalMsg = parts[2];

        String receivedMac = parts[3];

        int senderNodeId = !parts[0].startsWith("ACK") ? Integer.parseInt(parts[0])
                : Integer.parseInt(parts[0].substring(3));
        String messageKey = senderNodeId + ":" + parts[2];

        PublicKey destPublicKey = this.systemMembership.getPublicKey(senderNodeId); // Not sure is nodeID

        try {

            if (!CryptoUtils.verifyMAC(privateKey, destPublicKey, messageWithId, receivedMac)) {
                System.out.println("MAC verification failed for message: " + messageWithId);
                return;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Deliver only if the message has not been delivered before
        if (!delivered.containsKey(messageKey)) {
            delivered.put(messageKey, true); // Mark message as delivered

            if (message.startsWith("ACK")) {

                stopResending(messageKey);
                return;
            }

            // Send ACK back to the sender via a single message using fairloss
            try {
                seqNumber++;
                String ackMessage = "ACK" + nodeId + "|" + seqNumber + "|" + originalMsg;
                String ackMAC = CryptoUtils.generateMAC(privateKey, destPublicKey, ackMessage);
                fairLossLinks.send(srcIP, srcPort, ackMessage + "|" + ackMAC);

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (deliverCallbackCollect != null || deliverCallback != null) {
                System.out.println("PerfectLinks delivering message up: " + message);

                System.out.println("Stripping message...");

                if (getMessageType(originalMsg).equals("append") || getMessageType(originalMsg).equals("READ")
                        || getMessageType(originalMsg).equals("WRITE")
                        || getMessageType(originalMsg).equals("ACCEPT")) {
                    deliverCallback.deliver(senderNodeId, message);
                } else {
                    deliverCallbackCollect.deliver(senderNodeId, originalMsg);
                }
            }
        }
    }

    public String stripMessageToBeDelivered(String message) {
        int firstSeparatorIndex = message.indexOf("|");

        if (firstSeparatorIndex != -1) {
            message = message.substring(firstSeparatorIndex + 1); // Skip past "<number>|"
        }

        int separatorIndex = message.lastIndexOf("|");
        if (separatorIndex != -1) {
            return message.substring(0, separatorIndex);
        }

        return message;
    }

    // Stop resending a message (if needed)
    public void stopResending(String messageKey) {
        sentMessages.remove(messageKey);
    }

    // debug
    public void printSentMessages() {
        System.out.println("Sent Messages: ");
        for (String key : sentMessages.keySet()) {
            System.out.println(key);
        }
    }

    public static String[] getMessageElements(String message) {
        // Extract between "<" and ">"
        int start = message.indexOf('<');
        int end = message.indexOf('>');
        if (start != -1 && end != -1 && start < end) {

            String extractedPart = message.substring(start + 1, end);
            String[] elements = extractedPart.split(":");
            return elements;
        } else {
            System.out.println("Invalid format in message: " + message);
            return null;
        }
    }

    private int getPort(int nodeId) {
        if (systemMembership.getMembershipList().get(nodeId) == null) {
            return 6000 + nodeId;
        }
        return systemMembership.getMembershipList().get(nodeId).getPort(); // port
    }

    private String getIP(int nodeId) {
        if (systemMembership.getMembershipList().get(nodeId) == null) {
            return "127.0.0.1";
        }
        return systemMembership.getMembershipList().get(nodeId).getAddress(); // leader Ip
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    private String getMessageType(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            String[] parts = content.split(":");

            return parts[0];
        }
        return Constants.UNKNOWN;
    }

}
