package com.sec.depchain.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLibrary.class);
    private DeliverCallback deliverCallback;
    private final PerfectLinks perfectLinks;
    private static SystemMembership systemMembership;
    private Map<String, Map<String, Set<Integer>>> messageResponses = new HashMap<>();
    private final Set<String> processedTransactions = new HashSet<>();
    private static final int DEBUG_MODE = 1;
    private final Wallet wallet;

    public interface DeliverCallback {
        void deliverAppendResponse(boolean result, String transaction, String position);
    }

    public ClientLibrary(int clientId, Wallet wallet) throws Exception {
        this.perfectLinks = new PerfectLinks(clientId);
        this.perfectLinks.setDeliverCallback(this::onPerfectLinksDeliver);
        this.wallet = wallet;
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
    }

    public void sendTransferRequest(Transaction tx) {
        
        String formattedMessage = "<tx-request:" + serializeTransaction(tx) + ">";

        perfectLinks.send(systemMembership.getLeaderId(), formattedMessage); //only send to the leader
        /*for (int nodeId : systemMembership.getMembershipList().keySet()) {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sending request: {} to server {}", formattedMessage, nodeId);
            }
            perfectLinks.send(nodeId, formattedMessage);
        }*/
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
        }else if ("<append-response".equals(type) && "fail>".equals(status)) {
            //TODO send to client that transaction failed
        }
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }
    public void close() {
            LOGGER.info("Shutting down client resources...");
            if (perfectLinks != null){
            perfectLinks.close();
        }
        messageResponses.clear();
        processedTransactions.clear();
        LOGGER.info("Finished shutitng down client resources...");

    }
    private String serializeTransaction(Transaction tx) {
        String data = tx.getData().equals("") ? "empty" : tx.getData(); // how to deal with empty data? //TODO
        // Using colon separator with field prefixes
        return String.format( //from:to:value:data:signature:nonce
            "%s:%s:%s:%s:%s:%d",
            tx.getFrom(),
            tx.getTo(),
            tx.getValue().toString(),
            data,
            tx.getSignature(),
            tx.getNonce()
        );
    }
}
