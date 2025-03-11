package com.sec.depchain.server;

import com.sec.depchain.common.PerfectLinks;

import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.CryptoUtils;

public class ConditionalCollect {
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static Map<Integer, String> messages;
    private static Map<Integer, String> signatures;
    private static boolean collected;

    private static SystemMembership systemMembership;
    private static int nodeId;

    public interface DeliverCallback {
        void deliver(String[] messages);
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public ConditionalCollect(int nodeId, PerfectLinks perfectLinks, SystemMembership systemMembership)
            throws Exception {
        setNodeId(nodeId);
        this.perfectLinks = perfectLinks;
        //this.perfectLinks.setDeliverCallbackCollect(this::onPerfectLinksDeliver);
        
        //TODO changed this in order to compile
        this.perfectLinks.setDeliverCallbackCollect((NodeId, message) -> {
            try {
                onPerfectLinksDeliver(NodeId, message);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        this.messages = new ConcurrentHashMap<>();
        this.signatures = new ConcurrentHashMap<>();

        setSystemMembership(systemMembership);

        this.collected = false;
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            messages.put(processId, "UNDEFINED");
            signatures.put(processId, "UNDEFINED");
        }
    }

    // todo: real signature
    private static String signMessage(String message) {
        return "signature." + message + ".";
    }

    // DONE
    public void input(String message) throws Exception {
        PrivateKey privateKey = this.perfectLinks.getPrivateKey();
        String signature = CryptoUtils.signMessage(privateKey, message); // TODO sign message how can I get the private
                                                                         // key in a safe way?

        String formatted_message = "<SEND:" + message + ":" + signature + ">";

        int leaderId = systemMembership.getLeaderId();
        perfectLinks.send(leaderId, formatted_message);
    }

    // todo: real verification
    private static boolean verifySignature(int senderId, String message, String signature) {
        System.out.println("Verifying Message: " + message + "; Signature: " + signature);
        return true;
    }

    private static String[] getMessageArgs(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            // Split the content by ":"
            String[] parts = content.split(":");

            // Return the parts, including the type and arguments
            return parts;
        }
        return new String[] { "UNKNOWN" };
    }

    private static String getFormattedArray(Map<Integer, String> map) {
        return "[" + String.join(",", map.values()) + "]";
    }

    private static int getNumberOfMessages() {
        int counter = 0;
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            System.out.println("ProcessId: " + processId + "; Message: " + messages.get(processId));
            if (!(messages.get(processId).equals("UNDEFINED")))
                counter++;
            System.out.println("Counter: " + counter);
        }
        return counter;
    }

    // TODO
    private void processSend(int senderId, String sendMessage) throws Exception {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        String[] args = getMessageArgs(sendMessage);
        String message = args[1];
        String signature = args[2];
        if (CryptoUtils.verifySignature(systemMembership.getPublicKey(senderId), message, signature)) {
            messages.put(senderId, message);
            signatures.put(senderId, signature);
        }
        checkAndBrodcastCollectedMessages(sendMessage, senderId);
        /*
         * int N = systemMembership.getNumberOfNodes();
         * int f = systemMembership.getMaximumNumberOfByzantineNodes();
         * if(getNumberOfMessages() >= N - f) { // where is C(messages)
         * String formattedMessages = getFormattedArray(messages);
         * String formattedSignatures = getFormattedArray(signatures);
         * String formattedMessage = "<COLLECTED:" + formattedMessages + ":" +
         * formattedSignatures + ">";
         * for (Integer processId : systemMembership.getMembershipList().keySet()) {
         * System.out.println("Process ID: " + processId);
         * int seq_number = 0; // TODO: get the sequence number
         * perfectLinks.send(processId, formattedMessage, seq_number);
         * }
         * }
         */
    }

    public void checkAndBrodcastCollectedMessages(String sendMessage, int senderId) {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if (getNumberOfMessages() >= N - f && satisfiesConditionC()) {
            String formattedMessages = getFormattedArray(messages);
            String formattedSignatures = getFormattedArray(signatures);
            String formattedMessage = "<COLLECTED:" + formattedMessages + ":" + formattedSignatures + ">";
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                System.out.println("Process ID: " + processId);
                perfectLinks.send(processId, formattedMessage);
            }
        }
    }

    // TODO
    private boolean satisfiesConditionC() {
        return true;
    }

    public static String[] unformatArray(String input) {
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }

        return input.isEmpty() ? new String[0] : input.split("\\s*,\\s*");
    }

    private static boolean verifyAllSignatures(String[] messages, String[] signatures) throws Exception {
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            if (!CryptoUtils.verifySignature(systemMembership.getPublicKey(processId), messages[processId - 1],
                    signatures[processId - 1]))
                return false;
        }
        return true;
    }

    // TODO
    private void processCollected(int senderId, String collectedMessage) throws Exception {
        System.out.println("Received COLLECTED message: " + collectedMessage);

        String[] args = getMessageArgs(collectedMessage);
        String rawCollectedMessages = args[1];
        String rawCollectedSignatures = args[2];
        String[] collectedMessages = unformatArray(rawCollectedMessages);
        String[] collectedSignatures = unformatArray(rawCollectedSignatures);

        System.out.println("Collected Messages:");
        for (String message : collectedMessages) {
            System.out.println("- " + message);
        }

        System.out.println("Collected Signatures:");
        for (String signature : collectedSignatures) {
            System.out.println("- " + signature);
        }

        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if (!collected && collectedMessages.length >= N - f
                && verifyAllSignatures(collectedMessages, collectedSignatures) && satisfiesConditionC()) {// and C(M)
                                                                                                          // where is
                                                                                                          // this
            collected = true;
            System.out.println("ConditionalCollect delivering messages up: " + collectedMessages[0]);
            if (deliverCallback != null) {
                System.out.println("ConditionalCollect delivering messages up: " + collectedMessages[0]);
                deliverCallback.deliver(collectedMessages);
            }
        }
    }

    private String getMessageType(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            String[] parts = content.split(":");

            return parts[0];
        }
        return "UNKNOWN";
    }

    public void onPerfectLinksDeliver(int senderId, String message) throws Exception {

        String messageType = getMessageType(message);
        System.out.println("Message: " + message + ". Message Type: " + messageType);

        switch (messageType) {
            case "SEND":
                processSend(senderId, message);
                break;
            case "COLLECTED":
                processCollected(senderId, message);
                break;
            case "append":
                // not supose to happen only for debug purposes
                System.out.println("Received request in collect: " + message + " from Id: " + senderId);
                break;
            default:
                System.err.println("Error: Unknown message type received from " + senderId + " -> " + message);
        }
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setSystemMembership(SystemMembership systemMembership) {
        this.systemMembership = systemMembership;
    }
}
