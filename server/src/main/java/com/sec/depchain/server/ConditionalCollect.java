package com.sec.depchain.server;

import com.sec.depchain.common.PerfectLinks;

import java.security.PrivateKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
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

    //DONE

    /*upon event ⟨ cc, Init ⟩ do
    messages := [UNDEFINED]^N;
    Σ := [⊥]^N;
    collected := FALSE; */
    public ConditionalCollect(int nodeId, PerfectLinks perfectLinks, SystemMembership systemMembership)
            throws Exception {
        setNodeId(nodeId);
        this.perfectLinks = perfectLinks;
        setSystemMembership(systemMembership);
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
        this.collected = false; 

        for (Integer processId : systemMembership.getMembershipList().keySet()) {
            messages.put(processId, Constants.UNDEFINED);
            signatures.put(processId, ""); // signatures should start with //TODO null not UNDEFINED
        }
    }

    // DONE
    /*upon event ⟨ cc, Input | m ⟩ do
    σ := sign(self, cc || self || INPUT || m);
    String signingData = "cc||" + this.nodeId + "||INPUT||" + message;
    String signature = CryptoUtils.signMessage(privateKey, signingData);
    String metadata = nodeId + "|ConditionalCollect|INPUT|" + message;
    trigger ⟨ al, Send | ℓ, [SEND, m, σ] ⟩; */
    public void input(String message) throws Exception {
        //TODO what should be instead of cc //ROUND?
        //String message_to_sign = "<cc|" + nodeId + ":INPUT:" + message + ">";    //σ := sign(self, cc || self || INPUT || m);

        // String message_to_sign = "INPUT:" + message + ">";     //σ := sign(self, cc || self || INPUT || m);
        PrivateKey privateKey = this.perfectLinks.getPrivateKey();

        String signature = CryptoUtils.signMessage(privateKey, message); 
        //String signature = CryptoUtils.signWithPrivateKey(message, nodeId);

        String formatted_message = "<SEND:" + message + ":" + signature + ">";
        int leaderId = systemMembership.getLeaderId();
        System.out.println("Sending message: " + formatted_message + " to leader: " + leaderId);
        perfectLinks.send(leaderId, formatted_message);
    }


    private static String[] getMessageArgs(String message) {
        if (message.startsWith("<") && message.endsWith(">")) {
            String content = message.substring(1, message.length() - 1);

            String[] parts = content.split(":");

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
    //(only for the leader)
    /*upon event ⟨ al, Deliver | p, [SEND, m, σ] ⟩ do
    if verifysig(p, cc || p || INPUT || m, σ) then //p processID; cc->; INPUT; m -> message; sign
        messages[p] := m;
        Σ[p] := σ; */
    private void processSend(int senderId, String sendMessage) throws Exception {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        String[] args = getMessageArgs(sendMessage);
        String message = String.join(":", Arrays.copyOfRange(args, 1, args.length - 1));
        String signature = args[args.length - 1];
        if (CryptoUtils.verifySignature(systemMembership.getPublicKey(senderId), message, signature)) {
            messages.put(senderId, message);
            signatures.put(senderId, signature);
        }
        checkAndBrodcastCollectedMessages(sendMessage, senderId);
    }


    /*upon #(messages) ≥ N − f ∧ C(messages) do
    forall q ∈ Π do
        trigger ⟨ al, Send | q, [COLLECTED, messages, Σ] ⟩;
    messages := [UNDEFINED]^N;
    Σ := [⊥]^N; */
    public void checkAndBrodcastCollectedMessages(String sendMessage, int senderId) {
        if (nodeId != systemMembership.getLeaderId()) {
            System.out.println(
                    "Unexpected: non leader received SEND message: " + sendMessage + " from node: " + senderId);
            return;
        }
        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if (getNumberOfMessages() >= N - f && outputPredicate()) {
            String formattedMessages = getFormattedArray(messages);
            String formattedSignatures = getFormattedArray(signatures);
            String formattedMessage = "<COLLECTED:" + formattedMessages + ":" + formattedSignatures + ">";
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                System.out.println("Process ID: " + processId);
                perfectLinks.send(processId, formattedMessage);
            }
        }
        // // reset the state
        // messages.clear();
        // signatures.clear();
        // for (Integer processId : systemMembership.getMembershipList().keySet()) {
        //     messages.put(processId, "UNDEFINED");
        //     signatures.put(processId, "⊥");
        // }
        }

    // TODO
    private boolean outputPredicate() { 
        // predicate that determines whether the collected messages satisfy certain requirements before the protocol proceeds.
        //A quorum agrees on the same value (Byzantine fault tolerance)?
        // Majority Agreement?
        return true;
    }

    public static String[] unformatArray(String input) {
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1);
        }

        return input.isEmpty() ? new String[0] : input.split("\\s*,\\s*");
    }

    private static boolean verifyAllSignatures(String[] collectedMessages, String[] collectedSignatures) throws Exception {
        for (int i = 1; i <= collectedMessages.length ; i++) {
            if (collectedMessages[i].equals(Constants.UNDEFINED)) continue;
            String message = collectedMessages[i];
            String signature = collectedSignatures[i];
            // String signingData = "cc||" + i + "||INPUT||" + message;
            System.out.println("Verifying signature for message: " + message + " with signature: " + signature);
            if (!CryptoUtils.verifySignature(systemMembership.getPublicKey(i), message, signature)) {
                return false;
            }
        }
        return true;
    }

    // TODO

    /*upon event ⟨ al, Deliver | ℓ, [COLLECTED, M, Σ] ⟩ do
    if collected = FALSE ∧ #(M) ≥ N - f ∧ C(M) ∧
       (forall p ∈ Π such that M[p] ≠ UNDEFINED, it holds
        verifysig(p, cc || p || INPUT || M[p], Σ[p])) then
        collected := TRUE;
        trigger ⟨ cc, Collected | M ⟩;
 */
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
        if (!collected && getNumberOfMessagesDiffFromUNDEFINED(collectedMessages) >= N - f
                && verifyAllSignatures(collectedMessages, collectedSignatures) && outputPredicate()) {// and C(M)
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
    public int getNumberOfMessagesDiffFromUNDEFINED(String[] messages)
    {
        int count = 0;
        for(int i = 0; i < messages.length ; i++)
        {
            if(!messages[i].equals(Constants.UNDEFINED)){
                count ++;
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
