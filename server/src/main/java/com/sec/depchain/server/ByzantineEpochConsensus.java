package com.sec.depchain.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

import org.json.JSONObject;
import org.json.JSONArray;


public class ByzantineEpochConsensus {

    private static int N;
    private static int f;
    private static int position=1;
    private static int nodeId;
    private static int leaderId;
    private EpochSate state;
    private long ets;
    private String[] written;
    private String[] accepted;
    private ConditionalCollect cc;
    private String toPropose;
    private PerfectLinks perfectLinks;
    private SystemMembership systemMembership;
    private Timer quorumWriteTimer;
    private int countAccepts=0;
    private Timer quorumAcceptTimer;
    private int countWrites=0;
    private boolean writeQuorumReached = false;
    private boolean acceptQuorumReached = false;
    private static final int DEBUG_MODE = 1;
    private BlockchainMember blockchainMember;



    public ByzantineEpochConsensus(int leaderId, long ets, PerfectLinks perfectLinks, SystemMembership systemMembership,
                                   int nodeId, BlockchainMember blockchainMember) throws Exception {
        this.leaderId = leaderId;
        this.ets = ets;
        this.perfectLinks = perfectLinks;
        this.systemMembership = systemMembership;
        this.N = systemMembership.getNumberOfNodes();
        this.f = systemMembership.getMaximumNumberOfByzantineNodes();
        this.cc = new ConditionalCollect(leaderId, perfectLinks, systemMembership, this::sound);
        this.cc.setDeliverCallback(this::onCollectedDelivery);
        this.nodeId = nodeId;

        this.blockchainMember = blockchainMember;
        if (DEBUG_MODE == 1) {
            System.out.println("BEP - DEBUG: Initialized with Leader ID " + leaderId + ", Node ID " + nodeId);
        }
    }

    public void init() {
        TSvaluePair defaultVal = new TSvaluePair(0, null);
        this.state = new EpochSate(defaultVal, new HashSet<>());
        this.written = new String[N];
        this.accepted = new String[N];
        countAccepts = 0;
        countWrites = 0;

        if (DEBUG_MODE == 1) {
            System.out.println("BEP - DEBUG: Initialized epoch state.");
        }
    }

    public void propose(String v) {
        toPropose = v;

        if (nodeId == leaderId) {
            if (DEBUG_MODE == 1) {
                System.out.println("BEP - DEBUG: I am proposing value " + v);
            }

            if (getState().getValtsVal().getVal() == null) {
                TSvaluePair tsValuePair = new TSvaluePair(ets, toPropose);
                getState().setValtsVal(tsValuePair);
            }

            for (int nodeId : systemMembership.getMembershipList().keySet()) {
                String message = formatReadMessage(ets);

                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("type", "READ");
                jsonMessage.put("ets", ets);
                jsonMessage.put("position", position);

                if (DEBUG_MODE == 1) {
                    System.out.println("BEP - DEBUG: Leader " + leaderId + " sending READ message to " + nodeId);
                }

                perfectLinks.send(nodeId, jsonMessage.toString());
            }
        } else {
            if (DEBUG_MODE == 1) {
                System.out.println("BEP - DEBUG: I am not the leader, not doing much for now...");
            }
        }
    }

    public void deliverRead(int senderId) throws Exception {
        if (senderId == leaderId) {
            //String message = formatStateMessage(ets, state.getValtsVal(), state.getWriteSet());

            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("type", "STATE");
            jsonMessage.put("ets", ets);
            jsonMessage.put("ts", state.getValtsVal().getTimestamp());

            if (DEBUG_MODE == 1) {
                System.out.println("BEP - DEBUG: value is: " + state.getValtsVal().getVal() + ".");
            }
            if (state.getValtsVal().getVal() == null) {
                jsonMessage.put("val", "null");
            } else{
                jsonMessage.put("val", state.getValtsVal().getVal());
            }

            jsonMessage.put("write_set", state.getWriteSet());

            if (DEBUG_MODE == 1) {
                System.out.println("BEP - DEBUG: Sending STATE " + jsonMessage.toString() + " to conditional collect");
            }
            cc.onInit();
            cc.input(jsonMessage.toString());
        }
    }

