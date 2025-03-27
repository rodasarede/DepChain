package com.sec.depchain.common;

import java.math.BigInteger;

import org.checkerframework.checker.units.qual.s;

public class Transaction {
    private String from; // the address of the sender, that will be signing the transaction. This will be an externally-owned account as contract accounts cannot send transactions
    private String to; //the receiving address (if an externally-owned account, the transaction will transfer value. If a contract account, the transaction will execute the contract code)
    private BigInteger value; //how much ether to transfer
    private String data; //payload (func name, args, etc...); – optional field to include arbitrary data
    private long nonce;  //a sequentially incrementing counter which indicates the transaction number from the account
    private long timeStamp;
    private String signature; //  the identifier of the sender. This is generated when the sender's private key signs the transaction and confirms the sender has authorized this transaction
    private BigInteger gasLimit; //  the maximum amount of gas units that can be consumed by the transaction. The EVM specifies the units of gas required by each computational step
    private BigInteger gasPrice; //how much caller pays for the gas, In ether; Miners collect gas fees, prioritize higher prices. Extra-low price may never run
    //https://ethereum.org/en/developers/docs/transactions/

    public Transaction(String to, String from, BigInteger value, BigInteger gasLimit, BigInteger gasPrice, String data, long nonce, long timestamp, String signature) {
        this.to = to;
        this.from = from;
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
    public String getFrom() {
        return from;
    }
    public String getTo() {
        return to;
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
    public void setFrom(String from) {
        this.from = from;
    }
    public void setTo(String to) {
        this.to = to;
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

