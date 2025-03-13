package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ByzantineEpochConsensus {
    private static int N;
    private static int f;

    private static int nodeId;
    private static int leaderId;
    private static EpochSate state;
    private long ets;
    private String written[];
    private static String[] accepted; // Array to store ACCEPT messages
    private ConditionalCollect cc;

    private PerfectLinks perfectLinks;

    private SystemMembership systemMembership;

    public ByzantineEpochConsensus(int leaderId, long ets) {
        this.leaderId = leaderId;
        this.ets = ets;
    }

    public void init() {

        TSvaluePair defaultVal = new TSvaluePair(0, null); // TODO how to initialize

        this.state = new EpochSate(defaultVal, new HashSet<>());

        this.written = new String[N];
        this.accepted = new String[N];
    }

    public void propose(String v){
        if(nodeId == leaderId) //is leader
        {
            if(getState().getValtsVal().getVal() == null)
            {
                TSvaluePair tsValuePair = new TSvaluePair(ets, v); 
                getState().setValtsVal(tsValuePair);
                for(int nodeId: systemMembership.getMembershipList().keySet()) {
                    String message = formatReadMessage(systemMembership.getLeaderId()); //TODO confirm this
                            System.out.println("Leader sending " + message + " to " + nodeId);
                            perfectLinks.send(nodeId, message);
                }
            }
        }//mandar o append ao lider
    }

    public void deliverRead(int senderId) throws Exception {
        if (senderId == leaderId) {
            String message = formatStateMessage(state.getValtsVal(), state.getWriteSet());

            cc = new ConditionalCollect(nodeId, perfectLinks, systemMembership);

            cc.input(message);
        }
    }

    public void Collected(String states) {
        String tmpval = null;
        List<String> CollectedMessages = CollectedMessageSeparator(states);

        boolean firstConditionMet = false;
        for (String entry : CollectedMessages) {
            if (entry.equals("UNDEFINED"))
                continue;
            String[] parts = entry.replace("<", "").replace(">", "").split(":");
            long entryTs = Long.parseLong(parts[1]);
            String entryVal = parts[2];

            if (entryTs >= 0 && entryVal != null && binds(entryTs, entryVal, CollectedMessages)) {
                tmpval = entryVal;
                firstConditionMet = true;
                // TODO do I break??
            }
        }
        if (!firstConditionMet) {
            String leaderEntry = CollectedMessages.get(systemMembership.getLeaderId());
            String[] parts = leaderEntry.replace("<", "").replace(">", "").split(":");
            String entryVal = parts[2];
            if (unbound(CollectedMessages) && entryVal != null) // TODO not sure about this else if
            {
                tmpval = entryVal;
            }
        }
        // TODO condition after f+1 processes
        if (tmpval != null) // tmp value diff null
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

            // BRODCAST WRITE message to all nodes
            for (int nodeId : systemMembership.getMembershipList().keySet()) // for all q∈Π do
            {
                // trigger a send WRITE message containing tmpval
                String message = formatWriteMessage(tmpval);
                perfectLinks.send(nodeId, message);
            }
        }

    }

    public void deliverWrite(int id, String v)
    {
        written[id] = v; //id-1?
        check_write_quorom(v);
    }
    public void deliverAccpet(int id, String v)
    {
        accepted[id] = v; //id-1?
        check_accept_quorom(v);
    }
    private void check_write_quorom(String v){
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

    private void check_accept_quorom(String v){
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
        // trigger ⟨ bep, Decide | v ⟩;
       
        }
    

    private static boolean unbound(List<String> S) {
        if (getNumberOfDefinedEntries(S) < N - f) {
            return false;
        }
        for (String entry : S) {
            if (entry.equals("UNDEFINED"))
                continue;
            String[] parts = entry.replace("<", "").replace(">", "").split(":");
            long entryTs = Long.parseLong(parts[1]);

            if (entryTs != 0) {
                return false;
            }
        }
        return true;
    }
    private static boolean binds(long ts, String v, List <String> states)
    {
        return (states.size() >=  N - f && quoromHighest(ts, v, states) && certifiedValue(ts, v, states));
    }

    private static boolean quoromHighest(long ts, String v, List <String> S)
    {
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
        int count = 0;
        for(String entry: S)
        {
            if(entry.equals("UNDEFINED")) continue;
            String[] parts = entry.replace("<", "").replace(">", "").split(":");
            long entryTs = Long.parseLong(parts[1]);
            String entryVal = parts[2];

            String writeSetString = parts[3];

            Set <TSvaluePair> writeSet = parseWriteSet(writeSetString);
            //I need the writeSet 
            for(TSvaluePair writeSetEntry: writeSet){
                if(writeSetEntry.getTimestamp() >= ts && writeSetEntry.getVal().equals(v))
                {
                    count++;
                }
            }
        }
        return count > f;
    }

    private boolean sound(List <String> S){
        for(String entry: S)
        {
            if(entry.equals("UNDEFINED")) continue;
            String[] parts = entry.replace("<", "").replace(">", "").split(":");
            long entryTs = Long.parseLong(parts[1]);
            String entryVal = parts[2];
            if(binds(entryTs, entryVal, S) || unbound(S))
            {
                return true;
            }
        }
        return false;
    }
    public static int getNumberOfDefinedEntries(List<String> S) {
        int count = 0;
        for (String entry : S) {
            if (!entry.equals(Constants.UNDEFINED)) {
                count++;
            }
        }
        return count;
    }

    public static EpochSate getState() {
        return state;
    }

    private static String formatReadMessage(long ets) {
        return "<READ:" + ets + ">";
    }

    private static String formatStateMessage(TSvaluePair valtsVAl, Set<TSvaluePair> writeSet) {
        // TODO how to write the writeset into the message
        System.out.println("WriteSet: " + writeSet);
        return valtsVAl.getTimestamp() + ":" + valtsVAl.getVal() + ":" + writeSet;
    }

    private static String formatWriteMessage(String tmpval) {
        return "<WRITE:" + tmpval + ">";
    }

    private static String formatAcceptMessage(String val)
    {
        return "<ACCEPT:" + val + ">";
    }

    private static List<String> CollectedMessageSeparator(String collectedMessage) {
        List<String> messages = new ArrayList<>(N);

        // Define a regex pattern to match the <STATE:...> format
        Pattern pattern = Pattern.compile("<STATE:[^>]+>|UNDEFINED");
        Matcher matcher = pattern.matcher(collectedMessage);

        // Find all matches and add them to the list
        while (matcher.find()) {
            messages.add(matcher.group());
        }

        return messages;

    }

    public static Set<TSvaluePair> parseWriteSet(String writeSetString) {
        Set<TSvaluePair> writeSet = new HashSet<>();
        Pattern pattern = Pattern.compile("\\((\\d+),([A-Za-z0-9]+)\\)");
        Matcher matcher = pattern.matcher(writeSetString);

        while (matcher.find()) {
            long timestamp = Long.parseLong(matcher.group(1));
            String val = matcher.group(2);
            writeSet.add(new TSvaluePair(timestamp, val));
        }

        return writeSet;
    }
}