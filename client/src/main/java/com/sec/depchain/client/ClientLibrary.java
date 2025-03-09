package com.sec.depchain.client;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.PerfectLinks;

public class ClientLibrary {
    private final PerfectLinks perfectLinks;
    private final SystemMembership systemMembership;
    private static int seqNumber = 1;
    private MessageCallback callback; // Callback function

    public ClientLibrary(int clientId) throws Exception {
        systemMembership = new SystemMembership("../common/src/main/java/com/sec/depchain/resources/system_membership.properties");
        int clientPort = 5001;
        this.perfectLinks = new PerfectLinks(clientPort, clientId); // Listen for responses
        this.perfectLinks.setDeliverCallback(this::onMessageReceived);
    }

    // Setter for the callback
    public void setCallback(MessageCallback callback) {
        this.callback = callback;
    }

    public void sendAppendRequest(int clientId, String string) {
        String formattedMessage = "<append:" + seqNumber + ":" + string + ">";

        // Send the request to the leader (for now) TODO: decide to which nodes to we send the request
        String leaderIP = systemMembership.getMembershipList().get(systemMembership.getLeaderId()).getAddress();
        int leaderPort = systemMembership.getMembershipList().get(systemMembership.getLeaderId()).getPort();
        int leaderId = systemMembership.getLeaderId();
        perfectLinks.send(leaderIP, leaderPort, leaderId, formattedMessage, seqNumber);
        seqNumber++;
    }

    private void onMessageReceived(String srcIP, int srcPort, String message) {
        System.out.println("Client Library received from " + srcIP + ":" + srcPort + " -> " + message);
        System.out.println("Sending confirmation to Client Application");
        String formattedMessage = "Your string <string> was appended to the blockchain: " + message;
        if (callback != null) {
            callback.onMessage(formattedMessage); // Invoke the callback
        } else {
            System.out.println("No callback set: " + formattedMessage);
        }
    }

    // Functional interface for the callback
    public interface MessageCallback {
        void onMessage(String message);
    }
}
