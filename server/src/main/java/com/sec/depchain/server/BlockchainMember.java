package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static int clientId;

    //private static EpochSate state;

        public static void main(String[] args) throws Exception {
            if (args.length != 1) {
                System.out.println("Usage: <id>");
                return;
            }
            id = Integer.parseInt(args[0]);
            systemMembership = new SystemMembership(
                    "../common/src/main/java/com/sec/depchain/resources/system_membership.properties");
    
            if (id == systemMembership.getLeaderId()) {
                System.out.println("I am the leader with id: " + id);
                isLeader = true;
            }
    
            // PORT = systemMembership.getMembershipList().get(Id).getPort();
            // PORT = getPort(Id);
            perfectLinks = new PerfectLinks(id);
            perfectLinks.setDeliverCallbackCollect((NodeId, message) -> {
                try {
                    onPerfectLinksDeliver(NodeId, message);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
            perfectLinks.setDeliverCallback((NodeId, message) -> {
                try {
                    onPerfectLinksDeliver(NodeId, message);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });

            bep = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id);

            setBep(bep);

            bep.init();
            // testConditionalCollect();
            
            //Every correct process initializes the conditional collect primitive with this predicate sound(·).
            //cc = new ConditionalCollect(Id, perfectLinks, systemMembership);
            //cc.setPredicate(BlockchainMember::predicateSound);
            

            /* começar o algortimo 
            todos os p começam com o init() incluindo o lider para começarem todos com o mesmo estado

            -apend de um client:
                com ideia q o lider receba o apend mas se nao enviar terá q receber para começar o propose

                o lider começa com o propose


                //decide(valor)*/
        }

    public static void testConditionalCollect() throws Exception {
        // Conditional Collect will be used by byzantine read write epoch
        System.out.println("Starting test of Conditional Collect...");
        //TODO just to compile

        ConditionalCollect cc = new ConditionalCollect(id, perfectLinks, systemMembership, null);

        cc.setDeliverCallback((messagesFromCC) -> {
            System.out.println("Received Collected from CC:");
            for (Integer processId : systemMembership.getMembershipList().keySet()) {
                System.out.println("Message of nodeId " + processId);
                System.out.println("Message of nodeId " + messagesFromCC[processId - 1] + "\n");
            }
        });

        cc.input("hellofrom" + id);
    }


    private static void onPerfectLinksDeliver(int senderId, String message) throws Exception  {
            System.out.println("Received request: " + message + " from Id: " + senderId);
            String[] messageElements = PerfectLinks.getMessageElements(message);
            switch(messageElements[0]) {
                case "append":
                    String transaction = messageElements[1];
                    System.out.print("Proposing " + transaction);
                    setClientId(senderId); //TODO um bocado a fds para testar
                    bep.propose(transaction);
                    // Run consensus
                    
                    //boolean success = runConsensus(transaction);
                    
                    // String responseMessage = success ? "Transaction confirmed and appended." : "Transaction failed.";
                    // String formattedMessage = "<append:" + messageElements[1] + ":" + responseMessage + ">";
                    // // System.out.println(formattedMessage);
        
                    // // Send confirmation response back to client
                    // int destId = Integer.parseInt(message.split("\\|")[0]);
                    // perfectLinks.send(destId, formattedMessage);
                    break;
                case "READ":
                    System.out.println("Received READ message from " + senderId + " with message: " + message);
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
        System.out.println("DECIDE phase: Committing transaction.");
        blockchain.add(val);

        String responseMessage = "<append:" + val + ":success>";

        System.out.println(clientId);
        perfectLinks.send(clientId, responseMessage);
        
    }
    public static boolean isLeader() {
        return isLeader;
    }
   
    public static void setBep(ByzantineEpochConsensus bep) {
        BlockchainMember.bep = bep;
    }
    public static void setClientId(int client_id) {
        BlockchainMember.clientId = client_id;
    }
}
    

