package com.sec.depchain.server;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hyperledger.besu.datatypes.Address;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sec.depchain.common.Block;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;

public class Formatter {

    public static String formatAcceptMessage(Block v, long ets) {
        JSONObject message = new JSONObject();
        message.put("type", Constants.MessageType.ACCEPT);
        message.put("ets", ets);
        message.put("value", blockToJson(v)); // Serialize block
        return message.toString();
    }

    public static String formatWriteMessage(Block block, long ets) {
        JSONObject message = new JSONObject();
        message.put("type", Constants.MessageType.WRITE);
        message.put("ets", ets);
        message.put("value", blockToJson(block)); // Serialize block
        return message.toString();
    }

    public static JSONObject formatStateMessage(long ets, TSvaluePairBlock valtsVal, Set<TSvaluePairBlock> writeSet) {
        JSONObject message = new JSONObject();
        message.put("ets", ets);

        // Handle Block value
        JSONObject valuePair = new JSONObject();
        if (valtsVal.getVal() != null) {
            Block block = (Block) valtsVal.getVal();
            valuePair.put("value", blockToJson(block)); // Use block serializer
        } else {
            valuePair.put("value", JSONObject.NULL);
        }
        valuePair.put("timestamp", valtsVal.getTimestamp());
        message.put("value_pair", valuePair);

        // Serialize writeSet (containing Blocks)
        JSONArray writeSetArray = new JSONArray();
        for (TSvaluePairBlock pair : writeSet) {
            JSONObject pairObj = new JSONObject();
            if (pair.getVal() != null) {
                Block block = (Block) pair.getVal();
                pairObj.put("value", blockToJson(block));
            } else {
                pairObj.put("value", JSONObject.NULL);
            }
            pairObj.put("timestamp", pair.getTimestamp());
            writeSetArray.put(pairObj);
        }
        message.put("write_set", writeSetArray);

        return message;
    }

    public static Block jsonToBlock(JSONObject blockJson) throws JSONException {
        // Extract basic block fields
        String hash = blockJson.getString("hash");
        Object valueObj = blockJson.get("previousHash");

        String previousHash = (valueObj == JSONObject.NULL || valueObj == null) ? null : valueObj.toString();
        int height = blockJson.getInt("height");

        // Deserialize transactions
        JSONArray txArray = blockJson.getJSONArray("transactions");
        List<Transaction> transactions = new ArrayList<>();

        for (int i = 0; i < txArray.length(); i++) {
            JSONObject txJson = txArray.getJSONObject(i);
            Transaction tx = deserializeTransactionJson(txJson.toString());
            if (tx == null) {
                throw new JSONException("Failed to deserialize transaction at index " + i);
            }
            transactions.add(tx);
        }

        // Reconstruct the block
        Block block = new Block(previousHash, transactions, height);

        // Verify hash matches (security check)
        if (!block.getBlockHash().equals(hash)) {
            throw new IllegalStateException("Block hash mismatch - possible tampering");
        }

        return block;
    }

    public static JSONObject blockToJson(Block block) {
        JSONObject blockJson = new JSONObject();

        // Block metadata
        blockJson.put("hash", block.getBlockHash());
        blockJson.put("previousHash",
                block.getPreviousBlockHash() != null ? block.getPreviousBlockHash() : JSONObject.NULL);
        blockJson.put("height", block.getHeight());

        // Serialize transactions
        JSONArray transactionsArray = new JSONArray();
        for (Transaction tx : block.getTransactions()) {
            transactionsArray.put(serializeTransactionToJson(tx));
        }
        blockJson.put("transactions", transactionsArray);
        return blockJson;

    }

    private static JSONObject serializeTransactionToJson(Transaction tx) {
        JSONObject jsonTxWrapper = new JSONObject(); // Outer wrapper
        JSONObject jsonTx = new JSONObject(); // Inner transaction object

        // Populate the inner transaction object
        jsonTx.put("from", tx.getFrom());
        jsonTx.put("to", tx.getTo());
        jsonTx.put("amount", tx.getValue());
        jsonTx.put("data", tx.getData() != null ? tx.getData() : JSONObject.NULL);
        jsonTx.put("signature", tx.getSignature());
        jsonTx.put("nonce", tx.getNonce());

        // Add the inner transaction to the wrapper
        jsonTxWrapper.put("transaction", jsonTx);

        return jsonTxWrapper; // Returns { "transaction": { ... } }
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
