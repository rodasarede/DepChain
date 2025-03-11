package com.sec.depchain.server;

import com.sec.depchain.common.PerfectLinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.sec.depchain.common.SystemMembership;


public class ConditionalCollect {
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static Map<Integer, String> messages;
    private static Map<Integer, String> signatures;
    private static boolean collected = false;

    private static SystemMembership systemMembership;
    private static int nodeId;

    public interface DeliverCallback {
        void deliver(String[] messages);
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public ConditionalCollect(int nodeId, PerfectLinks perfectLinks) throws Exception {
        this.nodeId = nodeId;
        this.perfectLinks = perfectLinks;
        this.perfectLinks.setDeliverCallbackCollect(this::onPerfectLinksDeliver);

        this.messages = new ConcurrentHashMap<>();
        this.signatures = new ConcurrentHashMap<>();

        this.systemMembership = new SystemMembership(
                "../common/src/main/java/com/sec/depchain/resources/system_membership.properties");

        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            messages.put(processId, "UNDEFINED");
            signatures.put(processId, "UNDEFINED");
        }
    }

    // todo: real signature
    private static String signMessage(String message){
        return "signature." + message + ".";
    }

    // DONE
    public void input(String message) {
        String signature = signMessage(message);
        String formatted_message = "<SEND:" + message + ":" + signature + ">";
        int leaderId = systemMembership.getLeaderId();
        int seq_number = 0; // TODO: get the sequence number
        perfectLinks.send(leaderId, formatted_message, seq_number);
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
        return new String[] {"UNKNOWN"};
    }

    private static String getFormattedArray(Map<Integer, String> map) {
        return "[" + String.join(",", map.values()) + "]";
    }

    private static int getNumberOfMessages() {
        int counter = 0;
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            System.out.println("ProcessId: " + processId + "; Message: " + messages.get(processId));
            if(messages.get(processId) != "UNDEFINED")
                counter++;
            System.out.println("Counter: " + counter);
        }
        return counter;
    }


    // TODO
    private void processSend(int senderId, String sendMessage) {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println("Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        String[] args = getMessageArgs(sendMessage);
        String message = args[1];
        String signature = args[2];
        if (verifySignature(senderId, message, signature)) {
            messages.put(senderId, message);
            signatures.put(senderId, signature);
        }

        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if(getNumberOfMessages() >= N - f) {
            String formattedMessages = getFormattedArray(messages);
            String formattedSignatures = getFormattedArray(signatures);
            String formattedMessage = "<COLLECTED:" + formattedMessages + ":" + formattedSignatures + ">";
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                System.out.println("Process ID: " + processId);
                int seq_number = 0; // TODO: get the sequence number
                perfectLinks.send(processId, formattedMessage, seq_number);
            }
        }
    }

    public static String[] unformatArray(String input) {
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }

        return input.isEmpty() ? new String[0] : input.split("\\s*,\\s*");
    }

    private static boolean verifyAllSignatures(String[] messages, String[] signatures) {
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            if (!verifySignature(processId, messages[processId - 1], signatures[processId - 1]))
                return false;
        }
        return true;
    }

    // TODO
    private void processCollected(int senderId, String collectedMessage) {
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
        if (collected == false && collectedMessages.length >= N - f && verifyAllSignatures(collectedMessages, collectedSignatures)) {
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

    public void onPerfectLinksDeliver(int senderId, String message) {

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


}
