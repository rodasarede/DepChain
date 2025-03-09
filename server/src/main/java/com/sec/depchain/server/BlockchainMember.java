package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.List;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.PerfectLinks;


public class BlockchainMember {
    private static int id;
    private static boolean isLeader;
    private static SystemMembership systemMembership;
    private static int PORT;
    private static List<String> blockchain = new ArrayList<>();
    private static PerfectLinks perfectLinks;
    

    public static void main(String[] args) throws Exception {


        if (args.length != 1) {
            System.out.println("Usage: <id>");
            return;
        }
        id = Integer.parseInt(args[0]);
    
        systemMembership = new SystemMembership("../common/src/main/java/com/sec/depchain/resources/system_membership.properties");
        
        if(id == systemMembership.getLeaderId()){
            System.out.println("I am the leader with id: " + id);
            isLeader = true;
        }

        PORT = systemMembership.getMembershipList().get(id).getPort();
        perfectLinks = new PerfectLinks(PORT);
        perfectLinks.setDeliverCallback(BlockchainMember::handleRequest);

        System.out.println("Blockchain Member listening on port " + PORT + "...");
    }

    private static void handleRequest(String srcIP, int srcPort, String message) {
        System.out.println("Received request: " + message + "from " + srcIP + ":" + srcPort);

        if (message.startsWith("<append, ")) {
            String transaction = message.substring(9, message.length() - 1);
            
            // Run consensus
            boolean success = runConsensus(transaction);
            System.out.println(success ? "Transaction confirmed and appended." : "Transaction failed.");

            // Send confirmation response back to client
            String responseMessage = success ? "Transaction confirmed and appended." : "Transaction failed.";
            perfectLinks.send(srcIP, srcPort, responseMessage);
        }
    }

    private static boolean runConsensus(String transaction) {
        System.out.println("Running consensus: INIT -> PROPOSE -> DECIDE");

        if (!initConsensus()) return false;
        if (!proposeConsensus(transaction)) return false;
        return decideConsensus(transaction);
    }

    private static boolean initConsensus() {
        System.out.println("INIT phase successful.");
        return true;
    }

    private static boolean proposeConsensus(String transaction) {
        System.out.println("PROPOSE phase: " + transaction);
        return true;
    }

    private static boolean decideConsensus(String transaction) {
        System.out.println("DECIDE phase: Committing transaction.");
        blockchain.add(transaction);
        return true;
    }
}

