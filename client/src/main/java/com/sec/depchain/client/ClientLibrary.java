package com.sec.depchain.client;

import java.security.PublicKey;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.util.CryptoUtils;

public class ClientLibrary {
    private DeliverCallback deliverCallback; // Callback function
    private final PerfectLinks perfectLinks;

    // Functional interface for the callback
    public interface DeliverCallback {
        void deliverAppendResponse(boolean result);
    }

    public ClientLibrary(int clientId) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId); // Listen for responses
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
    }

    public void sendAppendRequest(String string) {
        String formattedMessage = "<append:" + string + ">";
        // TODO: Decide do which blockchain nodes to send the request
        perfectLinks.send(-1, formattedMessage); //TODO ensure this reaches the leader
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        System.out.println("Client Library received a message from node with id " + nodeId + " -> " + message);
        
        /*  String[] parts = message.split("\\|");

        String senderId = parts[0]; // The node that sent the message
        String seqNumber = parts[1]; // The sequence number
        String actualMessage = parts[2]; // The message payload (e.g., <append:ola:success>)
        String receivedMac = parts[3]; // The MAC signature
    
        // Reconstruct the original message without the MAC
        String reconstructedMessage = senderId + "|" + seqNumber + "|" + actualMessage;
        // if conditions are met (f+1 confirmations ?)

        //if(CryptoUtils.verifyHMAC(null, null, null))*/
        boolean result = true;

        //PublicKey senderPublicKey = this.systemMembership.getPublicKey(Integer.parseInt(senderId));


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