    public void collected(String[] states) {
        if (DEBUG_MODE == 1) {
            System.out.println("BEP - DEBUG: collected states: ");
            for (String s : states) {
                System.out.println("BEP - DEBUG: - " + s);
            }
        }
        String tmpval = null;

        List<String> CollectedMessages = new ArrayList<>();
        for (String s : states) {
            CollectedMessages.add(s);
        }
        boolean firstConditionMet = false;
        for (String s : states) {
            // System.out.println("Entry: " + entry);
            if (s.equals(Constants.UNDEFINED))
                continue;

            //TSvaluePair parsedEntry = getValsValFromStateMessage(entry);
            JSONObject jsonSTATE = new JSONObject(s);
            String value = jsonSTATE.getString("val");
            int timeStamp = jsonSTATE.getInt("ts");

            if (timeStamp >= 0 && value != null
                    && binds(timeStamp, value, CollectedMessages)) {
                // System.out.println("First condition met");
                tmpval = value;
                firstConditionMet = true;

            }
        }
        if (!firstConditionMet) {
            // String leaderEntry = CollectedMessages.get(systemMembership.getLeaderId() - 1);
            // String[] parts = leaderEntry.replace("<", "").replace(">", "").split(":");
            // String entryVal = null;
            // if (!leaderEntry.equals(Constants.UNDEFINED)) {
            //     entryVal = parts[2];
            // }

            String leaderEntry = CollectedMessages.get(systemMembership.getLeaderId() - 1);
            //String[] parts = leaderEntry.replace("<", "").replace(">", "").split(":");

            JSONObject jsonLeaderEntry = new JSONObject(leaderEntry);
            //String value = jsonSTATE.getString("val");

            String entryVal = null;
            if (!leaderEntry.equals(Constants.UNDEFINED)) {
                entryVal = jsonLeaderEntry.getString("val");
            }
            toPropose = entryVal;
            
            //System.out.println("Leader value to propose: " + toPropose);
            if (unbound(CollectedMessages) && toPropose != null) {
                tmpval = toPropose;
            }
        }
        
        // means we have a value to propose
        if (tmpval != null) // tmp value diff null
        {
            if (state.getWriteSet() != null) {
                Iterator<TSvaluePair> iterator = state.getWriteSet().iterator();
                while (iterator.hasNext()) {
                    TSvaluePair writeSetEntry = iterator.next();
                    if (state.getValtsVal().getTimestamp() == writeSetEntry.getTimestamp()) {
                        iterator.remove(); // Remove the entry with the matching timestamp
                    }
                }
            }

            TSvaluePair tsValuePair = new TSvaluePair(ets, tmpval);
            state.getWriteSet().add(tsValuePair);

            // BRODCAST WRITE message to all nodes
            for (int nodeId : systemMembership.getMembershipList().keySet()) // for all q∈Π do
            {
                // trigger a send WRITE message containing tmpval
                //String message = formatWriteMessage(tmpval, ets);

                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("type", "WRITE");
                jsonMessage.put("ets", ets);
                jsonMessage.put("value", tmpval);

                perfectLinks.send(nodeId, jsonMessage.toString());
            }
        }

    }

    public void deliverWrite(int id, String v) {
        written[id - 1] = v;
        check_write_quorom(v);
    }

    public void deliverAccept(int id, String v) {
        accepted[id - 1] = v;
        check_accept_quorom(v);
    }

