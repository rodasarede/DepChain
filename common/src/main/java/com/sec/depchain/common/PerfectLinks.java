package com.sec.depchain.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;

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

    private final ConcurrentHashMap<Integer, Deque<Integer>> waitingForACK;
    private final ConcurrentHashMap<Integer, AtomicInteger> higestSeqNumberSent;
    //private final ConcurrentHashMap<Integer, Map.Entry<Deque<Integer>, Integer>> deliveredHistory;

    //private final ConcurrentHashMap<String, Boolean> sentMessages; ///TODO not a good practice // guardar mensagens infinitamente -> mudar para sequence number
    //private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates

    private final ConcurrentHashMap<Integer, Deque<Integer>> deliveredHistory;
    private final ConcurrentHashMap<Integer, AtomicInteger> receivedSeqNumberUntil;

    private static SystemMembership systemMembership;
    private final int nodeId;
    private final int port;

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

        this.waitingForACK = new ConcurrentHashMap<>();
        this.higestSeqNumberSent = new ConcurrentHashMap<>();
        this.deliveredHistory = new ConcurrentHashMap<>();
        this.receivedSeqNumberUntil = new ConcurrentHashMap<>();

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

        // Ensure entry exists
        waitingForACK.putIfAbsent(destId, new ArrayDeque<>());
        higestSeqNumberSent.putIfAbsent(destId, new AtomicInteger(0));

        // Get references (modifications affect the actual data)
        Deque<Integer> destWaitingForACK = waitingForACK.get(destId);
        AtomicInteger seqNumber = higestSeqNumberSent.get(destId);

        // Increment sequence number
        int newSeqNum = seqNumber.incrementAndGet();
        destWaitingForACK.add(newSeqNum);

        String messageWithoutMac = nodeId + "|" + newSeqNum + "|" + message;
        PublicKey destPublicKey = this.systemMembership.getPublicKey(destId);

        try {
            String mac = CryptoUtils.generateMAC(privateKey, destPublicKey, messageWithoutMac);
            String authenticatedMsg = messageWithoutMac + "|" + mac;

            AtomicLong timeout = new AtomicLong(1000);

            new Thread(() -> {
                while (destWaitingForACK.contains(newSeqNum)) {
                    try {
                        fairLossLinks.send(destIP, destPort, authenticatedMsg);
                        Thread.sleep(timeout.get()); // Resend every second (adjust as needed)
                        timeout.set((long) (1.5 * timeout.get())); // Flexible timeout increase
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

        if (parts.length != 4) {
            LOGGER.warn("Invalid ACK format: {}", receivedACK);
            return false;
        }

        String receivedWithoutMac = parts[0] + "|" + parts[1] + "|" + parts[2];
        int receivedSrcId = Integer.parseInt(parts[1]);
        int receivedSeqNum = Integer.parseInt(parts[2]);
        String receivedMac = parts[3];

        if (DEBUG_MODE == 1) {
            LOGGER.debug("Extracted components:");
            LOGGER.debug("- Received srcID: {}", receivedSrcId);
            LOGGER.debug("- Received seqNum: {}", receivedSeqNum);
            LOGGER.debug("- Received message without MAC: {}", receivedWithoutMac);
            LOGGER.debug("- Received MAC: {}", receivedMac);
        }

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

    private void sendACK(int destId, int seqNumber) {
        try {
            String ackMessageWithoutMAC = "ACK|" + nodeId + "|" + seqNumber;

            PublicKey destPublicKey = this.systemMembership.getPublicKey(destId);
            String ackMAC = CryptoUtils.generateMAC(privateKey, destPublicKey, ackMessageWithoutMAC);

            String destIP = getIP(destId);
            int destPort = getPort(destId);

            fairLossLinks.send(destIP, destPort, ackMessageWithoutMAC + "|" + ackMAC);

            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sent ACK message: {}", ackMessageWithoutMAC);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while sending ACK", e);
        }
    }

    private void deliverMessage(int srcId, String message) {
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

            if (checkReceivedACK(receivedMessage)) {
                String[] parts = receivedMessage.split("\\|");

                int srcId = Integer.parseInt(parts[1]);
                int seqNumber = Integer.parseInt(parts[2]);

                Deque<Integer> srcWaitingForACK = waitingForACK.get(srcId);
                srcWaitingForACK.remove(seqNumber);
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Stopping to resend the message with seq num: {}", seqNumber);
                }
                return;
            } else {
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Check of the received ACK failed. Ignoring message.");
                }
                return;
            }
        } else {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Received message: {}", receivedMessage);
                LOGGER.debug("Received message: <srcID>|<seq_number>|<receivedPayload>|<receivedMac>");
            }

            if (checkReceivedMessage(receivedMessage)) {
                String[] parts = receivedMessage.split("\\|");

                int srcId = Integer.parseInt(parts[0]);
                int seqNumber = Integer.parseInt(parts[1]);
                String payload = parts[2];

                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Sending ACK to the following message: {}", receivedMessage);
                }
                sendACK(srcId, seqNumber);

                deliveredHistory.putIfAbsent(srcId, new ArrayDeque<>());
                receivedSeqNumberUntil.putIfAbsent(srcId, new AtomicInteger(0));

                if (deliveredHistory.get(srcId).contains(seqNumber) || seqNumber < receivedSeqNumberUntil.get(srcId).get()) {
                    if (DEBUG_MODE == 1) {
                        LOGGER.debug("Message already delivered: {}", receivedMessage);
                    }
                    return;
                }

                if (seqNumber == receivedSeqNumberUntil.get(srcId).get() + 1) {
                    receivedSeqNumberUntil.get(srcId).incrementAndGet();
                    while (deliveredHistory.get(srcId).contains(receivedSeqNumberUntil.get(srcId).get() + 1)) {
                        deliveredHistory.get(srcId).remove(receivedSeqNumberUntil.get(srcId).get() + 1);
                        receivedSeqNumberUntil.get(srcId).incrementAndGet();
                    }
                } else {
                    deliveredHistory.get(srcId).add(seqNumber);
                }

                deliverMessage(srcId, payload);
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
        //sentMessages.remove(messageKey);
    }

    // debug
    public void printSentMessages() {
        System.out.println("Sent Messages: ");
        /*for (String key : sentMessages.keySet()) {
            System.out.println(key);
        }*/
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
    public void close() {
        fairLossLinks.close();
    }

}
