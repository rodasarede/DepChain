package com.sec.depchain.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.sec.depchain.common.util.KeyLoader;

public class SystemMembership {
    public static final String KEY_DIR = "../common/src/main/java/com/sec/depchain/resources/keys";

    private int leaderId;
    private HashMap<Integer, Member> membershipList = new HashMap<>();
    private int numberOfNodes;
    private Map<Integer, PublicKey> publicKeys; // TODO initialize ?

    public SystemMembership(String filename) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            Properties properties = new Properties();
            properties.load(fis);

            setLeaderId(Integer.parseInt(properties.getProperty("LeaderId")));
            setNumberOfNodes(Integer.parseInt(properties.getProperty("NumberOfNodes")));
            setPublicKeys(KeyLoader.loadPublicKeys(KEY_DIR));
            // System.out.println("Leader ID: " + this.leaderId);

            for (int i = 1; i <= numberOfNodes; i++) {
                String address = properties.getProperty(i + "_Address");
                int port = Integer.parseInt(properties.getProperty(i + "_Port"));
                // TODO Should I save the private key?
                membershipList.put(i, new Member(address, port));
                // System.out.println("Member" + i + " Address: " + address + " Port: " + port);
            }
            System.out.println("Membership List loaded ");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public HashMap<Integer, Member> getMembershipList() {
        return this.membershipList;
    }

    public Map<Integer, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public int getLeaderId() {
        return this.leaderId;
    }

    public void setMembershipList(HashMap<Integer, Member> membershipList) {
        this.membershipList = membershipList;
    }

    public void setPublicKeys(Map<Integer, PublicKey> publicKeys) {
        this.publicKeys = publicKeys;
    }

    public PublicKey getPublicKey(Integer id) {
        return this.publicKeys.get(id);
    }
}
