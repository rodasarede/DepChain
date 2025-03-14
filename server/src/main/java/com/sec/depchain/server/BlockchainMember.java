package com.sec.depchain.server;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.PerfectLinks;

public class BlockchainMember {
    private static int id;
    private static boolean isLeader =  false; //should start as false
    private static SystemMembership systemMembership;
    private static int PORT;
    private static List<String> blockchain = new ArrayList<>();
    private static PerfectLinks perfectLinks;
    private static ByzantineEpochConsensus bep;

   private static Map<Integer, String> clientTransactions = new ConcurrentHashMap<>();

        public static void main(String[] args) throws Exception {
            if (args.length != 1) {
                System.out.println("Usage: <id>");
                return;
            }
            id = Integer.parseInt(args[0]);
            systemMembership = new SystemMembership(
                    Constants.PROPERTIES_PATH);
    
            isLeader = (id == systemMembership.getLeaderId());
    
            perfectLinks = new PerfectLinks(id);
            perfectLinks.setDeliverCallbackCollect((NodeId, message) -> {
                try {
                    onPerfectLinksDeliver(NodeId, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            perfectLinks.setDeliverCallback((NodeId, message) -> {
                try {
                    onPerfectLinksDeliver(NodeId, message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            bep = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id);

            setBep(bep);

            bep.init();
        }

    private static void onPerfectLinksDeliver(int senderId, String message) throws Exception  {
            // System.out.println("Received request: " + message + " from Id: " + senderId);
            String[] messageElements = PerfectLinks.getMessageElements(message);
            switch(messageElements[0]) {
                case "append":
                    String transaction = messageElements[1];
                    clientTransactions.put(senderId, transaction);
                    bep.propose(transaction);
                    break;
                case "READ":
                    // System.out.println("Received READ message from " + senderId + " with message: " + message);
                    bep.deliverRead(senderId);
                    break;
                case "WRITE":
                    bep.deliverWrite(senderId, messageElements[2]);
                    break;
                case "ACCEPT":
                    bep.deliverAccept(senderId, messageElements[2]);
                    break;
                default: 
                    break;
                
            }
        }
    public static void decide(String val){
        blockchain.add(val);
        int index = blockchain.size() ;
        System.out.println("DECIDE PHASE: Transaction " + val + " committed at index " + index + ".");
        

        for (Map.Entry<Integer, String> entry : clientTransactions.entrySet()) {
            if (entry.getValue().equals(val)) {
                int clientId = entry.getKey();
                String responseMessage = "<append:" + val + ":" + index +  ":success>";
    
                // System.out.println("Sending response to client: " + clientId);
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
    

