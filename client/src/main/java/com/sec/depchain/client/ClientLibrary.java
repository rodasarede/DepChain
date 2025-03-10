package com.sec.depchain.client;

import com.sec.depchain.common.PerfectLinks;

public class ClientLibrary {
    private DeliverCallback deliverCallback; // Callback function
    private final PerfectLinks perfectLinks;
    private static int seqNumber = 1;

    // Functional interface for the callback
    public interface DeliverCallback {
        void deliverAppendResponse(boolean result);
    }

    public ClientLibrary(int clientId) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId); // Listen for responses
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
    }

    // Setter for the callback
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public void sendAppendRequest(String string) {
        String formattedMessage = "<append:" + seqNumber + ":" + string + ">";
        perfectLinks.send(-1, formattedMessage, seqNumber);
        seqNumber++;
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        System.out.println("Client Library received a message from node with id " + nodeId + " -> " + message);

        // if conditions are met (f+1 confirmations ?)
        boolean result = true;

        if (deliverCallback != null) {
            deliverCallback.deliverAppendResponse(result); // Invoke the callback
        } else {
            System.out.println("No callback set: could not deliver AppendResponse");
        }
    }
}