    private void check_write_quorom(String v) {
        // System.out.println("Checking write quorum for value: " + v);
        int count = 0;
        for (String writtenEntry : written) {
            if (writtenEntry != null && writtenEntry.equals(v)) {
                count++;
            }
        }
       
        if (count > (N + f) / 2) {
            if (quorumWriteTimer != null) {
                quorumWriteTimer.cancel();
                quorumWriteTimer.purge();
            }
            TSvaluePair tsValuePair = new TSvaluePair(ets, v);

            state.setValtsVal(tsValuePair);
            writeQuorumReached = true;
            written = new String[N]; // clear written
            for (int nodeId : systemMembership.getMembershipList().keySet()) // for all q∈Π do
            {
                // trigger a send ACCEPT Message
                // String message = formatAcceptMessage(v, ets);

                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("type", "ACCEPT");
                jsonMessage.put("ets", ets);
                jsonMessage.put("value", v);

                perfectLinks.send(nodeId, jsonMessage.toString());
            }
        }else{
            if (quorumWriteTimer != null) {
                quorumWriteTimer.cancel();
                quorumWriteTimer.purge();
            }
            // Set a timeout to abort if quorum is not reached in time
            quorumWriteTimer = new Timer();
            quorumWriteTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // System.out.println("CountWrites: " + countWrites );
                    if (!writeQuorumReached) {
                        //System.out.println("Write quorum not reached, aborting...");
                        written = new String[N]; // Clear the written array
                        writeQuorumReached = false;
                    }else{
                        //System.out.println("Timer:Write quorum reached");
                    }
                }
            },5000 - count *1000); // Timeout 
        }

    }

    private void check_accept_quorom(String v) {
        int count = 0;
        for (String acceptedEntry : accepted) {
            if (acceptedEntry != null && acceptedEntry.equals(v)) {
                count++;
            }
        }
        countAccepts = count;
        if (count > (N + f) / 2) {
            if (quorumAcceptTimer != null) {
                quorumAcceptTimer.cancel();
                quorumAcceptTimer.purge();
            }
            TSvaluePair tsValuePair = new TSvaluePair(ets, v);
            state.setValtsVal(tsValuePair);
            accepted = new String[N]; // clear accepted
            acceptQuorumReached = true;
            position++;
            blockchainMember.decide(v);

        }else{
            if (quorumAcceptTimer != null) {
                quorumAcceptTimer.cancel();
                quorumAcceptTimer.purge();
            }
            quorumAcceptTimer = new Timer();
            quorumAcceptTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!acceptQuorumReached) {
                        //System.out.println("Accept quorum not reached, aborting...");
                        accepted = new String[N]; // Clear the accept array
                        acceptQuorumReached = false;
                    }else{
                        //System.out.println("Timer:Accept quorum reached");
                    }
                }
            }, 5000 - count *1000); // Timeout 
        }

    }

    private boolean unbound(List<String> S) {
        if (getNumberOfDefinedEntries(S) < N - f) {
            return false;
        }
        for (String s : S) {
            if (s.equals(Constants.UNDEFINED))
                continue;

            JSONObject jsonSTATE = new JSONObject(s);
            //String value = jsonSTATE.getString("val");
            int timeStamp = jsonSTATE.getInt("ts");

            //TSvaluePair parsedEntry = getValsValFromStateMessage(entry);
            // System.out.println("Parsed Entry timestamp: " + parsedEntry.getTimestamp() + " " + parsedEntry.getVal());
            if (timeStamp != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean binds(long ts, String v, List<String> states) {
        return (states.size() >= N - f && quoromHighest(ts, v, states) && certifiedValue(ts, v, states));
    }

    private static boolean quoromHighest(long ts, String v, List<String> S) {
        int count = 0;
        for (String s : S) {
            if (s.equals(Constants.UNDEFINED))
                continue;
            /*
            String[] parts = state.split(":");
            long entryTs = Long.parseLong(parts[1]);
            String entryVal = parts[2];
             */
            JSONObject jsonSTATE = new JSONObject(s);
            String value = jsonSTATE.getString("val");
            int timeStamp = jsonSTATE.getInt("ts");

            if (timeStamp < ts || (timeStamp == ts && value.equals(v))) {
                count++;
            }
        }
        // System.out.println("Count: " + count + " N + f: " + (N + f) / 2);
        return count > (N + f) / 2;
    }

    private static boolean certifiedValue(long ts, String v, List<String> S) {
        int count = 0;
        for (String s : S) {
            if (s.equals(Constants.UNDEFINED))
                continue;
/*
            String[] parts = entry.replace("<", "").replace(">", "").split(":");
            String writeSetString = parts[2];
            Set<TSvaluePair> writeSet = parseWriteSet(writeSetString);
*/
            JSONObject jsonSTATE = new JSONObject(s);
            String value = jsonSTATE.getString("val");
            int timeStamp = jsonSTATE.getInt("ts");
            JSONArray writeSet = jsonSTATE.getJSONArray("write_set");
/*
            for (TSvaluePair writeSetEntry : writeSet) {
                if (writeSetEntry.getTimestamp() >= ts && writeSetEntry.getVal().equals(v)) {
                    count++;
                }

            }
*/
        }
        // System.out.println("Count: " + count + " f: " + f);
        return count > f;
    }

    public boolean sound(List<String> S) {

        for (String s : S) {
            if (s.equals(Constants.UNDEFINED))
                continue;

            //TSvaluePair parsedEntry = getValsValFromStateMessage(entry);
            JSONObject jsonSTATE = new JSONObject(s);
            String value = jsonSTATE.getString("val");
            int timeStamp = jsonSTATE.getInt("ts");

            if (binds(timeStamp, value, S) || unbound(S)) {
                // dont we have to change something? i don't think so
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

    private static String formatReadMessage(long ets) {
        return "<READ:" + ets +  ":" + position + ">";
    }

    private static String formatStateMessage(long ets, TSvaluePair valtsVAl, Set<TSvaluePair> writeSet) {
        // TODO write the writeset into the message
        // System.out.println("WriteSet: " + writeSet); 
        return ets + ":" + valtsVAl.getTimestamp() + ":" + valtsVAl.getVal() + ":" + writeSet;
    }

    private static String formatWriteMessage(String tmpval, long ets) {
        return "<WRITE:" + ets + ":" + tmpval + ">";
    }

    private static String formatAcceptMessage(String val, long ets) {
        return "<ACCEPT:" + ets + ":" + val + ">";
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

    public EpochSate getState() {
        return state;
    }

    private void onCollectedDelivery(String[] collectedMessages) {
        
        //System.out.println("Received on Consensus Layer Collected Messages:");
        for (int i = 0; i < collectedMessages.length; i++) {
            //System.out.println("Message from node " + i + ": " + collectedMessages[i]);
        }
        collected(collectedMessages);

    }

    private static TSvaluePair getValsValFromStateMessage(String entry) {
        String[] parts = entry.replace("<", "").replace(">", "").split(":");
        long entryTs = Long.parseLong(parts[1]);
        String entryVal = parts[2];
        return new TSvaluePair(entryTs, entryVal);
    }

    public void setCc(ConditionalCollect cc) {
        this.cc = cc;
    }

}