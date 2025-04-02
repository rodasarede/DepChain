package com.sec.depchain.server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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

public class ByzantineEpochConsensus {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByzantineEpochConsensus.class);

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
            LOGGER.debug("Initialized with Leader ID " + leaderId + ", Node ID " + nodeId);
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
            LOGGER.debug("Initialized epoch state.");
        }
    }

    public void propose(String v) {
        toPropose = v;

        if (nodeId == leaderId) {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("I am proposing value " + v);
            }
            if (getState().getValtsVal().getVal() == null) {
                TSvaluePair tsValuePair = new TSvaluePair(ets, toPropose);
                getState().setValtsVal(tsValuePair);
            }
            for (int nodeId : systemMembership.getMembershipList().keySet()) {
                String message = MessageFormatter.formatReadMessage(ets, position);
            

                if (DEBUG_MODE == 1) {
                    LOGGER.debug("Leader " + leaderId + " sending READ message to " + nodeId);
                }

                perfectLinks.send(nodeId, message);
            }
        } else {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("I am not the leader, not doing much for now...");
            }
        }
    }

    public void deliverRead(int senderId) throws Exception {
        if (senderId == leaderId) {
            JSONObject message = MessageFormatter.formatStateMessage(ets, state.getValtsVal(), state.getWriteSet());
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sending STATE " + message + " to conditional collect");
            }
            cc.onInit();
            cc.input(message);
        }
    }

    public void collected(String[] states) {
        if (DEBUG_MODE == 1) {
            LOGGER.debug("collected states: ");
            for (String state : states) {
                LOGGER.debug("- " + state);
            }
        }
        String tmpval = null;

        
        List<String> collectedMessages = new ArrayList<>(Arrays.asList(states));
        boolean firstConditionMet = false;
        for (String entry : states) {
            // System.out.println("Entry: " + entry);
            if (entry.equals(Constants.UNDEFINED))
                continue;

            TSvaluePair parsedEntry = getValsValFromStateMessage(entry);
            

            if (parsedEntry.getTimestamp() >= 0 && parsedEntry.getVal() != null
                    && binds(parsedEntry.getTimestamp(), parsedEntry.getVal(), collectedMessages)) {
                // System.out.println("First condition met");
                tmpval = parsedEntry.getVal();
                firstConditionMet = true;

            }
        }
        if (!firstConditionMet) {
            String leaderEntry = collectedMessages.get(systemMembership.getLeaderId() - 1);
    
            String entryVal = null;
            if (!leaderEntry.equals(Constants.UNDEFINED)) {
                JSONObject json = new JSONObject(leaderEntry);
                JSONObject valuePair = json.getJSONObject("value_pair");
                String value = valuePair.getString("value");  // Extracts "string to propose"
                entryVal = value;

            }
            toPropose = entryVal;
            
            //System.out.println("Leader value to propose: " + toPropose);
            if (unbound(collectedMessages) && toPropose != null) {
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
                String message = MessageFormatter.formatWriteMessage(tmpval, ets);
                perfectLinks.send(nodeId, message);
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
        System.out.println("Checking write quorum for value: " + v);
        int count = 0;
        for (String writtenEntry : written) {
            System.out.println(writtenEntry);
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
                String message = MessageFormatter. formatAcceptMessage(v, ets);
                perfectLinks.send(nodeId, message);
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
        for (String entry : S) {
            if (entry.equals(Constants.UNDEFINED))
                continue;

            JSONObject entryJson = new JSONObject(entry);
            JSONObject valuePair = entryJson.getJSONObject("value_pair");

            long entryTs = valuePair.getLong("timestamp");
            // System.out.println("Parsed Entry timestamp: " + parsedEntry.getTimestamp() + " " + parsedEntry.getVal());
            if (entryTs != 0) {
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
        for (String entry : S) {
            if (entry.equals(Constants.UNDEFINED))
                continue;
                
                JSONObject entryJson = new JSONObject(entry);

                JSONObject valuePair = entryJson.getJSONObject("value_pair");
        
                long entryTs = valuePair.getLong("timestamp");
                String entryVal = valuePair.isNull("value") ? null : valuePair.getString("value");

        // Safe null comparison
        boolean valuesMatch = (entryVal == null && v == null) || 
                            (entryVal != null && entryVal.equals(v));

            if (entryTs < ts || (entryTs == ts && valuesMatch)) {
                count++;
            }
        }
        return count > (N + f) / 2;
    }

    private static boolean certifiedValue(long ts, String v, List<String> S) {
        int count = 0;
        for (String entry : S) {
            if (entry.equals(Constants.UNDEFINED))
                continue;
                
            JSONObject entryJson = new JSONObject(entry);
            JSONArray writeSetArray = entryJson.getJSONArray("write_set");
    
            for (int i = 0; i < writeSetArray.length(); i++) {
                JSONObject writeSetEntry = writeSetArray.getJSONObject(i);
                long entryTs = writeSetEntry.getLong("timestamp");
                String entryVal = writeSetEntry.getString("value");
                
                if (entryTs >= ts && entryVal.equals(v)) {
                    count++;
                }
            }
        }
        return count > f;
    }

    public boolean sound(List<String> S) {
        for (String entry : S) {
            if (entry.equals(Constants.UNDEFINED))
                continue;
            System.out.println(entry);
            TSvaluePair parsedEntry = getValsValFromStateMessage(entry);

            if (binds(parsedEntry.getTimestamp(), parsedEntry.getVal(), S) || unbound(S)) {
                // dont we have to change something?
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

    private TSvaluePair getValsValFromStateMessage(String entry) {
        JSONObject entryJson = new JSONObject(entry);

        JSONObject valuePair = entryJson.getJSONObject("value_pair");
        Object valueObj = valuePair.get("value");

        String value = (valueObj == JSONObject.NULL || valueObj == null) ? null : valueObj.toString();
        return new TSvaluePair(
            valuePair.getLong("timestamp"),
            value
        );
    }

    public void setCc(ConditionalCollect cc) {
        this.cc = cc;
    }

}