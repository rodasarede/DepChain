package com.sec.depchain.client;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;

public class ClientLibrary {
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
        
        JSONObject txRequest = new JSONObject();
        txRequest.put("type", "tx-request");
        txRequest.put("transaction", serializeTransactionToJson(tx));
        String jsonMessage = txRequest.toString();

        
            if (DEBUG_MODE == 1) {
                System.out.println("CLIENT LIBRARY - DEBUG: Sending request: {"+ jsonMessage +"} to server { "+ systemMembership.getLeaderId()+ "}");
            }
            for(int nodeId : systemMembership.getMembershipList().keySet()){
                if (nodeId == systemMembership.getLeaderId()) {
                    perfectLinks.send(nodeId, jsonMessage);
                }
            } 
        
    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        if (DEBUG_MODE == 1) {
            System.out.println("CLIENT LIBRARY - DEBUG: Received response: {"+ message +"} from server {"+ nodeId +"}");
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
                        System.out.println("CLIENT LIBRARY - DEBUG: Delivering response for transaction: {"+ transaction +"} at position: {" + position + "}");
                    }
                    deliverCallback.deliverAppendResponse(true, transaction, position);
                } else {
                    if (DEBUG_MODE == 1) {
                        System.out.println("CLIENT LIBRARY - DEBUG: No callback set: unable to deliver append response.");
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
            System.out.println("CLIENT LIBRARY - INFO: Shutting down client resources...");
            if (perfectLinks != null){
            perfectLinks.close();
        }
        messageResponses.clear();
        processedTransactions.clear();
        System.out.println("CLIENT LIBRARY - INFO: Finished shutitng down client resources...");

    }
    private JSONObject serializeTransactionToJson(Transaction tx) {
        JSONObject jsonTx = new JSONObject();
        jsonTx.put("from", tx.getFrom());
        jsonTx.put("to", tx.getTo());
        jsonTx.put("amount", tx.getValue());
        jsonTx.put("data", tx.getData());
        jsonTx.put("signature", tx.getSignature());
        jsonTx.put("nonce", tx.getNonce());
        // add any other relevant fields
        return jsonTx;
    }
}
