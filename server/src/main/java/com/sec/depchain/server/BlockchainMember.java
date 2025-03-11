package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.List;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.PerfectLinks;

public class BlockchainMember {
    private static int Id;
    private static boolean isLeader =  false; //should start as false
    private static SystemMembership systemMembership;
    private static int PORT;
    private static List<String> blockchain = new ArrayList<>();
    private static PerfectLinks perfectLinks;

    private static EpochSate state;
    
        public static void main(String[] args) throws Exception {
            if (args.length != 1) {
                System.out.println("Usage: <id>");
                return;
            }
            Id = Integer.parseInt(args[0]);
    
            systemMembership = new SystemMembership(
                    "../common/src/main/java/com/sec/depchain/resources/system_membership.properties");
    
            if (Id == systemMembership.getLeaderId()) {
                System.out.println("I am the leader with id: " + Id);
                isLeader = true;
            }
    
            // PORT = systemMembership.getMembershipList().get(Id).getPort();
            // PORT = getPort(Id);
            perfectLinks = new PerfectLinks(Id);
            perfectLinks.setDeliverCallbackCollect(BlockchainMember::onPerfectLinksDeliver);
            perfectLinks.setDeliverCallback(BlockchainMember::onPerfectLinksDeliver);
    
            // System.out.println("Blockchain Member listening on port " + PORT + "...");



            testConditionalCollect();
        }

    public static void testConditionalCollect() throws Exception {
        // Conditional Collect will be used by byzantine read write epoch
        System.out.println("Starting test of Conditional Collect...");

        ConditionalCollect cc = new ConditionalCollect(Id, perfectLinks); // Pass the necessary Id

        cc.setDeliverCallback((messagesFromCC) -> {
            System.out.println("Received Collected from CC:");
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                System.out.println("Message of nodeId " + processId);
                System.out.println("Message of nodeId " + messagesFromCC[processId - 1] + "\n");
            }
            });

        cc.input("hellofrom" + Id);
    }


    private static void onPerfectLinksDeliver(int senderId, String message) {
            System.out.println("Received request: " + message + " from Id: " + senderId);
            String[] messageElements = PerfectLinks.getMessageElements(message);
            if (messageElements[0].equals("append")) {
                String transaction = messageElements[1];
    
                // Run consensus
                boolean success = runConsensus(transaction);
                String responseMessage = success ? "Transaction confirmed and appended." : "Transaction failed.";
                String formattedMessage = "<append:" + messageElements[1] + ":" + responseMessage + ">";
                // System.out.println(formattedMessage);
    
                // Send confirmation response back to client
                int destId = Integer.parseInt(message.split("\\|")[0]);
                perfectLinks.send(destId, formattedMessage);
            }
        }
        //why static
        private static boolean runConsensus(String transaction) {
            System.out.println("Running consensus: INIT -> PROPOSE -> DECIDE");
    
            if (!initConsensus())
                return false;
            if (!proposeConsensus(transaction))
                return false;
            return decideConsensus(transaction);
        }
        //why static
        private static boolean initConsensus() {
            //initialize 
            setState(new EpochSate(systemMembership.getNumberOfNodes()));
            System.out.println("INIT phase successful.");
            return true;
        }
        //why static?
        private static boolean proposeConsensus(String transaction) {
            //Only the leader
    
            if(isLeader())
            {
                if(getState().getVal() == null) // val == null
                {
                    getState().setVal(transaction); // val:=v;
                    for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
                    {
                        // For each process q , it triggers a Send event to send a [READ] msg doing AuthPerfectLinks
                        //TODO
                        // Processes reply with STATE message containing its local state <valts, val, writeset>:
                        // Leader sends READ to all processes
                        //how to create message???
                        //String message = "<READ|" + getEpochId() + "|" + Id + ">";
                        //estado atual
                        //perfectLinks.send(nodeId, message, seq_number);
                        //seq_number ++;
                    }
            
                }
            }
    
            System.out.println("PROPOSE phase: " + transaction);
            return true;
        }
    
        private static boolean decideConsensus(String transaction) {
            System.out.println("DECIDE phase: Committing transaction.");
            blockchain.add(transaction);
            return true;
        }
        public static boolean isLeader() {
            return isLeader;
        }
        public static EpochSate getState() {
            return state;
    }
    public static void setState(EpochSate state) {
        BlockchainMember.state = state;
    }

}
