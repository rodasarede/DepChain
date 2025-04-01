package com.sec.depchain.server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sec.depchain.common.Block;
import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;

public class ByzantineEpochConsensusBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByzantineEpochConsensusBlock.class);

    private static int N;
    private static int f;
    private static int position=1;
    private static int nodeId;
    private static int leaderId;
    private EpochSateJava state;
    private long ets;
    private Block[] written;
    private Block[] accepted;
    private ConditionalCollect cc;
    private Block toPropose;
    private PerfectLinks perfectLinks;
    private SystemMembership systemMembership;
    private Timer quorumWriteTimer;
    private int countAccepts;
    private Timer quorumAcceptTimer;
    private int countWrites;
    private boolean writeQuorumReached = false;
    private boolean acceptQuorumReached = false;
    private static final int DEBUG_MODE = 1;
    private BlockchainMember blockchainMember;


    public ByzantineEpochConsensusBlock(int leaderId, long ets, PerfectLinks perfectLinks, SystemMembership systemMembership,
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
        this.written = new Block[N];
        this.accepted = new Block[N];
        this.countAccepts = 0;
        this.countWrites = 0;
        Block latestBlock = blockchainMember.getBlockchain_1().getLatestBlock();
        TSvaluePairBlock defaultVal = new TSvaluePairBlock(0, latestBlock);
        this.state = new EpochSateJava(defaultVal, new HashSet<>()); //TODO duvida 

        if (DEBUG_MODE == 1) {
            LOGGER.debug("Initialized epoch state.");
        }
    }

    public void propose(Block v) {
        if(v != null){
            toPropose = v;
        }

        if (nodeId == leaderId) {
            if (DEBUG_MODE == 1) {
                LOGGER.debug("I am proposing value " + v.getBlockHash());
            }
            for (int nodeId : systemMembership.getMembershipList().keySet()) {
                String message = formatReadMessage(ets);

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
            String message = formatStateMessage(ets, state.getValtsVal(), state.getWriteSet()); //TODO 
            if (DEBUG_MODE == 1) {
                LOGGER.debug("Sending STATE " + message + " to conditional collect");
            }
            cc.onInit();
            cc.input(message);
        }
    }
    public boolean sound(List<String> S) {
        return true;
    }
    public void setCc(ConditionalCollect cc) {
        this.cc = cc;
    }
    
    private void onCollectedDelivery(String[] collectedMessages) {
        
        //System.out.println("Received on Consensus Layer Collected Messages:");
        for (int i = 0; i < collectedMessages.length; i++) {
            //System.out.println("Message from node " + i + ": " + collectedMessages[i]);
        }
    }
    private static String formatReadMessage(long ets) {
        return "<READ:" + ets +  ":" + position + ">";
    }

    private String formatStateMessage(long ets, TSvaluePairBlock valtsVal, Set<TSvaluePairBlock> writeSet) {
        JSONObject stateMessage = new JSONObject();
        stateMessage.put("ets", ets);
        
        // Serialize valtsVal (timestamp + block)
        JSONObject valtsValJson = new JSONObject();
        valtsValJson.put("timestamp", valtsVal.getTimestamp());
        valtsValJson.put("block", 
            valtsVal.getVal() != null ? 
                Block.serializeBlock(valtsVal.getVal()) : 
                null
        );
        stateMessage.put("valtsVal", valtsValJson);
        
        // Serialize writeSet
        stateMessage.put("writeSet", serializeWriteSet(writeSet));
        
        return stateMessage.toString();
    }

    private JSONArray serializeWriteSet(Set<TSvaluePairBlock> writeSet) {
    JSONArray writeSetArray = new JSONArray();
    for (TSvaluePairBlock entry : writeSet) {
        JSONObject entryJson = new JSONObject();
        entryJson.put("timestamp", entry.getTimestamp());
        
        // Serialize the Block in TSvaluePair (reuse your existing serializeBlock)
        if (entry.getVal() != null) {
            entryJson.put("block", Block.serializeBlock(entry.getVal()));
        } else {
            entryJson.put("block", "null");
        }
        
        writeSetArray.add(entryJson);
    }
    return writeSetArray;
}
}