package com.sec.depchain.client;

import java.math.BigInteger;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperledger.besu.datatypes.Address;
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
    private static final int DEBUG_MODE = 0;
    private final Wallet wallet;

    public interface DeliverCallback {
        void deliverAppendResponse(Transaction tx, String transaction, String position);
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
        txRequest.put("transaction", tx.serializeTransactionToJson());
        String jsonMessage = txRequest.toString();

        if (DEBUG_MODE == 1) {
            System.out.println("CLIENT LIBRARY - DEBUG: Sending request: {" + jsonMessage + "} to server { "
                    + systemMembership.getLeaderId() + "}");
        }

        perfectLinks.send(systemMembership.getLeaderId(), jsonMessage);// only send to the leader

    }

    private void onPerfectLinksDeliver(int nodeId, String message) {
        if (DEBUG_MODE == 1) {
            System.out.println(
                    "CLIENT LIBRARY - DEBUG: Received response: {" + message + "} from server {" + nodeId + "}");
        }
        JSONObject jsonResponse = new JSONObject(message);
        String type = jsonResponse.getString("type");
        boolean success = jsonResponse.getBoolean("success");
        String response = jsonResponse.getString("response");
        Transaction tx = deserializeTransactionJson(message);
        tx.setStatus(success);
        tx.setResponse(response);
        // Convert JSON transaction to your Transaction object
        String txHash = tx.computeTxHash();
        int f = systemMembership.getMaximumNumberOfByzantineNodes();
        if ("tx-response".equals(type)) {
            if (processedTransactions.contains(txHash)) {  // Compare by hash
                return;
            }

            // Track responses by txHash instead of Transaction object
            messageResponses.putIfAbsent(txHash, new HashMap<>());
            Map<String, Set<Integer>> positionResponses = messageResponses.get(txHash);

            String position = "default";  // Or extract from JSON if available
            positionResponses.putIfAbsent(position, new HashSet<>());
            Set<Integer> respondingNodes = positionResponses.get(position);
            respondingNodes.add(nodeId);

            if (respondingNodes.size() >=  (f + 1)) {
                if (deliverCallback != null) {
                    processedTransactions.add(txHash);
                    messageResponses.remove(txHash);
                    deliverCallback.deliverAppendResponse(tx, txHash, position);
                }
            }
        }
      
    }

    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    public void close() {
        System.out.println("CLIENT LIBRARY - INFO: Shutting down client resources...");
        if (perfectLinks != null) {
            perfectLinks.close();
        }
        messageResponses.clear();
        processedTransactions.clear();
        System.out.println("CLIENT LIBRARY - INFO: Finished shutitng down client resources...");

    }

    public static Transaction deserializeTransactionJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONObject txJson = json.getJSONObject("transaction");

            // Extract and convert fields
            String senderAddress = txJson.getString("from");
            String toAddress = txJson.getString("to");
            BigInteger value;
            if (txJson.get("amount") instanceof Integer) {
                value = BigInteger.valueOf(txJson.getInt("amount"));
            } else {
                value = new BigInteger(txJson.getString("amount"));
            }
            String data = txJson.getString("data");
            BigInteger nonce;
            if (txJson.get("nonce") instanceof Integer) {
                nonce = BigInteger.valueOf(txJson.getInt("nonce"));
            } else {
                nonce = new BigInteger(txJson.getString("nonce"));
            }
            String signature = txJson.getString("signature"); // Note: Typo in your JSON? Should be "signature"

            // Using current timestamp since it's not in the JSON

            return new Transaction(Address.fromHexString(senderAddress), Address.fromHexString(toAddress), value, data,
                    nonce, signature);

        } catch (Exception e) {
            System.err.println("Failed to deserialize transaction: " + e.getMessage());
            return null;
        }
    }
}
