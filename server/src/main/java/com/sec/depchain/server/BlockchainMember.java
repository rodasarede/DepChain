package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private static Map<Integer, String> states; //TODO how can I save the states for the collected

    private static ConditionalCollect cc;
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

            testConditionalCollect();
        }

    public static void testConditionalCollect() throws Exception {
        // Conditional Collect will be used by byzantine read write epoch
        System.out.println("Starting test of Conditional Collect...");

        ConditionalCollect cc = new ConditionalCollect(Id, perfectLinks, systemMembership); // Pass the necessary Id

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

        private static boolean runConsensus(String transaction) {
            System.out.println("Running consensus: INIT -> PROPOSE -> DECIDE");
            //Clear? at a new round?
            states.clear();
            if (!initConsensus())
                return false;
            if (!proposeConsensus(transaction))
                return false;
            return decideConsensus(transaction);
        }

        private static boolean initConsensus() {
            //initialize 
            int epochNumber = state.getEpochNumber() + 1;
            states = new HashMap<>();
            setState(new EpochSate(systemMembership.getNumberOfNodes(), epochNumber));
            System.out.println("INIT phase successful.");
            return true;
        }

        private static boolean proposeConsensus(String transaction) {
            if(isLeader())//Only the leader
            {
                if(getState().getVal() == null) // val == null
                {
                    getState().setVal(transaction); // val:=v;
                    for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
                    { 
                        String message = formatReadMessage(systemMembership.getLeaderId(),null);
                        perfectLinks.send(nodeId, message);
                    }
            
                }
            }
            System.out.println("PROPOSE phase: " + transaction);
            return true;
        }
        private static boolean handleReadMessage(int senderId)
        {
            if(senderId == systemMembership.getLeaderId()) //Only the leader can send READ msg
            {
                //STATE|valts|val|writeset:
                String message = formatStateMessage(state.getEpochNumber(), state.getValts(), state.getVal(), state.getWriteSet());
                try {
                    cc.input(message);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } 
            }
            return true;
        }
        //TODO 
        private static boolean handleWriteMessage(String message)
        {
            String[] parts = message.split("\\|");

            String v = parts[2]; //get v

            return true;
        }
        private static boolean onDeliver(int SenderId, String message){
            String[] parts = message.split("\\|");
            switch(parts[1]){
                case "READ":
                    handleReadMessage(SenderId); //TODO
                    break;
                case "STATE":
                    //handleStateMessage();
                    break;
                case "WRITE":
                    //handeWriteMessage();      
                    break;
                case "ACCEPT":
                    //handleAcceptMessage();
                    break;
                default: 
                    break;
            }
            return true;
        }
        private static boolean handleCollectedStates(){
            //State in form [State,ts,v,ws] or undefined
            String tmpval = null; //tmpval:=⊥;
            for (Map.Entry<Integer, String> entry : states.entrySet()) {
                String[] split = entry.getValue().split("\\|");
                int ts = Integer.parseInt(split[1]);
                String val = split[2];
                if(ts >= 0 && val!= null) //ts≥0 , v diff null //TODO binds(ts, v, states)
                {
                    tmpval = val;
                }
            }
            //else if 
            //on bounded value?
            if (tmpval == null) {
                String leaderState = states.get(systemMembership.getLeaderId());
                if (leaderState != null && !leaderState.equals("UNDEFINED")) {
                    String[] leaderStateParts = leaderState.split("\\|");
                    String v = leaderStateParts[2];
                    if (v != null ) {
                        tmpval = v; // Unbound value from leader's state
                    }
                }
            }
            //TODO condition after f+1 processes
            if(tmpval != null) //tmp value diff null
            {
                for (Iterator<Map.Entry<Integer, String>> it = state.getWriteSet().entrySet().iterator(); it.hasNext();) {
                    Map.Entry<Integer, String> entry = it.next();
                     //if exits ts such that (ts,tmpvalue) pertence ao writeset
                    if(entry.getValue().equals(tmpval)){
                        it.remove(); //remove old entry
                        state.getWriteSet().put(state.getValts(), tmpval); //add new entry ets: current epoch timestamp
                        break;
                    }
            }
        }
            //BRODCAST WRITE message to all nodes
            for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
            {
                //trigger a send WRITE message containing tmpval
                String message =  formatWriteMessage(state.getEpochNumber(), state.getValts(), tmpval);
                
            }
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
    private static String formatReadMessage(Integer epochNumber, Integer id)
    {
        return "<READ:" + epochNumber + ":" + id + ">";
    }
    private static String formatStateMessage(int epochNumber,Integer valts, String val, HashMap<Integer, String> writeSet)
    {
        //TODO how to write the writeset into the message
        return "<STATE:" + epochNumber + ":" + valts + ":" + val + ":" + writeSet+">";
    }
    private static String formatWriteMessage(int epochNumber, Integer valts, String tmpval)
    {
        return "<WRITE:" + epochNumber + ":" + valts + ":" + tmpval + ">";
    }
    private static String formatAcceptMessage()
    {
        return "<ACCEPT>";
    }

}
