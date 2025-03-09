package com.sec.depchain.common;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;




public class SystemMembership {

    private int leaderId;
    private HashMap<Integer, Member> membershipList = new HashMap<>();
    private int numberOfNodes;

    public SystemMembership(String filename) {
        
        
        try{
            Properties properties = new Properties();
            properties.load(new FileInputStream(filename));

            this.leaderId = Integer.parseInt(properties.getProperty("LeaderId"));
            this.numberOfNodes = Integer.parseInt(properties.getProperty("NumberOfNodes"));
            // System.out.println("Leader ID: " + this.leaderId);

            for (int i = 1; i <= numberOfNodes; i++) {
                String address = properties.getProperty( i + "_Address");
                int port = Integer.parseInt(properties.getProperty(i + "_Port"));
                membershipList.put(i, new Member(address, port));
                // System.out.println("Member" + i + " Address: " + address + " Port: " + port);
            }
            System.out.println("Membership List loaded ");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getLeaderId(){
        return this.leaderId;
    }

    public HashMap<Integer, Member> getMembershipList(){
        return this.membershipList;
    }


    
}
