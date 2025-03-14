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
        // TODO: Decide do which blockchain nodes to send the request
        for(int nodeId: systemMembership.getMembershipList().keySet())
        {
            perfectLinks.send(nodeId, formattedMessage);
        }
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        System.out.println("Client Library received a message from node with id " + nodeId + " -> " + message);
        int f = systemMembership.getMaximumNumberOfByzantineNodes();

        String[] parts = message.split("\\|");
        String[] elements = parts[2].split("\\:");
        String status = elements[3];
        String position = elements[2];
        String transaction = elements[1];
        String type = elements[0];


        if(type.equals("<append") && status.equals("success>"))
        { 
            messageResponses.putIfAbsent(transaction, new HashSet<>());
            Set<Integer> respondingNodes = messageResponses.get(transaction);
            respondingNodes.add(nodeId);
            //ensure we get f+1 sucess responses to the append request 
            // System.out.println("Responding nodes: " + respondingNodes.size());
            if (respondingNodes.size() >= (f + 1)) {
                if (deliverCallback != null) {
                    messageResponses.remove(transaction); 
                    // System.out.println("Delivering AppendResponse to callback");
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
