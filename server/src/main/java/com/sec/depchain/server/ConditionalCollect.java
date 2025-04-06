package com.sec.depchain.server;

import com.sec.depchain.common.PerfectLinks;

import java.security.PrivateKey;
import java.util.*;
import java.util.function.Predicate;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;

public class ConditionalCollect {
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static ArrayList<String> messages;
    private static ArrayList<String> signatures;
    private static boolean collected;

    private Predicate<List<String>> outputPredicate;

    private static SystemMembership systemMembership;
    private static int nodeId;

    private static final int DEBUG_MODE = 0;
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

    public void input(JSONObject stateMessage) throws Exception {
        PrivateKey privateKey = this.perfectLinks.getPrivateKey();

        // Convert the state message to string for signing
        JSONObject envelope = new JSONObject();
        envelope.put("type", Constants.MessageType.STATE);
        envelope.put("content", stateMessage); // stateMessage doesn't contain type
        String signature = CryptoUtils.signMessage(privateKey, stateMessage.toString());

        envelope.put("signature", signature); // Fixed typo "signature" (was "signature")

        int leaderId = systemMembership.getLeaderId();

        if (DEBUG_MODE == 1) {
            System.out.println("CONDITIONAL COLLECT: INPUT DELIVERING" + envelope.toString());
        }

        perfectLinks.send(leaderId, envelope.toString());
    }

    private static int getNumberOfMessages() {
        int counter = 0;
        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            // System.out.println("ProcessId: " + processId + "; Message: " +
            // messages.get(processId - 1));
            if (!(messages.get(processId - 1).equals(Constants.UNDEFINED)))
                counter++;
            // System.out.println("Counter: " + counter);
        }
        return counter;
    }

    private void processSend(int senderId, JSONObject sendMessage) throws Exception {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }

        JSONObject content = sendMessage.getJSONObject("content");
        String signature = sendMessage.getString("signature");

        if (CryptoUtils.verifySignature(systemMembership.getPublicKey(senderId), content.toString(), signature)) {
            messages.set(senderId - 1, content.toString());
            signatures.set(senderId - 1, signature);
        }
        if (DEBUG_MODE == 1) {
            System.out.println("CONDITIONAL COLLECT: SEND CHECK " + sendMessage.toString());
        }
        checkAndBrodcastCollectedMessages(sendMessage, senderId);
    }

    public void checkAndBrodcastCollectedMessages(JSONObject sendMessage, int senderId) {
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
            JSONObject collectedMessage = new JSONObject();
            collectedMessage.put("type", Constants.MessageType.COLLECTED);
            collectedMessage.put("messages", new JSONArray(messages));
            collectedMessage.put("signatures", new JSONArray(signatures));
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                // System.out.println("Process ID: " + processId);
                perfectLinks.send(processId, collectedMessage.toString());
            }
        }
    }

    private static boolean verifyAllSignatures(String[] collectedMessages, String[] collectedSignatures)
            throws Exception {
        for (int i = 1; i <= collectedMessages.length; i++) {
            if (collectedMessages[i - 1].equals(Constants.UNDEFINED))
                continue;
            String message = collectedMessages[i - 1];
            String signature = collectedSignatures[i - 1];

            // System.out.println("Verifying signature for message: " + message + " with
            // signature: " + signature);
            if (!CryptoUtils.verifySignature(systemMembership.getPublicKey(i), message, signature)) {
                System.out.println("CONDITIONAL COLLECT: Signature on index " + i + "failed! LEADER CHANGE!");
                return false;
            }
        }
        return true;
    }

    private void processCollected(int senderId, JSONObject collectedMessage) throws Exception {
        // System.out.println("Received COLLECTED message: " + collectedMessage);
        JSONArray collectedMessages = collectedMessage.getJSONArray("messages");
        JSONArray collectedSignatures = collectedMessage.getJSONArray("signatures");

        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();

        List<String> messageList = new ArrayList<>();
        for (int i = 0; i < collectedMessages.length(); i++) {
            messageList.add(collectedMessages.getString(i));
        }

        if (!collected &&
                getNumberOfMessagesDiffFromUNDEFINED(messageList.toArray(new String[0])) >= N - f &&
                verifyAllSignatures(messageList.toArray(new String[0]),
                        collectedSignatures.toString().replaceAll("[\\[\\]\"]", "").split(","))
                &&
                outputPredicate.test(messageList)) {
            collected = true;
            // System.out.println("ConditionalCollect delivering messages up: " +
            // collectedMessages[0]);
            if (deliverCallback != null) {
                // System.out.println("ConditionalCollect delivering messages up: " +
                // collectedMessages[0]);
                deliverCallback.deliver(messageList.toArray(new String[0]));
            }
        }
    }

    public void onPerfectLinksDeliver(int senderId, String message) throws Exception {

        // String messageType = getMessageType(message);
        // System.out.println("Message: " + message + ". Message Type: " + messageType);
        JSONObject messageObj = new JSONObject(message);
        String messageType = messageObj.getString("type");
        switch (messageType) {
            case Constants.MessageType.STATE:
                processSend(senderId, messageObj);
                break;
            case Constants.MessageType.COLLECTED:
                processCollected(senderId, messageObj);
                break;
            case "append-request":
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
}