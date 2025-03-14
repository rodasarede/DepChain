package com.sec.depchain.common;

public class Member {
    private String address; 
    private int port; 

    public Member(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public String getAddress() {
        return this.address;
    }
}
