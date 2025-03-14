package com.sec.depchain.client;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
    private DeliverCallback deliverCallback; // Callback function
    private final PerfectLinks perfectLinks;
    private static SystemMembership systemMembership;
    // Functional interface for the callback
    public interface DeliverCallback {
        void deliverAppendResponse(boolean result);
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
        
        boolean result = true;

        if (deliverCallback != null) {
            deliverCallback.deliverAppendResponse(result); // Invoke the callback
        } else {
            System.out.println("No callback set: could not deliver AppendResponse");
        }
    }

    // Setter for the callback
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }
}
