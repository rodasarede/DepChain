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
    private static int Id;
    private static boolean isLeader =  false; //should start as false
    private static SystemMembership systemMembership;
    private static int PORT;
    private static List<String> blockchain = new ArrayList<>();
    private static PerfectLinks perfectLinks;

    private static EpochSate state = new EpochSate(new TSvaluePair(0, null), new HashSet<>());;
    
    private static String [] written; // TODO what type of array do we need?
    private static String [] accepted; // Array to store ACCEPT messages
    private static int N; // Total number of processes
    private static int f; // Maximum number of Byzantine faults
    private static long ets = 1; // Epoch timestamp

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
        //TODO
        ConditionalCollect cc = new ConditionalCollect(Id, perfectLinks, systemMembership, ByzantineEpochConsensus.sound); // Pass the necessary Id

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
            switch(messageElements[0]) {
                case "append":
                    String transaction = messageElements[1];
                    // Run consensus
                    
                    boolean success = runConsensus(transaction);
                    
                    // String responseMessage = success ? "Transaction confirmed and appended." : "Transaction failed.";
                    // String formattedMessage = "<append:" + messageElements[1] + ":" + responseMessage + ">";
                    // // System.out.println(formattedMessage);
        
                    // // Send confirmation response back to client
                    // int destId = Integer.parseInt(message.split("\\|")[0]);
                    // perfectLinks.send(destId, formattedMessage);
                    break;
                case "READ":
                    System.out.println("Received READ message from " + senderId + " with message: " + message);
                    handleReadMessage(senderId); //TODO
                    break;
                case "STATE":
                    handleCollectedStates(message); //TODO
                    break;
                case "WRITE":
                    handleWriteMessage(message);      
                    break;
                case "ACCEPT":
                    //handleAcceptMessage();
                    break;
                default: 
                    break;
                
            }
        }

        private static boolean runConsensus(String transaction) {
            System.out.println("Running consensus: INIT -> PROPOSE -> DECIDE");
            // is states only for each Epoch? If yes we need to clear after each (sucessfull?) epoch
            //states.clear();   
            if (!initConsensus())
                return false;
            if (!proposeConsensus(transaction))
                return false;
            // if (!decideConsensus(transaction))
            //     return false;
            return true;
        }

        // how do we ensure all processes init (they might not receive append)
        private static boolean initConsensus() {
            System.out.println("INIT phase:");
            // initialize with a value state, output by the Byzantine epoch consensus instance that the process ran previously
            // first epoch means state is empty 
            TSvaluePair defaultVal = new TSvaluePair(0, null);
            state = new EpochSate(defaultVal, new HashSet<>());

            //written = new TSvaluePair[N];
            //accepted = new TSvaluePair[N];

            // shouldnt written be the writeset? and updated acordingly?
            // I know it is in 5.17 algo but still
            written = new String[N];
            accepted = new String[N];

            return true;
        }

        private static boolean  proposeConsensus(String transaction) {
            if(isLeader())//Only the leader
            {
                if(getState().getValtsVal().getVal() == null) // val == null then val := v;
                {
                    //TODO what TS should I put here?
                    TSvaluePair tsValuePair = new TSvaluePair(ets, transaction); 
                    getState().setValtsVal(tsValuePair); // val:=v for valts:=ets;

                    for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
                    {
                        // does it send read to self as well? 
                        // I think so but for now it doesnt for test purposes
                        
                            String message = formatReadMessage(systemMembership.getLeaderId()); //TODO confirm this
                            System.out.println("Leader sending " + message + " to " + nodeId);
                            perfectLinks.send(nodeId, message);
                        
                    }
            
                }
            }
            // System.out.println("PROPOSE phase: " + transaction);
            return true;
        }
        private static boolean handleReadMessage(int senderId)
        {
            // received Read -> invokes conditional collect primitive with message [State,valts,val,writeset] 
            String message = formatStateMessage(state.getValtsVal(), state.getWriteSet());
            try {
                //TODO
                cc = new ConditionalCollect(Id, perfectLinks, systemMembership, ByzantineEpochConsensus.sound());
                cc.setDeliverCallback((messagesFromCC) -> {
                    System.out.println("Received Collected from CC:");
                    for (Integer processId : systemMembership.getMembershipList().keySet()) {
                        System.out.println("Message of nodeId " + processId);
                        System.out.println("Message of nodeId " + messagesFromCC[processId - 1] + "\n");
                    }
                });
                cc.input(message); //maybe we should pass the leader
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
                    handleCollectedStates(message); //TODO
                    break;
                case "WRITE":
                    handleWriteMessage(message);      
                    break;
                case "ACCEPT":
                    //handleAcceptMessage();
                    break;
                default: 
                    break;
            }
            return true;
        }
        private static boolean handleCollectedStates(String S){

            //State in form [State,ts,v,ws] or undefined
            String tmpval = null; //tmpval:=⊥;
            /*if exists ts ≥ 0, v ≠ ⊥ from S such that binds(ts, v, states) then  
                tmpval := v; */
            List <String> CollectedMessages = CollectedMessageSeparator(S);
            boolean firstConditionMet = false;
            for (String entry: CollectedMessages) {
                if(entry.equals("UNDEFINED")) continue;
                String[] parts = entry.replace("<", "").replace(">", "").split(":");
                long entryTs = Long.parseLong(parts[1]);
                String entryVal = parts[2];

               if(entryTs >= 0 && entryVal != null && binds(entryTs, entryVal, CollectedMessages ))
               {
                tmpval = entryVal;
                firstConditionMet = true;
                //TODO do I break??
               }
            }
            if(!firstConditionMet)
            {
                String leaderEntry = CollectedMessages.get(systemMembership.getLeaderId());
                String[] parts = leaderEntry.replace("<", "").replace(">", "").split(":");
                String entryVal = parts[2];
                if(unbound(CollectedMessages) && parts != null) //TODO not sure about this else if
                {
                    tmpval = parts[2];
                }
            }
            //TODO condition after f+1 processes
            if(tmpval != null) //tmp value diff null
            {
                Iterator<TSvaluePair> iterator = state.getWriteSet().iterator();
                while (iterator.hasNext()) {
                    TSvaluePair writeSetEntry = iterator.next();
                    if (state.getValtsVal().getTimestamp() == writeSetEntry.getTimestamp()) {
                        iterator.remove(); // Remove the entry with the matching timestamp
                    }
                }
            
            TSvaluePair tsValuePair = new TSvaluePair(ets, tmpval);
            state.getWriteSet().add(tsValuePair);

            //BRODCAST WRITE message to all nodes
            for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
            {
                //trigger a send WRITE message containing tmpval
                String message =  formatWriteMessage(tmpval);
                perfectLinks.send(nodeId, message);
            }
        }
            return true;
        }
        private static boolean quoromHighest(long ts, String v, List <String> S)
        {
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            int count = 0;
            for(String entry: S)
            {
                if(entry.equals("UNDEFINED")) continue;
                String[] parts = entry.replace("<", "").replace(">", "").split(":");
                long entryTs = Long.parseLong(parts[1]);
                String entryVal = parts[2];

                if(entryTs < ts || (entryTs == ts && entryVal.equals(v)))
                {
                    count ++;
                }
               
            }
            return count > (N + f) / 2;
        }

        private static boolean certifiedValue(long ts, String v, List <String> S)
        {
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            int count = 0;
            for(String entry: S)
            {
                if(entry.equals("UNDEFINED")) continue;
                String[] parts = entry.replace("<", "").replace(">", "").split(":");
                long entryTs = Long.parseLong(parts[1]);
                String entryVal = parts[2];
                //I need the writeSet 
                /*for(TSvaluePair writeSetEntry: entry.writeset){
                    if(writeSetEntry.ts >= ts && writeSetEntry.val.equals(v))
                    {
                        count++;
                    }
                }*/
            }
            return count > f;
        }

        private static boolean binds(long ts, String v, List <String> states)
        {
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            return (states.size() >=  N - f && quoromHighest(ts, v, states) && certifiedValue(ts, v, states));
        }
        private static boolean unbound(List <String> S){
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            if(getNumberOfDefinedEntries(S) < N - f)
            {
                return false;
            }
            for(String entry: S){
                if(entry.equals("UNDEFINED")) continue;
                String[] parts = entry.replace("<", "").replace(">", "").split(":");
                long entryTs = Long.parseLong(parts[1]);

                if(entryTs != 0)
                {
                    return false;
                }
            }
            return true;
        }
        private static boolean predicateSound(List <String> S){
            if(unbound(S)){
                return true;
            }
            for(String entry: S){
                String[] parts = entry.replace("<", "").replace(">", "").split(":");
                long entryTs = Long.parseLong(parts[1]);
                String entryVal = parts[2];
                if(binds(entryTs, entryVal, S)){
                    return true;
                }
            }
            return false;
        }
        public static int getNumberOfDefinedEntries(List <String> S)
        {
            int count = 0;
            for(String entry: S)
            {
                if(!entry.equals(Constants.UNDEFINED))
                {
                    count ++;
                }
            }
            return count;
        }
        private static boolean decideConsensus(String transaction) {
            System.out.println("DECIDE phase: Committing transaction.");
            blockchain.add(transaction);
            return true;
        }
        /*upon event ⟨ al, Deliver | p, [WRITE, v] ⟩ do
                written[p] := v; */
        private static boolean handleWriteMessage(int senderId, String val){
            written[senderId] = val;
            check_write_quorom(val);
            return true;
        }

        private static void check_write_quorom(String v){
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            int count = 0 ;
            for(String writtenEntry: written)
            {
                if(writtenEntry.equals(v)){
                    count ++;
                }
            }
            if(count > (N+f)/2)
            {
                TSvaluePair tsValuePair = new TSvaluePair(ets, v);

                state.setValtsVal(tsValuePair);
                written = new String[N]; //clear written
                for(int nodeId: systemMembership.getMembershipList().keySet()) //for all q∈Π do 
                {
                    //trigger a send ACCEPT Message
                    String message = formatAcceptMessage(v);
                    perfectLinks.send(nodeId, message);
                }
            }
           
        }
        /*upon event ⟨ al, Deliver | p, [ACCEPT, v] ⟩ do
                accepted[p] := v; */
        private static boolean handleAcceptMessage(int senderId, String val){
            written[senderId] = val; //accepted[p] := v;
            check_accept_quorom(val);
            
            return true;
        }
        private static void check_accept_quorom(String v){
            int N = systemMembership.getNumberOfNodes();
            int f = systemMembership.getMaximumNumberOfByzantineNodes();
            int count = 0 ;
            for(String acceptedEntry: accepted)
            {
                if(acceptedEntry.equals(v)){
                    count ++;
                }
            }
            if(count > (N+f)/2)
            {
                TSvaluePair tsValuePair = new TSvaluePair(ets, v);

                state.setValtsVal(tsValuePair);
                accepted = new String[N]; //clear accepted
                
            }
            // ⟨ bep, Decide | v ⟩;
           
            }
        
    
    private static boolean processWrittenValues(){
        int N = systemMembership.getNumberOfNodes();
        int f = systemMembership.getMaximumNumberOfByzantineNodes(); 
        return true;
    }
    private void decide(String val){
        blockchain.add(val);
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

    // READ messages only need  the current timstamp
    private static String formatReadMessage(long ets)
    {
        return "<READ:" + ets + ">";
    }
    private static String formatStateMessage(TSvaluePair valtsVAl, Set<TSvaluePair> writeSet)
    {
        //TODO how to write the writeset into the message
        System.out.println("WriteSet: " + writeSet);
        return valtsVAl.getTimestamp() + ":" + valtsVAl.getVal() + ":" + writeSet ;
    }
    private static String formatWriteMessage(String tmpval)
    {
        return "<WRITE:" + tmpval + ">";
    }
    private static String formatAcceptMessage(String val)
    {
        return "<ACCEPT:" + val + ">";
    }
    private static List <String> CollectedMessageSeparator(String collectedMessage)
    {
        List<String> messages = new ArrayList<>(systemMembership.getNumberOfNodes());
        
        // Define a regex pattern to match the <STATE:...> format
        Pattern pattern = Pattern.compile("<STATE:[^>]+>|UNDEFINED");
        Matcher matcher = pattern.matcher(collectedMessage);
        
        // Find all matches and add them to the list
        while (matcher.find()) {
            messages.add(matcher.group());
        }
        
        return messages;

    }
    

}
