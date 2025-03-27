package com.sec.depchain.server;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class ContractAccount extends Account {
    private String codeHash;
    private Map<String, String> storage;

    public ContractAccount(String address, String code){
        this(address, BigInteger.ZERO, BigInteger.ZERO, code , new HashMap<>());
    }
    public ContractAccount(String address, BigInteger balance, BigInteger nonce, String code, Map <String, String> storage){
        super(address, balance, nonce, "Contract");
        this.codeHash = code != null ? code : "";
        this.storage = storage != null ? storage :new HashMap<>();
    }
public String getCodeHash() {
    return codeHash;
}
public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
}

}
