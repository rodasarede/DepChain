package com.sec.depchain.server;

import com.sec.depchain.common.PerfectLinks;

import java.security.PrivateKey;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;

import org.json.JSONObject;
import org.json.JSONArray;

public class ConditionalCollect {
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static ArrayList<String> messages;
    private static ArrayList<String> signatures;
    private static boolean collected;

    private Predicate<List<String>> outputPredicate;

    private static SystemMembership systemMembership;
    private static int nodeId;
    private static final int DEBUG_MODE = 1;
    private int TAMPER_MESSAGE = 0;

    public interface DeliverCallback {
        void deliver(String[] messages);
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public ConditionalCollect(int nodeId, PerfectLinks perfectLinks, SystemMembership systemMembership,
            Predicate<List<String>> outputPredicate) throws Exception {
        setNodeId(nodeId);
        this.perfectLinks = perfectLinks;
        setSystemMembership(systemMembership);
        // this.perfectLinks.setDeliverCallbackCollect(this::onPerfectLinksDeliver);

    
        this.perfectLinks.setDeliverCallbackCollect((NodeId, message) -> {
            try {
                onPerfectLinksDeliver(NodeId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        this.messages = new ArrayList<>(systemMembership.getNumberOfNodes());
        this.signatures = new ArrayList<>(systemMembership.getNumberOfNodes());
        this.collected = false;

        this.outputPredicate = outputPredicate;

    }

    public void onInit() {
        collected = false;
        messages.clear();
        signatures.clear();

        // Fill with Constants.UNDEFINED
        for (int i = 0; i < systemMembership.getNumberOfNodes(); i++) {
            messages.add(Constants.UNDEFINED);
            signatures.add(Constants.UNDEFINED);
        }

    }

    public void input(String message) throws Exception {

        PrivateKey privateKey = this.perfectLinks.getPrivateKey();

        String signature = CryptoUtils.signMessage(privateKey, message);

        //String formatted_message = "<STATE:" + message + ":" + signature + ">";
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("type", "SEND");
        jsonMessage.put("message", message);
        jsonMessage.put("signature", signature);

        int leaderId = systemMembership.getLeaderId();
        // System.out.println("Sending message: " + formatted_message + " to leader: " + leaderId);
        perfectLinks.send(leaderId, jsonMessage.toString());
    }

    private static String[] getMessageArgs(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            String[] parts = content.split(":");

            return parts;
        }
        return new String[] { Constants.UNKNOWN };
    }

    private static String getFormattedArray(Map<Integer, String> map) {
        return "[" + String.join(",", map.values()) + "]";
    }

    private static int getNumberOfMessages() {
        int counter = 0;
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            // System.out.println("ProcessId: " + processId + "; Message: " + messages.get(processId - 1));
            if (!(messages.get(processId - 1).equals("UNDEFINED")))
                counter++;
            // System.out.println("Counter: " + counter);
        }
        return counter;
    }

    private void processSend(int senderId, String sendMessage) throws Exception {
        if (DEBUG_MODE == 1) {
            System.out.println("CONDITIONAL COLLECT - DEBUG: Received SEND message:" + sendMessage);
        }

        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        /*
        String[] args = getMessageArgs(sendMessage);
        String message = String.join(":", Arrays.copyOfRange(args, 1, args.length - 1));
        String signature = args[args.length - 1];
        */

        JSONObject jsonMessage = new JSONObject(sendMessage);
        String message = jsonMessage.getString("message");
        String signature = jsonMessage.getString("signature");

        if (CryptoUtils.verifySignature(systemMembership.getPublicKey(senderId), message, signature)) {
            if (DEBUG_MODE == 1) {
                System.out.println("CONDITIONAL COLLECT - DEBUG: SEND signature OK");
            }
            messages.set(senderId - 1, message);
            signatures.set(senderId - 1, signature);
        } else {

            if (DEBUG_MODE == 1) {
                System.out.println("CONDITIONAL COLLECT - DEBUG: SEND signature FAILED");
            }
        }
        if (DEBUG_MODE == 1) {
            System.out.println("CONDITIONAL COLLECT - DEBUG: CURRENT MESSAGES AND SIGNATURES:");
            for (int i = 0; i < messages.size(); i++) {
                System.out.println("CONDITIONAL COLLECT - DEBUG: Sender " + (i + 1) + ": Message = " + messages.get(i) + ", Signature = " + signatures.get(i));
            }
        }
        checkAndBrodcastCollectedMessages(sendMessage, senderId);
    }

    public void checkAndBrodcastCollectedMessages(String sendMessage, int senderId) {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }

        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if (getNumberOfMessages() >= N - f && outputPredicate.test(messages)) {

            if (TAMPER_MESSAGE == 1) {
                int index = 0;
                messages.set(index, "1" + messages.get(index).substring(1));
            }


//            String formattedMessage = "<COLLECTED:" + messages + ":" + signatures + ">";
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("type", "COLLECTED");
            jsonMessage.put("messages", messages);
            jsonMessage.put("signatures", signatures);

            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                // System.out.println("Process ID: " + processId);
                perfectLinks.send(processId, jsonMessage.toString());
            }


        }

    }

