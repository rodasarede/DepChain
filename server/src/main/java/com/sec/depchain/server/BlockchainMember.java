package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.PerfectLinks;

/**
 * Represents a blockchain member node in the Byzantine fault-tolerant system.
 */
public class BlockchainMember {
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
            System.out.println("[ERROR] BLOCKCHAIN MEMBER: Usage: <id>");
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
                e.printStackTrace();
            }
        });
        perfectLinks.setDeliverCallback((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        bep = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id);
        setBep(bep);

        if (DEBUG_MODE == 1) {
            System.out.println("[DEBUG] BLOCKCHAIN MEMBER: Initialized with ID " + id + ", Leader: " + isLeader);
        }

        bep.init();
    }

    private static void onPerfectLinksDeliver(int senderId, String message) throws Exception {
        if (DEBUG_MODE == 1) {
            System.out.println("[DEBUG] BLOCKCHAIN MEMBER: Received message from " + senderId + " -> " + message);
        }

        String[] messageElements = PerfectLinks.getMessageElements(message);
        switch (messageElements[0]) {
            case "append":
                String transaction = messageElements[1];
                clientTransactions.put(senderId, transaction);
                bep.propose(transaction);
                break;
            case "READ":
                bep.deliverRead(senderId);
                break;
            case "WRITE":
                bep.deliverWrite(senderId, messageElements[2]);
                break;
            case "ACCEPT":
                bep.deliverAccept(senderId, messageElements[2]);
                break;
            default:
                if (DEBUG_MODE == 1) {
                    System.out.println("[DEBUG] BLOCKCHAIN MEMBER: Unknown message type -> " + message);
                }
                break;
        }
    }

    public static void decide(String val) {
        blockchain.add(val);
        int index = blockchain.size();
        System.out.println("[INFO] BLOCKCHAIN MEMBER: Transaction " + val + " committed at index " + index + ".");

        for (Map.Entry<Integer, String> entry : clientTransactions.entrySet()) {
            if (entry.getValue().equals(val)) {
                int clientId = entry.getKey();
                String responseMessage = "<append:" + val + ":" + index + ":success>";

                if (DEBUG_MODE == 1) {
                    System.out.println("[DEBUG] BLOCKCHAIN MEMBER: Sending response to client " + clientId + " -> " + responseMessage);
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
}
