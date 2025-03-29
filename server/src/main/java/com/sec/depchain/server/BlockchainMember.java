package com.sec.depchain.server;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import com.sec.depchain.common.util.KeyLoader;
import com.sec.depchain.common.PerfectLinks;

/**
 * Represents a blockchain member node in the Byzantine fault-tolerant system.
 */
public class BlockchainMember {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockchainMember.class);
    private static int id;
    private static boolean isLeader = false; // Should start as false
    private static SystemMembership systemMembership;
    private static List<String> blockchain = new ArrayList<>();
    private static PerfectLinks perfectLinks;
    private static ByzantineEpochConsensus bep;
    private static Map<Integer, String> clientTransactions = new ConcurrentHashMap<>();
    private static final int DEBUG_MODE = 1; // Set to 1 to enable debug messages

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            LOGGER.error("Usage: <id>");
            return;
        }
        id = Integer.parseInt(args[0]);
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
        isLeader = (id == systemMembership.getLeaderId());

        perfectLinks = new PerfectLinks(id);
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

        bep = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id);
        setBep(bep);

        if (DEBUG_MODE == 1) {
            LOGGER.debug("Initialized with ID {}, Leader: {}", id, isLeader);
        }

        bep.init();
    }

    private static void onPerfectLinksDeliver(int senderId, String message) throws Exception {
        if (DEBUG_MODE == 1) {
            LOGGER.debug("Received message from {} -> {}", senderId, message);
        }

        message = message.substring(1, message.length() - 1);
        String[] elements = message.split(":");
        String messageType = elements[0];

        switch (messageType) {
            case "tx-request":
                String transaction = elements[1];
                Transaction tx = deserializeTransaction(elements);
                if(CryptoUtils.verifySignature(tx)) //TODO if transaction signature is valid what is the next step?
                {
                    clientTransactions.put(senderId, transaction);
                }
                clientTransactions.put(senderId, transaction);
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

    public static void decide(String val) {
        blockchain.add(val);
        int index = blockchain.size();
        LOGGER.info("Transaction {} committed at index {}.", val, index);

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

    public static boolean isLeader() {
        return isLeader;
    }

    public static void setBep(ByzantineEpochConsensus bep) {
        BlockchainMember.bep = bep;
    }
    private static Transaction deserializeTransaction(String tx[])
    {
        String senderAddress = tx[1];
        String toAddress = tx[2];
        BigInteger value = new BigInteger(tx[3]);
        String data = tx[4].equals("empty") ? "" : tx[4];

        String signature = tx[5];
        long nonce = Long.parseLong(tx[6]);
        return new Transaction(senderAddress, toAddress, value, data, nonce, 0, signature);
        //from:to:value:data:signature:nonce
    }
}
