package com.sec.depchain.server;

import java.math.BigInteger;

import com.sec.depchain.common.util.CryptoUtils;

public class EOAAccount extends Account{

    private String privateKey, publicKey;
    
    public EOAAccount(String address){
        super(address, BigInteger.ZERO, BigInteger.ZERO, "");  //TODO
    }
    
    
}
