package com.sec.depchain.common;

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

import org.json.JSONObject;

public class PerfectLinks {
    private DeliverCallback deliverCallbackCollect; // Callback to deliver to the class above
    private DeliverCallback deliverCallback; // Callback to deliver to the class above
    private final FairLossLinks fairLossLinks;

    private final ConcurrentHashMap<Integer, Deque<Integer>> waitingForACK;
    private final ConcurrentHashMap<Integer, AtomicInteger> higestSeqNumberSent;
    //private final ConcurrentHashMap<Integer, Map.Entry<Deque<Integer>, Integer>> deliveredHistory;


    private final ConcurrentHashMap<Integer, Deque<Integer>> deliveredHistory;
    private final ConcurrentHashMap<Integer, AtomicInteger> receivedSeqNumberUntil;

    private static SystemMembership systemMembership;
    private final int nodeId;
    private final int port;

    private final PrivateKey privateKey;

    private static final int DEBUG_MODE = 0;

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

    //Constructor for testing
    public PerfectLinks(int nodeId,FairLossLinks fairLossLinks) throws Exception{
        systemMembership = new SystemMembership(
                Constants.PROPERTIES_PATH);
        this.port = getPort(nodeId);
        this.nodeId = nodeId;
        this.systemMembership = systemMembership;
        this.fairLossLinks = fairLossLinks;
        this.waitingForACK = new ConcurrentHashMap<>();
        this.higestSeqNumberSent = new ConcurrentHashMap<>();
        this.deliveredHistory = new ConcurrentHashMap<>();
        this.receivedSeqNumberUntil = new ConcurrentHashMap<>();

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
            System.out.println("PERFECT LINKS - DEBUG: Sending message {"+ message +"} to server {"+ destId +"}");
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

        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("type", "NORMAL");
        jsonMessage.put("nodeId", nodeId);
        jsonMessage.put("seqNum", newSeqNum);
        jsonMessage.put("message", message);

        //String messageWithoutMac = nodeId + "|" + newSeqNum + "|" + message;
        PublicKey destPublicKey = this.systemMembership.getPublicKey(destId);

        try {
            String mac = CryptoUtils.generateMAC(privateKey, destPublicKey, jsonMessage.toString());
            jsonMessage.put("mac", mac);
            //String authenticatedMsg = messageWithoutMac + "|" + mac;

            AtomicLong timeout = new AtomicLong(1000);

            new Thread(() -> {
                while (destWaitingForACK.contains(newSeqNum)) {
                    try {
                        if (DEBUG_MODE == 1) {
                            System.out.println("PERFECT LINKS - DEBUG: Sending message {"+jsonMessage.toString()+"} to server {"+ destId+"}");
                        }
                        fairLossLinks.send(destIP, destPort, jsonMessage.toString());
                        Thread.sleep(timeout.get()); // Resend every second (adjust as needed)
                        timeout.set((long) (1.5 * timeout.get())); // Flexible timeout increase
                    } catch (Exception e) {
                        //Thread.currentThread().interrupt(); // Preserve interruption status
                        System.out.println("PERFECT LINKS - INFO: Thread interrupted while resending message to {"+destId+"}");
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }

    private boolean checkReceivedACK(String receivedACK) {
        /*
        String[] parts = receivedACK.split("\\|");

        if (parts.length != 4) {
            System.out.println("PERFECT LINKS - INFO: Invalid ACK format: {"+receivedACK+"}");
            return false;
        }
        */

        JSONObject jsonReceivedACK = new JSONObject(receivedACK);
        String receivedMac = jsonReceivedACK.getString("mac");
        jsonReceivedACK.remove("mac");
        String receivedWithoutMac = jsonReceivedACK.toString();
        int receivedSrcId = jsonReceivedACK.getInt("nodeId");
        int receivedSeqNum = jsonReceivedACK.getInt("seqNum");
        //String receivedPayload = jsonReceivedMessage.getString("message");
/*
        String receivedWithoutMac = parts[0] + "|" + parts[1] + "|" + parts[2];
        int receivedSrcId = Integer.parseInt(parts[1]);
        int receivedSeqNum = Integer.parseInt(parts[2]);
        String receivedMac = parts[3];
*/
        if (DEBUG_MODE == 1) {
            System.out.println("PERFECT LINKS - DEBUG: Extracted components:");
            System.out.println("PERFECT LINKS - DEBUG: - Received srcID: {"+ receivedSrcId +"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received seqNum: {" + receivedSeqNum +"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received message without MAC: {"+receivedWithoutMac+"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received MAC: {"+ receivedMac+"}");
        }

        PublicKey receivedIdPK = this.systemMembership.getPublicKey(receivedSrcId);

        try {
            if (!CryptoUtils.verifyMAC(privateKey, receivedIdPK, receivedWithoutMac, receivedMac)) {
                System.out.println("PERFECT LINKS - INFO: MAC verification failed for message: {"+receivedWithoutMac+"}");
                return false;
            }
        } catch (Exception e) {
            System.out.println("PERFECT LINKS - ERROR: Exception during MAC verification: " + e);
            return false;
        }
        return true;
    }

    private boolean checkReceivedMessage(String receivedMessage) {
        /*
        String[] parts = receivedMessage.split("\\|");

        if (parts.length != 4) {
            System.out.println("PERFECT LINKS - INFO: Invalid message format: {"+ receivedMessage+"}");
            return false;
        }
         */

        JSONObject jsonReceivedMessage = new JSONObject(receivedMessage);
        String receivedMac = jsonReceivedMessage.getString("mac");
        jsonReceivedMessage.remove("mac");
        String receivedWithoutMac = jsonReceivedMessage.toString();
        int receivedSrcId = jsonReceivedMessage.getInt("nodeId");
        int receivedSeqNum = jsonReceivedMessage.getInt("seqNum");
        String receivedPayload = jsonReceivedMessage.getString("message");


/*
        String receivedWithoutMac = parts[0] + "|" + parts[1] + "|" + parts[2];
        int receivedSrcId = Integer.parseInt(parts[0]);
        int receivedSeqNum = Integer.parseInt(parts[1]);
        String receivedPayload = parts[2];
        String receivedMac = parts[3];
*/
        if (DEBUG_MODE == 1) {
            System.out.println("PERFECT LINKS - DEBUG: Extracted components:");
            System.out.println("PERFECT LINKS - DEBUG: - Received srcID: {"+receivedSrcId+"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received seqNum: {"+receivedSeqNum+"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received message without MAC: {"+jsonReceivedMessage.toString()+"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received Payload: {"+receivedPayload+"}");
            System.out.println("PERFECT LINKS - DEBUG: - Received MAC: {"+receivedMac+"}");
        }

        PublicKey receivedIdPK = this.systemMembership.getPublicKey(receivedSrcId);

        try {
            if (!CryptoUtils.verifyMAC(privateKey, receivedIdPK, receivedWithoutMac, receivedMac)) {
                System.out.println("PERFECT LINKS - INFO: MAC verification failed for message: {"+receivedWithoutMac+"}");
                return false;
            }
        } catch (Exception e) {
            System.out.println("PERFECT LINKS - ERROR: Exception during MAC verification:" + e);
            return false;
        }
        return true;
    }

    private void sendACK(int destId, int seqNumber) {
        try {
            JSONObject jsonACKMessage = new JSONObject();
            jsonACKMessage.put("type", "ACK");
            jsonACKMessage.put("nodeId", nodeId);
            jsonACKMessage.put("seqNum", seqNumber);

            PublicKey destPublicKey = this.systemMembership.getPublicKey(destId);
            String ackMAC = CryptoUtils.generateMAC(privateKey, destPublicKey, jsonACKMessage.toString());
            jsonACKMessage.put("mac", ackMAC);

            String destIP = getIP(destId);
            int destPort = getPort(destId);

            fairLossLinks.send(destIP, destPort, jsonACKMessage.toString());

            if (DEBUG_MODE == 1) {
                System.out.println("PERFECT LINKS - DEBUG: Sent ACK message: {"+jsonACKMessage.toString()+"}");
            }
        } catch (Exception e) {
            System.out.println("PERFECT LINKS - ERROR: Exception while sending ACK: "+ e);
        }
    }

    private void deliverMessage(int srcId, String message) {
        String messageType = getMessageType(message);
        if (DEBUG_MODE == 1) {
            System.out.println("PERFECT LINKS - DEBUG: Message type identified: {"+messageType+"}");
        }

        if (deliverCallbackCollect != null || deliverCallback != null) {

            if (messageType.equals("tx-request") ||
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


        JSONObject receivedJson = new JSONObject(receivedMessage);
        String type = receivedJson.getString("type");

        switch (type) {
            case "ACK": {
                if (DEBUG_MODE == 1) {
                    System.out.println("PERFECT LINKS - DEBUG: Received ACK: {" + receivedMessage + "}");
                    //System.out.println("PERFECT LINKS - DEBUG: Received ACK: ACK|<srcID>|<seq_number>|<receivedPayload>|<receivedMac>");
                }

                if (checkReceivedACK(receivedMessage)) {

                    int srcId = receivedJson.getInt("nodeId");
                    int seqNumber = receivedJson.getInt("seqNum");

                    Deque<Integer> srcWaitingForACK = waitingForACK.get(srcId);
                    srcWaitingForACK.remove(seqNumber);
                    if (DEBUG_MODE == 1) {
                        System.out.println("PERFECT LINKS - DEBUG: Stopping to resend the message with seq num: {" + seqNumber + "}");
                    }
                    return;
                } else {
                    if (DEBUG_MODE == 1) {
                        System.out.println("PERFECT LINKS - DEBUG: Check of the received ACK failed. Ignoring message.");
                    }
                    return;
                }
            }
            case "NORMAL": {
                if (DEBUG_MODE == 1) {
                    System.out.println("PERFECT LINKS - DEBUG: Received message: {" + receivedMessage + "}");
                    //System.out.println("PERFECT LINKS - DEBUG: Received message: <srcID>|<seq_number>|<receivedPayload>|<receivedMac>");
                }

                if (checkReceivedMessage(receivedMessage)) {

                    int srcId = receivedJson.getInt("nodeId");
                    int seqNumber = receivedJson.getInt("seqNum");
                    String payload = receivedJson.getString("message");

                    if (DEBUG_MODE == 1) {
                        System.out.println("PERFECT LINKS - DEBUG: Sending ACK to the following message: {" + receivedMessage + "}");
                    }
                    sendACK(srcId, seqNumber);

                    deliveredHistory.putIfAbsent(srcId, new ArrayDeque<>());
                    receivedSeqNumberUntil.putIfAbsent(srcId, new AtomicInteger(0));

                    if (deliveredHistory.get(srcId).contains(seqNumber) || seqNumber < receivedSeqNumberUntil.get(srcId).get()) {
                        if (DEBUG_MODE == 1) {
                            System.out.println("PERFECT LINKS - DEBUG: Message already delivered: {" + receivedMessage + "}");
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
                        System.out.println("PERFECT LINKS - DEBUG: Check of the received message failed. Ignoring message.");
                    }
                    return;
                }
            }
            default: {
                if (DEBUG_MODE == 1) {
                    System.out.println("PERFECT LINKS - DEBUG: Bad or unkown type: " + type);
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
    public void close(){
        System.out.println("PERFECT LINK - INFO: Shutting down PerfectLinks resources...");
        if(fairLossLinks != null)
        {
            System.out.println("PERFECT LINK - INFO: Closing FairLossLinks...");

         fairLossLinks.close();
         System.out.println("PERFECT LINK - INFO: FairLossLinks closed.");

        }
        waitingForACK.clear();

        higestSeqNumberSent.clear();

        deliveredHistory.clear();

        receivedSeqNumberUntil.clear();
        System.out.println("PERFECT LINK - INFO: Finished perfect links shutdown.");
    }
    public PublicKey getPublicKey(int index){
        return this.systemMembership.getPublicKey(index);
    }
    public DeliverCallback getDeliverCallback() {
        return deliverCallback;
    }
}
