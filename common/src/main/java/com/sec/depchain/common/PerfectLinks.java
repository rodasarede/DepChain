package com.sec.depchain.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;

public class PerfectLinks {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerfectLinks.class);
    private DeliverCallback deliverCallbackCollect; // Callback to deliver to the class above
    private DeliverCallback deliverCallback; // Callback to deliver to the class above
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; ///TODO not a good practice // guardar mensagens infinitamente -> mudar para sequence number
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates

    //private final ConcurrentHashMap<Integer, Integer> sentMessages = new ConcurrentHashMap<>();
    //private final ConcurrentHashMap<Integer, Integer> deliveredMessages = new ConcurrentHashMap<>();
    private static SystemMembership systemMembership;
    private final int nodeId;
    private final int port;
    private int seqNumber = 0;

    private final PrivateKey privateKey;
    private static final int DEBUG_MODE = 1;

    public interface DeliverCallback {
        void deliver(int NodeId, String message);
    }

    // TODO sequence number in order to make in-order delivery
    public PerfectLinks(int nodeId) throws Exception {
        systemMembership = new SystemMembership(
                Constants.PROPERTIES_PATH);
        this.port = getPort(nodeId);
        //System.out.println("I'm on port " + this.port);
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
        if (DEBUG_MODE == 1) {
            LOGGER.debug("Sending message {} to server {}", message, destId);
        }
        String destIP = getIP(destId);
        int destPort = getPort(destId);
        // change key to have dest id and source id of a given message
        String messageKey = destId + ":" + nodeId + ":" + message;
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

    private boolean checkReceivedACK(String receivedACK) {
        String[] parts = receivedACK.split("\\|");

        if (parts.length != 5) {
            LOGGER.warn("Invalid ACK format: {}", receivedACK);
            return false;
        }

        String receivedWithoutMac = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
        int receivedSrcId = Integer.parseInt(parts[1]);
        int receivedSeqNum = Integer.parseInt(parts[2]);
        String receivedPayload = parts[3];
        String receivedMac = parts[4];

        if (DEBUG_MODE == 1) {
            LOGGER.debug("Extracted components:");
            LOGGER.debug("- Received srcID: {}", receivedSrcId);
            LOGGER.debug("- Received seqNum: {}", receivedSeqNum);
            LOGGER.debug("- Received message without MAC: {}", receivedWithoutMac);
            LOGGER.debug("- Received Payload: {}", receivedPayload);
            LOGGER.debug("- Received MAC: {}", receivedMac);
        }

        String receivedMessageKey = nodeId + ":" + receivedSrcId + ":" + receivedPayload;
        PublicKey receivedIdPK = this.systemMembership.getPublicKey(receivedSrcId);

        try {
            if (!CryptoUtils.verifyMAC(privateKey, receivedIdPK, receivedWithoutMac, receivedMac)) {
                LOGGER.warn("MAC verification failed for message: {}", receivedWithoutMac);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during MAC verification", e);
            return false;
        }
        return true;
    }

    private boolean checkReceivedMessage(String receivedMessage) {
        String[] parts = receivedMessage.split("\\|");

        if (parts.length != 4) {
            LOGGER.warn("Invalid message format: {}", receivedMessage);
            return false;
        }

        String receivedWithoutMac = parts[0] + "|" + parts[1] + "|" + parts[2];
        int receivedSrcId = Integer.parseInt(parts[0]);
        int receivedSeqNum = Integer.parseInt(parts[1]);
        String receivedPayload = parts[2];
        String receivedMac = parts[3];

        if (DEBUG_MODE == 1) {
            LOGGER.debug("Extracted components:");
            LOGGER.debug("- Received srcID: {}", receivedSrcId);
            LOGGER.debug("- Received seqNum: {}", receivedSeqNum);
            LOGGER.debug("- Received message without MAC: {}", receivedWithoutMac);
            LOGGER.debug("- Received Payload: {}", receivedPayload);
            LOGGER.debug("- Received MAC: {}", receivedMac);
        }

        String receivedMessageKey = nodeId + ":" + receivedSrcId + ":" + receivedPayload;
        PublicKey receivedIdPK = this.systemMembership.getPublicKey(receivedSrcId);

        try {
            if (!CryptoUtils.verifyMAC(privateKey, receivedIdPK, receivedWithoutMac, receivedMac)) {
                LOGGER.warn("MAC verification failed for message: {}", receivedWithoutMac);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Exception during MAC verification", e);
            return false;
        }
        return true;
    }

    private void sendACK(String destIP, int destPort, PublicKey destPublicKey, String payload) {
        try {
            seqNumber++;
            String ackMessage = "ACK|" + nodeId + "|" + seqNumber + "|" + payload;
            String ackMAC = CryptoUtils.generateMAC(privateKey, destPublicKey, ackMessage);
            fairLossLinks.send(destIP, destPort, ackMessage + "|" + ackMAC);

            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sent ACK message: {}", ackMessage);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while sending ACK", e);
        }
    }

    private void deliverMessage(int srcId, String message, String bigMessage) {
        String messageType = getMessageType(message);
        if (DEBUG_MODE == 1) {
            LOGGER.debug("Message type identified: {}", messageType);
        }

        if (deliverCallbackCollect != null || deliverCallback != null) {

            if (messageType.equals("append-request") ||
                    messageType.equals("append-response") ||
                    messageType.equals("READ") ||
                    messageType.equals("WRITE") ||
                    messageType.equals("ACCEPT")) {
                deliverCallback.deliver(srcId, message);
            } else {
                deliverCallbackCollect.deliver(srcId, message);
            }
        }
    }

    private void onFairLossDeliver(String srcIP, int srcPort, String receivedMessage) {
        if (receivedMessage.startsWith("ACK")) {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Received ACK: {}", receivedMessage);
                LOGGER.debug("Received ACK: ACK|<srcID>|<seq_number>|<receivedPayload>|<receivedMac>");
            }

            if (checkReceivedACK(receivedMessage)){
                String[] parts = receivedMessage.split("\\|");

                int receivedSrcId = Integer.parseInt(parts[1]);
                String receivedPayload = parts[3];

                // TODO can this messageKey be repeated? i think it can
                String correspondingMessageKey = receivedSrcId + ":" + nodeId + ":" + receivedPayload;

                LOGGER.debug("Stopping to resend: {}", correspondingMessageKey);
                stopResending(correspondingMessageKey);
                return;
            }
            else {
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Check of the received ACK failed. Ignoring message.");
                }
                return;
            }
        }
        else {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Received message: {}", receivedMessage);
                LOGGER.debug("Received message: <srcID>|<seq_number>|<receivedPayload>|<receivedMac>");
            }

            if (checkReceivedMessage(receivedMessage)) {
                String[] parts = receivedMessage.split("\\|");

                int receivedSrcId = Integer.parseInt(parts[0]);
                String receivedPayload = parts[2];

                PublicKey receivedIdPK = this.systemMembership.getPublicKey(receivedSrcId);

                // TODO can this messageKey be repeated? i think it can
                String messageKey = receivedSrcId + ":" + nodeId + ":" + receivedPayload;

                LOGGER.debug("Sending ACK to the following message: {}", receivedMessage);
                sendACK(srcIP, srcPort, receivedIdPK, receivedPayload); // TODO send to the srcIP or to the src ID?

                if (delivered.containsKey(messageKey)) {
                    if (DEBUG_MODE == 1) {
                        LOGGER.debug("Message already delivered: {} with key {}", receivedMessage, messageKey);
                    }
                    return;
                }

                delivered.put(messageKey, true);
                deliverMessage(receivedSrcId, receivedPayload, receivedMessage);

                return;
            } else {
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Check of the received message failed. Ignoring message.");
                }
                return;
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

    public String getMessageType(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            String[] parts = content.split(":");

            //System.out.println("will return type of message " + message + ": " + parts[0]);
            return parts[0];
        }
        return Constants.UNKNOWN;
    }

}
