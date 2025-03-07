package com.sec.app;

public class ClientApplication {
    private static int seqNumber = 1;
    public static void main(String[] args) throws Exception {
        ClientLibrary client = new ClientLibrary( 5001); // clientPort //TODO
            
        // create app interface 
        String request = "example entry";
        client.sendRequest(request, seqNumber);
        seqNumber++;
        
    }
}

