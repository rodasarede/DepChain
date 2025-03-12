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
    private TSvaluePair valtsVal; // (valts,val)
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
