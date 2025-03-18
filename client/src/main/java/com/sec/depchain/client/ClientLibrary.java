package com.sec.depchain.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static SystemMembership systemMembership;
    private final Map<String, Set<Integer>> messageResponses = new HashMap<>();
    private final Set<String> processedTransactions = new HashSet<>();
    private static final int DEBUG_MODE = 1;

    /**
     * Functional interface for the callback used to handle append responses.
     */
    public interface DeliverCallback {
        void deliverAppendResponse(boolean result, String position, String transaction);
    }

    /**
     * Constructs a ClientLibrary instance.
     *
     * @param clientId The ID of the client.
     * @throws Exception If an error occurs during initialization.
     */
    public ClientLibrary(int clientId) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId); // Initialize PerfectLinks for communication
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
    }

    /**
     * Sends an append request to all nodes in the system.
     *
     * @param string The string to be appended.
     */
    public void sendAppendRequest(String string) {
        String formattedMessage = "<append:" + string + ">";

        for (int nodeId : systemMembership.getMembershipList().keySet()) {
            if (DEBUG_MODE == 1) {
                System.out.println("[CLIENT LIBRARY] Sending request: " + formattedMessage + " to server " + nodeId);
            }
            perfectLinks.send(nodeId, formattedMessage);
        }
    }

    /**
     * Handles incoming messages from PerfectLinks.
     *
     * @param nodeId  The ID of the sender node.
     * @param message The received message.
     */
    private void onPerfectLinksDeliver(int nodeId, String message) {
        if (DEBUG_MODE == 1) {
            System.out.println("[CLIENT LIBRARY] Received response: " + message + " from server " + nodeId);
        }

        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        String[] parts = message.split("\\|");
        String[] elements = parts[2].split(":");
        String type = elements[0];
        String transaction = elements[1];
        String position = elements[2];
        String status = elements[3];

        // Process only successful append responses
        if ("<append".equals(type) && "success>".equals(status)) {
            if (processedTransactions.contains(transaction)) {
                return;
            }

            messageResponses.putIfAbsent(transaction, new HashSet<>());
            Set<Integer> respondingNodes = messageResponses.get(transaction);
            respondingNodes.add(nodeId);

            // Ensure f+1 successful responses before delivering to the callback
            if (respondingNodes.size() >= (f + 1)) {
                if (deliverCallback != null) {
                    processedTransactions.add(transaction);
                    messageResponses.remove(transaction);
                    if (DEBUG_MODE == 1)
                        System.out.println("[CLIENT LIBRARY] Delivering response for transaction: " + transaction + " at position: " + position);
                    deliverCallback.deliverAppendResponse(true, transaction, position);
                } else {
                    if (DEBUG_MODE == 1)
                        System.out.println("[CLIENT LIBRARY] No callback set: unable to deliver append response.");
                }
            }
        }
    }

    /**
     * Sets the callback function to handle append responses.
     *
     * @param callback The callback implementation.
     */
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }
}
