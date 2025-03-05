package com.sec.app;


public class ClientLibrary {
    private final PerfectLinks perfectLinks;
    private final String serverAddress;
    private final int serverPort;

    public ClientLibrary(String serverAddress, int serverPort, int clientPort) throws Exception {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.perfectLinks = new PerfectLinks(clientPort); // Listen for responses
        this.perfectLinks.setDeliverCallback(this::onMessageReceived);
    }

    public void sendRequest(String message, int seqNumber) {
        String formattedMessage = "<append, " + message + ">";

        // Send the request
        // Currently only sends to a single blockchain node (working as a centralized server)
        perfectLinks.send(serverAddress, serverPort, formattedMessage);


    }

    private void onMessageReceived(String srcIP, int srcPort, String message) {
        System.out.println("Perfect Deliver from " + srcIP + ":" + srcPort + " -> " + message);
    }
}


