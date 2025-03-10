package com.sec.depchain.common;

public class Member {
    private String address; // Keep address private
    private int port; // Keep port private

    public Member(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return this.port; // Provide a getter for port
    }

    public String getAddress() {
        return this.address; // Provide a getter for ip
    }
}
