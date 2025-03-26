package com.sec.depchain.server;

import java.math.BigInteger;

import org.checkerframework.checker.units.qual.s;

public class Transaction {
    private String sender; 
    private String receiver; //to destination address (external or contract)
    private BigInteger value; //how much ether to transfer
    private String data; //payload (func name, args, etc...)
    private long nonce; //transaction sequence number (from sender)
    private long timeStamp;
    private String signature; // ECDSA signature args?
    private BigInteger gasLimit; // Max gas caller willing to spend; Call aborts if exceeded; No refunds !
    private BigInteger gasPrice; //how much caller pays for the gas, In ether; Miners collect gas fees, prioritize higher prices. Extra-low price may never run

    public Transaction(String sender, String receiver, BigInteger value, BigInteger gasLimit, BigInteger gasPrice, String data, long nonce, long timestamp, String signature) {
        this.sender = sender;
        this.receiver = receiver;
        this.value = value;
        this.gasLimit = gasLimit;
        this.gasPrice = gasPrice;
        this.data = data;
        this.nonce = nonce;
        this.timeStamp = timestamp;
        this.signature = signature;
    }
    public String getData() {
        return data;
    }
    public BigInteger getGasLimit() {
        return gasLimit;
    }
    public BigInteger getGasPrice() {
        return gasPrice;
    }
    public long getNonce() {
        return nonce;
    }
    public String getReceiver() {
        return receiver;
    }
    public String getSender() {
        return sender;
    }
    public String getSignature() {
        return signature;
    }
    public long getTimeStamp() {
        return timeStamp;
    }
    public BigInteger getValue() {
        return value;
    }
    public void setData(String data) {
        this.data = data;
    }
    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }
    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }
    public void setNonce(long nonce) {
        this.nonce = nonce;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
    public void setValue(BigInteger value) {
        this.value = value;
    }

}