    public static String[] unformatArray(String input) {
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }

        return input.isEmpty() ? new String[0] : input.split("\\s*,\\s*");
    }

    private static boolean verifyAllSignatures(String[] collectedMessages, String[] collectedSignatures)
            throws Exception {
        for (int i = 1; i <= collectedMessages.length; i++) {
            if (collectedMessages[i - 1].equals(Constants.UNDEFINED))
                continue;
            String message = collectedMessages[i - 1];
            String signature = collectedSignatures[i - 1];

            // System.out.println("Verifying signature for message: " + message + " with signature: " + signature);
            if (!CryptoUtils.verifySignature(systemMembership.getPublicKey(i), message, signature)) {
                System.out.println("CONDITIONAL COLLECT: Signature on index " + i + "failed! LEADER CHANGE!");
                return false;
            }
        }
        return true;
    }

    private void processCollected(int senderId, String collectedMessage) throws Exception {
        if (DEBUG_MODE == 1) {
            System.out.println("CONDITIONAL COLLECT - DEBUG: Received COLLECTED message:" + collectedMessage);
        }
/*
        // System.out.println("Received COLLECTED message: " + collectedMessage);
        String[][] result = splitCollectedMessage(collectedMessage);
        String[] collectedMessages = result[0];
        String[] collectedSignatures = result[1];
*/

        // System.out.println("Collected Messages:");
        // for (String message : collectedMessages) {
        //     System.out.println("- " + message);
        // }

        // System.out.println("Collected Signatures:");
        // for (String signature : collectedSignatures) {
        //     System.out.println("- " + signature);
        // }

        // Parse the JSON message
        JSONObject jsonMessage = new JSONObject(collectedMessage);

        // Extract messages and signatures arrays
        JSONArray collectedMessagesArray = jsonMessage.getJSONArray("messages");
        JSONArray collectedSignaturesArray = jsonMessage.getJSONArray("signatures");

        // Convert JSONArrays to String arrays
        String[] collectedMessages = new String[collectedMessagesArray.length()];
        String[] collectedSignatures = new String[collectedSignaturesArray.length()];

        for (int i = 0; i < collectedMessagesArray.length(); i++) {
            collectedMessages[i] = collectedMessagesArray.getString(i);
            collectedSignatures[i] = collectedSignaturesArray.getString(i);
        }

        // Debugging output
        if (DEBUG_MODE == 1) {
            System.out.println("Extracted Messages: ");
            for (String msg : collectedMessages) {
                System.out.println(msg);
            }
            System.out.println("Extracted Signatures: ");
            for (String sig : collectedSignatures) {
                System.out.println(sig);
            }
        }



        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();

        List<String> messageList = new ArrayList<>(Arrays.asList(collectedMessages));

        if (!collected && getNumberOfMessagesDiffFromUNDEFINED(collectedMessages) >= N - f
                && verifyAllSignatures(collectedMessages, collectedSignatures) && outputPredicate.test(messageList)) {
            collected = true;
            // System.out.println("ConditionalCollect delivering messages up: " + collectedMessages[0]);
            if (deliverCallback != null) {
                //System.out.println("ConditionalCollect delivering messages up: " + collectedMessages[0]);
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

        //String messageType = getMessageType(message);
        // System.out.println("Message: " + message + ". Message Type: " + messageType);

        JSONObject jsonMessage = new JSONObject(message);
        String type = "";
        if (jsonMessage.getString("type") != null) {
            type = jsonMessage.getString("type");
            if (DEBUG_MODE == 1) {
                System.out.println("CONDITIONAL COLLECT - DEBUG: message has type:" + type);
            }
        } else {
            System.out.println("CONDITIONAL COLLECT - ERROR: message has no type!");
        }


        /*
        String receivedMac = jsonReceivedACK.getString("mac");
        jsonReceivedACK.remove("mac");
        String receivedWithoutMac = jsonReceivedACK.toString();
        int receivedSrcId = jsonReceivedACK.getInt("nodeId");
        int receivedSeqNum = jsonReceivedACK.getInt("seqNum");
*/
        switch (type) {
            /*
            case "STATE":
                processSend(senderId, message);
                break;

             */
            case "SEND":
                processSend(senderId, message);
                break;

            case "COLLECTED":
                processCollected(senderId, message);
                break;
            case "tx-request":
                // not supose to happen only for debug purposes
                System.out.println("Received request in collect: " + message + " from Id: " + senderId);
                break;
            default:
                System.err.println("Error: Unknown message type received from " + senderId + " -> " + message);
        }
    }

    public int getNumberOfMessagesDiffFromUNDEFINED(String[] messages) {
        int count = 0;
        for (int i = 0; i < messages.length; i++) {
            if (!messages[i].equals(Constants.UNDEFINED)) {
                count++;
            }
        }
        return count;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public void setSystemMembership(SystemMembership systemMembership) {
        this.systemMembership = systemMembership;
    }

    public static String[][] collectedParser(String input) {
        // Regular expression to extract top-level content inside brackets []
        Pattern outerBracketPattern = Pattern.compile("\\[([^\\[\\]]*)\\]");
        Matcher matcher = outerBracketPattern.matcher(input);

        String[] messages = new String[0]; // Placeholder for messages
        String[] signatures = new String[0]; // Placeholder for signatures

        int index = 0;
        while (matcher.find()) {
            String content = matcher.group(1).trim(); // Extract content inside []

            if (index == 0) {
                // Extract messages, but preserve inner brackets in individual items
                messages = content.split(",\\s*(?=\\d+:|UNDEFINED)"); // Ensure we split correctly
            } else if (index == 1) {
                // Extract signatures
                signatures = content.isEmpty() ? new String[0] : content.split(",\\s*");
            }
            index++;
        }

        return new String[][] { messages, signatures }; // Return as a 2D array
    }

    public static String[][] splitCollectedMessage(String input) {
        // Remove starting "<COLLECTED:" and ending ">"
        if (input.startsWith("<COLLECTED:") && input.endsWith(">")) {
            input = input.substring(11, input.length() - 1);
        }

        // Split the message into two parts using "]:" as delimiter
        String[] parts = input.split("\\]:", 2);

        if (parts.length != 2) {
            return new String[][] { {}, {} }; // Return empty arrays if the format is incorrect
        }

        // Add back the closing bracket "]" to the first part (since it was removed by
        // the split)
        parts[0] += "]";

        // Split messages and signatures into separate arrays
        String[] messages = parts[0].substring(1, parts[0].length() - 1).split(",\\s*"); // Remove outer [] and split
        String[] signatures = parts[1].substring(1, parts[1].length() - 1).split(",\\s*"); // Remove outer [] and split

        return new String[][] { messages, signatures };
    }

}
