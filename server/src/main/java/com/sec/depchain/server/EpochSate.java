package com.sec.depchain.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EpochSate {
    private Integer valts; //value TS
    private String val; //proposed or decided value 
    private HashMap<String, Integer> writeSet; //set with (TS,val)
    private boolean[] written;
    private boolean[] accepted;

    public EpochSate(int numberOfNodes) {
        this.valts = 0;
        this.val = null;
        this.writeSet = new HashMap<Integer, String>(); //TODO change to a set?
        this.written = new boolean[numberOfNodes];
        this.accepted = new boolean[numberOfNodes];

    }
    public boolean[] getAccepted() {
        return accepted;
    }
    public String getVal() {
        return val;
    }
    public Integer getValts() {
        return valts;
    }
    public HashMap<Integer, String> getWriteSet() {
        return writeSet;
    }
    public boolean[] getWritten() {
        return written;
    }
    public void setAccepted(boolean[] accepted) {
        this.accepted = accepted;
    }
    public void setVal(String val) {
        this.val = val;
    }
    public void setValts(Integer valts) {
        this.valts = valts;
    }
    public void setWriteSet(HashMap<Integer, String> writeSet) {
        this.writeSet = writeSet;
    }
    public void setWritten(boolean[] written) {
        this.written = written;
    }
}
