package com.sec.depchain.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLibrary.class);
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static SystemMembership systemMembership;
    private Map<String, Map<String, Set<Integer>>> messageResponses = new HashMap<>();
    private final Set<String> processedTransactions = new HashSet<>();
    private static final int DEBUG_MODE = 1;

    public interface DeliverCallback {
        void deliverAppendResponse(boolean result, String transaction, String position);
    }

    public ClientLibrary(int clientId) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId);
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
    }

    public void sendAppendRequest(String string) {
        String formattedMessage = "<append-request:" + string + ">";

        for (int nodeId : systemMembership.getMembershipList().keySet()) {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sending request: {} to server {}", formattedMessage, nodeId);
            }
            perfectLinks.send(nodeId, formattedMessage);
        }
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        if (DEBUG_MODE == 1) {
            LOGGER.debug("Received response: {} from server {}", message, nodeId);
        }

        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        String[] elements = message.split(":");
        String type = elements[0];
        String transaction = elements[1];
        String position = elements[2];
        String status = elements[3];

        if ("<append-response".equals(type) && "success>".equals(status)) {
            if (processedTransactions.contains(transaction)) {
                return;
            }

            messageResponses.putIfAbsent(transaction, new HashMap<>());

            Map<String, Set<Integer>> positionResponses = messageResponses.get(transaction);

            positionResponses.putIfAbsent(position, new HashSet<>());
            Set<Integer> respondingNodes = positionResponses.get(position);
            respondingNodes.add(nodeId);

            if (respondingNodes.size() >= (f + 1)) {
                if (deliverCallback != null) {
                    processedTransactions.add(transaction);
                    messageResponses.remove(transaction);
                    if (DEBUG_MODE == 1) {
                        LOGGER.debug("Delivering response for transaction: {} at position: {}", transaction, position);
                    }
                    deliverCallback.deliverAppendResponse(true, transaction, position);
                } else {
                    if (DEBUG_MODE == 1) {
                        LOGGER.debug("No callback set: unable to deliver append response.");
                    }
                }
            }
        }
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }
    public void close()
    {
        LOGGER.info("Shutting down client resources...");
        if (perfectLinks != null){
        perfectLinks.close();
    }
    messageResponses.clear();
    processedTransactions.clear();
    LOGGER.info("Finished shutitng down client resources...");

    }
}
