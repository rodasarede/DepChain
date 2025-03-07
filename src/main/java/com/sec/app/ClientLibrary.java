package com.sec.app;


public class ClientLibrary {
    private final PerfectLinks perfectLinks;
    
    private final SystemMembership systemMembership;


    public ClientLibrary(int clientPort) throws Exception {
        systemMembership = new SystemMembership("src/main/java/com/sec/resources/system_membership.properties");
        this.perfectLinks = new PerfectLinks(clientPort); // Listen for responses //TODO what node ID?
        this.perfectLinks.setDeliverCallback(this::onMessageReceived);

    }

    public void sendRequest(String message, int seqNumber) {
        String formattedMessage = "<append, " + message + ">";

        // Send the request
        // Currently only sends to a single blockchain node (working as a centralized server the leader)
        perfectLinks.send(systemMembership.getMembershipList().get(systemMembership.getLeaderId()).address, systemMembership.getMembershipList().get(systemMembership.getLeaderId()).port, formattedMessage);


    }

    private void onMessageReceived(String srcIP, int srcPort, String message) {
        System.out.println("Perfect Deliver from " + srcIP + ":" + srcPort + " -> " + message);
    }
}


