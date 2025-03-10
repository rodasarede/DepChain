package com.sec.depchain.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class PerfectLinks {
    private DeliverCallback deliverCallback; // Callback to deliver to the class above
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; // Store messages to resend
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates
    private final int nodeId;
    private final int port;

    private static final int retries = 5; // Number of retries before considering failure

    private final PrivateKey _privateKey;

    private final Map<Integer, PublicKey> publicKeys;

    public interface DeliverCallback {
        void deliver(int NodeId, String message);
    }

    public PerfectLinks(int nodeId) throws Exception {
        this.port = getPort(nodeId);
        System.out.println("I'm on port " + this.port);
        this.fairLossLinks = new FairLossLinks(this.port);
        this.sentMessages = new ConcurrentHashMap<>();
        this.delivered = new ConcurrentHashMap<>(); // Initialize delivered set
        this.nodeId = nodeId;

        // Register callback to receive messages from FairLossLinks
        this.fairLossLinks.setDeliverCallback(this::onFairLossDeliver);

        // Start listening for messages
        this.fairLossLinks.deliver();


        System.out.println("Loading private key from: " + "../keys/private_key_" + this.nodeId + ".pem");
        this._privateKey = KeyLoader.loadPrivateKey("../keys/private_key_" + this.nodeId + ".pem"); // TODO here with the id of
                                                                                            // the
                                                                                            // node
        //TODO here we can change
        this.publicKeys = KeyLoader.loadPublicKeys("../keys");
        /*PrivateKey KrA = KeyLoader.loadPrivateKey("keys/private_key_1.pem");
        //PublicKey KuA = KeyLoader.loadPublicKey("keys/public_key_1.pem");
        //PublicKey KuB = KeyLoader.loadPublicKey("keys/public_key_2.pem");
        PrivateKey KrB = KeyLoader.loadPrivateKey("keys/private_key_2.pem");

        PublicKey KuA = publicKeys.get(1);
        PublicKey KuB = publicKeys.get(2);
        // Generate shared secret from Party A's perspective
        //byte[] sharedSecretA = CryptoUtils.deriveSharedSecret(KrA, KuB);

        // Generate shared secret from Party B's perspective
        //byte[] sharedSecretB = CryptoUtils.deriveSharedSecret(KrB, KuA);

        // Compare the shared secrets
        System.out.println("Shared Secret A: " + Base64.getEncoder().encodeToString(sharedSecretA));
        System.out.println("Shared Secret B: " + Base64.getEncoder().encodeToString(sharedSecretB));*/
    }

    // Set the callback to notify when a message is delivered
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    // Send a message Perfectly (keep resending)
    public void send(int destId, String message, int seqNumber) {
        // TODO if destId == -1 -> its a client: send to all servers but for now set to 1
        if (destId == -1)
            destId = 1;
        String destIP = getIP(destId);
        int destPort = getPort(destId);
        String messageKey = destId + ":" + message;
        sentMessages.put(messageKey, true);
        System.out.println("Message Key: " + messageKey);

        String messageWithId = nodeId + "|"+ message;
        // Resend indefinitely (until process crashes)
        
        System.out.println("Destination Node Id: " + destId);
        PublicKey destPublicKey = publicKeys.get(destId); 
        // generate mac
        try {

            // IP
            String mac = CryptoUtils.generateMAC(_privateKey, destPublicKey, messageWithId);

            // tampering mac
            /*
            String tamperedMac = mac.substring(0, mac.length() - 1) + "b";
            System.out.println("original mac: " + mac);
            System.out.println("tampered mac: " + tamperedMac);
            mac = tamperedMac;

             */

            String authenticatedMsg = messageWithId + "|" + mac; // append mac
            new Thread(() -> {
                int retriesLeft = retries;
                while (sentMessages.containsKey(messageKey)) {
                    try {
                        if (retriesLeft == 0) {
                            System.out.println("Message not delivered after 5 retries");
                            break;
                        }
                        fairLossLinks.send(destIP, destPort, authenticatedMsg); // send authenticated msg
                        retriesLeft--;
                        Thread.sleep(1000); // Resend every second (adjust as needed)
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Handle received messages from FairLossLinks
    private void onFairLossDeliver(String srcIP, int srcPort, String message) {

        String[] parts = message.split("\\|");
        if (parts.length != 3) {
            System.out.println("Invalid message format: " + message);
            return;

        }
        String messageWithId = parts[0] + "|" + parts[1];
        

        String originalMsg = parts[1];
        
        // String[] elements = getMessageElements(originalMsg);
        

        String receivedMac = parts[2];
        // TODO
        // um unico teste
        System.out.println(receivedMac);
        
        int senderNodeId = !parts[0].startsWith("ACK") ? Integer.parseInt(parts[0]) : Integer.parseInt(parts[0].substring(3));
        System.out.println("Sender Node ID: " + senderNodeId);
        String messageKey =  senderNodeId + ":" + parts[1];
        System.out.println("Message Key: " + messageKey);
        
        

        PublicKey destPublicKey = publicKeys.get(senderNodeId); 
        try {

            if (!CryptoUtils.verifyMAC(_privateKey, destPublicKey, messageWithId, receivedMac)) {
                System.out.println("MAC verification failed for message: " + messageWithId);
                return;



            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // Deliver only if the message has not been delivered before
        if (!delivered.containsKey(messageKey)) {
            System.out.println("Fair loss Deliver from " + srcIP + ":" + srcPort + " -> " + message);
            delivered.put(messageKey, true); // Mark message as delivered

            if (message.startsWith("ACK")) {
                 
                System.out.println("stop resending message: " + messageKey);
                stopResending(messageKey);
                return;
            }


            // Send ACK back to the sender via a single message using fairloss
            try {
                String ackMessage = "ACK" + nodeId + "|" + originalMsg;
                String ackMAC = CryptoUtils.generateMAC(_privateKey, destPublicKey, ackMessage);
                fairLossLinks.send(srcIP, srcPort, ackMessage + "|" + ackMAC);

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (deliverCallback != null) {
                deliverCallback.deliver(senderNodeId, message);
            }
        }
    }

    // Stop resending a message (if needed)
    public void stopResending(String messageKey) {
        sentMessages.remove(messageKey);
    }

    // debug
    public void printSentMessages() {
        System.out.println("Sent Messages: ");
        for (String key : sentMessages.keySet()) {
            System.out.println(key);
        }
    }

    public static String[] getMessageElements(String message) {
        // Extract between "<" and ">"
        int start = message.indexOf('<');
        int end = message.indexOf('>');
        if (start != -1 && end != -1 && start < end) {
            
            String extractedPart = message.substring(start + 1, end);
            String[] elements = extractedPart.split(":");
            System.out.println("Extracted parts: " + Arrays.toString(elements));
            return elements;
        } else {
            System.out.println("Invalid format in message: " + message);
            return null;
        }
    }


    private int getPort(int nodeId) {
        return nodeId + 5000;
    }

    private String getIP(int nodeId) {
        return "localhost";
    }

}
