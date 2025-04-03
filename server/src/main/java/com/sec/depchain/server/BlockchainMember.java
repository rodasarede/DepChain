package com.sec.depchain.server;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.besu.datatypes.Address;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;
import com.sec.depchain.common.Blockchain;
import com.sec.depchain.common.PerfectLinks;

import org.json.JSONObject;

/**
 * Represents a blockchain member node in the Byzantine fault-tolerant system.
 */
public class BlockchainMember {
    private int id;
    private boolean isLeader = false; // Should start as false
    private static SystemMembership systemMembership;
    private List<String> blockchain = new ArrayList<>();
    private PerfectLinks perfectLinks;
    private ByzantineEpochConsensus bep;
    private Map<Integer, String> clientTransactions = new ConcurrentHashMap<>();
    private static Blockchain blockchain_1;
    private int DEBUG_MODE = 1;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("BLOCKCHAIN MEMBER - ERROR: Usage: <id>");
            return;
        }
        int nodeId = Integer.parseInt(args[0]);
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
        BlockchainMember node = new BlockchainMember(nodeId);
        node.start();
    }
    public BlockchainMember(int id) throws Exception{
        this.id = id;
        this.isLeader = (id == systemMembership.getLeaderId());
        this.blockchain_1 = new Blockchain();
        if (DEBUG_MODE == 1) {
            System.out.println("BLOCKCHAIN MEMBER - DEBUG: Initialized with ID {"+id+"}, Leader: {"+isLeader+"}");
        }
        this.perfectLinks = new PerfectLinks(id);

        perfectLinks.setDeliverCallbackCollect((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                System.out.println("BLOCKCHAIN MEMBER - ERROR: Exception in deliverCallbackCollect: " + e);
            }
        });
        perfectLinks.setDeliverCallback((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                System.out.println("BLOCKCHAIN MEMBER - ERROR: Exception in deliverCallback:" + e);
            }
        });
        
        this.bep = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id, this);
    }
    public void start() throws Exception{
        bep.init();
    }
    //TODO mudei para public
    public void onPerfectLinksDeliver(int senderId, String message) throws Exception {
        if (DEBUG_MODE == 1) {
            System.out.println("BLOCKCHAIN MEMBER - DEBUG: Received message from {"+senderId+"} -> {"+message+"}");
        }

        JSONObject jsonMessage = new JSONObject(message);
        String type = "";
        if (jsonMessage.getString("type") != null) {
            type = jsonMessage.getString("type");
            if (DEBUG_MODE == 1) {
                System.out.println("BLOCKCHAIN MEMBER - DEBUG: message has type:" + type);
            }
        } else {
            System.out.println("BLOCKCHAIN MEMEBER - ERROR: message has no type!");
        }
        String value = "";

/*
        message = message.substring(1, message.length() - 1);
        String[] elements = message.split(":");
        String messageType = elements[0];
*/
        switch (type) {
            case "tx-request":
                // String transaction = message.replace(":", "_");
                //String jsonStringTransaction = jsonMessage.getString("transaction");
                JSONObject jsonTransaction = jsonMessage.getJSONObject("transaction");

                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: jsonStringTransaction: "+jsonTransaction.toString());
                }
                Transaction transaction = deserializeTransaction(jsonTransaction.toString());
                if (transaction.isValid(blockchain_1.getCurrentState())) //TODO if transaction signature is valid what is the next step?
                {
                    clientTransactions.put(senderId, jsonTransaction.toString());
                    // to string just to work
                }
                else {
                    System.out.println("BLOCKCHAIN MEMBER - ERROR: Invalid transaction signature from client {"+senderId+"}: {"+jsonTransaction.toString()+"}");
                    //String responseMessage = "<append-response:" + jsonTransaction.toString() + ":0:fail>";

                    JSONObject jsonResponseMessage = new JSONObject();
                    jsonResponseMessage.put("type", "append-response");
                    jsonResponseMessage.put("status", "fail");
                    jsonResponseMessage.put("transfer", jsonTransaction);
                    // jsonResponseMessage.put("ts", 0);

                    perfectLinks.send(senderId, jsonResponseMessage.toString());
                    break;
                }
                
                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: append: bep.propose(senderId:{"+senderId+"}, value:{"+ jsonTransaction.toString() +"})");
                }
                bep.propose(jsonTransaction.toString());
                break;

            case "READ":
                bep.deliverRead(senderId);
                break;
            case "WRITE":
                value = jsonMessage.getString("value");

                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: WRITE: bep.deliverWrite(senderId:{"+senderId+"}, value:{"+value+"})");
                }
                bep.deliverWrite(senderId, value);
                break;

            case "ACCEPT":
                value = jsonMessage.getString("value");
                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: ACCEPT: bep.deliverAccept(senderId:{"+senderId+"}, value:{"+value+"})");
                }
                bep.deliverAccept(senderId, value);
                break;

            default:
                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: Unknown message type -> {"+message+"}");
                }
                break;
        }
    }

    public void decide(String val) {
        //TODO change the decide logic to add a new block with the val decided( for now a single transaction, list of transactions if we have time)
        // blockchain.add(val);
        // int index = blockchain.size();

        System.out.println("Decided transaction: " + val);
        //String[] transaction = val.split("_");
        Transaction tx = deserializeTransaction(val);
        if(tx.execute(blockchain_1.getCurrentState(), blockchain_1.getLatestBlock().getTransactions(), blockchain_1)){
            System.out.println("Transaction executed successfully");
            System.out.println("Updating world state");
            blockchain_1.updateSimpleWorldState();
        }else{
            //TODO if exection fails send fail message
            System.out.println("Transaction execution failed");
        }  

        int index = blockchain_1.getChainSize();
        blockchain_1.getLatestBlock().printBlockDetails();

        System.out.println("BLOCKCHAIN MEMBER - INFO: Transaction {"+val+"} committed at index {" + index + "}.");
        //blockchain_1.getLatestBlock().printBlockDetails();

        for (Map.Entry<Integer, String> entry : clientTransactions.entrySet()) {
            if (entry.getValue().equals(val)) {
                int clientId = entry.getKey();
                //String responseMessage = "<append-response:" + val + ":" + index + ":success>";

                JSONObject jsonResponseMessage = new JSONObject();
                jsonResponseMessage.put("type", "append-response");
                jsonResponseMessage.put("status", "success");
                jsonResponseMessage.put("transfer", val);
                jsonResponseMessage.put("index", index);

                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: Sending response to client {"+clientId+"} -> {"+jsonResponseMessage.toString()+"}");
                }

                perfectLinks.send(clientId, jsonResponseMessage.toString());
                clientTransactions.remove(clientId);
            }
        }
        bep.init();
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setBep(ByzantineEpochConsensus bep) {
        this.bep = bep;
    }
    public List<String> getBlockchain() {
        return blockchain;
    }
    public Map<Integer, String> getClientTransactions() {
        return clientTransactions;
    }
    public int getId() {
        return id;
    }
    public ByzantineEpochConsensus getBep() {
        return bep;
    }
    public void setPerfectLinks(PerfectLinks perfectLinks) {
        this.perfectLinks = perfectLinks;
    }
    public static void setSystemMembership(SystemMembership membership) {
        systemMembership = membership;
    }
    public PerfectLinks getPerfectLinks() {
        return perfectLinks;
    }
    public void cleanup(){
        this.perfectLinks.close();
    }
    /*
    private static Transaction deserializeTransaction(String tx[])
    {
        String senderAddress = tx[1];
        String toAddress = tx[2];
        BigInteger value = new BigInteger(tx[3]);
        String data = tx[4].equals("empty") ? "" : tx[4];

        String signature = tx[5];
        BigInteger nonce = new BigInteger(tx[6]);
        return new Transaction(Address.fromHexString(senderAddress), Address.fromHexString(toAddress), value, data, nonce, 0, signature);
        //from:to:value:data:signature:nonce
    }
     */

    public Transaction deserializeTransaction(String jsonStringTransaction) {
        try {
            JSONObject jsonTransaction = new JSONObject(jsonStringTransaction);

            // Extract and convert fields
            String senderAddress = jsonTransaction.getString("from");
            String toAddress = jsonTransaction.getString("to");
            BigInteger value;
            if (jsonTransaction.get("amount") instanceof Integer) {
                value = BigInteger.valueOf(jsonTransaction.getInt("amount"));
            } else {
                value = new BigInteger(jsonTransaction.getString("amount"));
            }
            String data = jsonTransaction.getString("data");
            BigInteger nonce;
            if (jsonTransaction.get("nonce") instanceof Integer) {
                nonce = BigInteger.valueOf(jsonTransaction.getInt("nonce"));
            } else {
                nonce = new BigInteger(jsonTransaction.getString("nonce"));
            }
            String signature = jsonTransaction.getString("signature"); // Note: Typo in your JSON? Should be "signature"

            // Using current timestamp since it's not in the JSON

            return new Transaction(Address.fromHexString(senderAddress), Address.fromHexString(toAddress), value, data, nonce, signature);

        } catch (Exception e) {
            System.err.println("Failed to deserialize transaction: " + e.getMessage());
            return null;
        }
    }

    
}
