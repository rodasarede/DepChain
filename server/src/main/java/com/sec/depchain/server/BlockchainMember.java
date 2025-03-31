package com.sec.depchain.server;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.besu.datatypes.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;
import com.sec.depchain.common.Blockchain;
import com.sec.depchain.common.PerfectLinks;

/**
 * Represents a blockchain member node in the Byzantine fault-tolerant system.
 */
public class BlockchainMember {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainMember.class);
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
            LOGGER.error("Usage: <id>");
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
            LOGGER.debug("Initialized with ID {}, Leader: {}", id, isLeader);
        }
        this.perfectLinks = new PerfectLinks(id);

        perfectLinks.setDeliverCallbackCollect((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                LOGGER.error("Exception in deliverCallbackCollect", e);
            }
        });
        perfectLinks.setDeliverCallback((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                LOGGER.error("Exception in deliverCallback", e);
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
            LOGGER.debug("Received message from {} -> {}", senderId, message);
        }

        message = message.substring(1, message.length() - 1);
        String[] elements = message.split(":");
        String messageType = elements[0];

        switch (messageType) {
            case "tx-request":
                String transaction = message.replace(":", "_");
                Transaction tx = deserializeTransaction(elements);
                if(tx.isValid(blockchain_1.getCurrentState())) //TODO if transaction signature is valid what is the next step?
                {
                    clientTransactions.put(senderId, transaction);
                }
                else{
                    LOGGER.error("Invalid transaction signature from client {}: {}", senderId, transaction);
                    String responseMessage = "<append-response:" + transaction + ":0:fail>";
                    perfectLinks.send(senderId, responseMessage);
                    break;
                }
                
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("append: bep.propose(senderId:{}, value:{})", senderId, elements[1]);
                }
                bep.propose(transaction);
                break;
            case "READ":
                bep.deliverRead(senderId);  
                break;
            case "WRITE":
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("WRITE: bep.deliverWrite(senderId:{}, value:{})", senderId, elements[2]);
                }
                bep.deliverWrite(senderId, elements[2]);
                break;
            case "ACCEPT":
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("ACCEPT: bep.deliverAccept(senderId:{}, value:{})", senderId, elements[2]);
                }
                bep.deliverAccept(senderId, elements[2]);
                break;
            default:
                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Unknown message type -> {}", message);
                }
                break;
        }
    }

    public void decide(String val) {
        //TODO change the decide logic to add a new block with the val decided( for now a single transaction, list of transactions if we have time)
        // blockchain.add(val);
        // int index = blockchain.size();

        System.out.println("Decided transaction: " + val);
        String[] transaction = val.split("_");
        Transaction tx = deserializeTransaction(transaction);
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

        LOGGER.info("Transaction {} committed at index {}.", val, index);
        blockchain_1.getLatestBlock().printBlockDetails();

        for (Map.Entry<Integer, String> entry : clientTransactions.entrySet()) {
            if (entry.getValue().equals(val)) {
                int clientId = entry.getKey();
                String responseMessage = "<append-response:" + val + ":" + index + ":success>";

                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Sending response to client {} -> {}", clientId, responseMessage);
                }

                perfectLinks.send(clientId, responseMessage);
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

    
}
