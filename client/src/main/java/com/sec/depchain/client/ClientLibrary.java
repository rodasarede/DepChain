package com.sec.depchain.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
    private DeliverCallback deliverCallback; // Callback function
    private final PerfectLinks perfectLinks;
    private static SystemMembership systemMembership;
    private Map<String, Set<Integer>> messageResponses = new HashMap<>();
    private Set<String> processedTransactions = new HashSet<>();
    private int DEBUG_MODE = 0;

    // Functional interface for the callback
    public interface DeliverCallback {
        void deliverAppendResponse(boolean result, String position, String transaction);
    }

    public ClientLibrary(int clientId) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId); // Listen for responses
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
        this.systemMembership = new SystemMembership(
                Constants.PROPERTIES_PATH);
        
    }

    public void sendAppendRequest(String string) {
        String formattedMessage = "<append:" + string + ">";
        for(int nodeId: systemMembership.getMembershipList().keySet()) {
            if (DEBUG_MODE == 1) {
                System.out.println("CLIENT LIBRARY: Sending " + formattedMessage + " to server " + nodeId + ".");
            }
            if(nodeId == 1){

                perfectLinks.send(nodeId, formattedMessage);
            }
        }
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        if (DEBUG_MODE == 1) {
            System.out.println("CLIENT LIBRARY: Received " + message + "from server " + nodeId);
        }
        int f = systemMembership.getMaximumNumberOfByzantineNodes();

        String[] parts = message.split("\\|");
        String[] elements = parts[2].split("\\:");
        String status = elements[3];
        String position = elements[2];
        String transaction = elements[1];
        String type = elements[0];


        if(type.equals("<append") && status.equals("success>"))
        { 
            if(processedTransactions.contains(transaction)){
                return;
            }
            messageResponses.putIfAbsent(transaction, new HashSet<>());
            Set<Integer> respondingNodes = messageResponses.get(transaction);
            respondingNodes.add(nodeId);
            //ensure we get f+1 sucess responses to the append request 
            // System.out.println("Responding nodes: " + respondingNodes.size());
            if (respondingNodes.size() >= (f + 1)) {
                if (deliverCallback != null) {
                    // System.out.println("Delivering AppendResponse to callback");~
                    processedTransactions.add(transaction);
                    messageResponses.remove(transaction);
                    System.out.println("CLIENT LIBRARY: Delivering " + transaction + " to CLIENT APP");
                    deliverCallback.deliverAppendResponse(true, transaction , position);
                } else {
                    System.out.println("No callback set: could not deliver AppendResponse");
                }
            }
        }
        
    }

    // Setter for the callback
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }
}
