package com.sec.depchain.server;

import java.security.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class TSvaluePair{
    private final long timestamp;
    private final String val;

    public TSvaluePair(long timestamp, String val)
    {
        this.timestamp = timestamp;
        this.val = val;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public String getVal() {
        return val;
    }
}
public class EpochSate {
    // value that the process received most recently in a quorum of writes
    private TSvaluePair valtsVal; // (valts,val)
    // entry for every value that the process has ever written (TS is the most recently timesetamp where certain value was written )
    // process writes v when it sends Write message to all processes containing  v during write phase
    private Set<TSvaluePair> writeSet; //set with (TS,val)


    public EpochSate(TSvaluePair valtsVal, Set<TSvaluePair> writSet) {
        this.valtsVal = valtsVal;
        this.writeSet = writeSet;
    }
    public TSvaluePair getValtsVal() {
        return valtsVal;
    }
    public Set<TSvaluePair> getWriteSet() {
        return writeSet;
    }
    public void setValtsVal(TSvaluePair valtsVal) {
        this.valtsVal = valtsVal;
    }
    public void setWriteSet(Set<TSvaluePair> writeSet) {
        this.writeSet = writeSet;
    }

}
