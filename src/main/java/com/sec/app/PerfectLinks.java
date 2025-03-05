package com.sec.app;
import java.util.concurrent.*;

public class PerfectLinks {
    private final FairLossLinks fairLossLinks;
    private final ConcurrentHashMap<String, Boolean> sentMessages; // Store messages to resend
    private final ConcurrentHashMap<String, Boolean> delivered; // Store delivered messages to avoid duplicates
    private DeliverCallback deliverCallback; // Callback to notify the main class

    private static final int retries = 5; // Number of retries before considering failure

    public interface DeliverCallback {
        void deliverReceivedMessage(String srcIP, int srcPort, String message);
    }

    public PerfectLinks(int port) throws Exception {
        this.fairLossLinks = new FairLossLinks(port);
        this.sentMessages = new ConcurrentHashMap<>();
        this.delivered = new ConcurrentHashMap<>(); // Initialize delivered set

        // Register callback to receive messages from FairLossLinks
        this.fairLossLinks.setDeliverCallback(this::onFairLossDeliver);

        // Start listening for messages
        this.fairLossLinks.deliver();
    }

    // Set the callback to notify when a message is delivered
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    // Send a message Perfectly (keep resending)
    public void send(String destIP, int destPort, String message) {
        String messageKey = destIP + ":" + destPort + ":" + message;
        sentMessages.put(messageKey, true);

        // Resend indefinitely (until process crashes)
        
        new Thread(() -> {
            int retriesLeft = retries;
            while (sentMessages.containsKey(messageKey) ) {
                try {
                    if(retriesLeft==0){
                        System.out.println("Message not delivered after 5 retries");
                        break;
                    }
                    fairLossLinks.send(destIP, destPort, message);
                    retriesLeft--;
                    Thread.sleep(1000); // Resend every second (adjust as needed)
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Handle received messages from FairLossLinks
    private void onFairLossDeliver(String srcIP, int srcPort, String message) {
        String messageKey = srcIP + ":" + srcPort + ":" + message;
        
        // Deliver only if the message has not been delivered before
        if (!delivered.containsKey(messageKey)) {
            System.out.println("Fair loss Deliver from " + srcIP + ":" + srcPort + " -> " + message);
            delivered.put(messageKey, true); // Mark message as delivered
            
            if(message.startsWith("ACK:")){
                // remove ACK: from message 
                message = message.substring(4);
                System.out.println("stop resending message: " + message);
                stopResending(srcIP, srcPort, message);
                return;
            }
            // Send ACK back to the sender via a single message using fairloss
            try {
                fairLossLinks.send(srcIP, srcPort, "ACK:"+ message);
            } catch (Exception e) {
                e.printStackTrace();
            } 
            if (deliverCallback != null) {
                deliverCallback.deliverReceivedMessage(srcIP, srcPort, message);
            }
        }
    }

    // Stop resending a message (if needed)
    public void stopResending(String destIP, int destPort, String message) {
        String messageKey = destIP + ":" + destPort + ":" + message;
        sentMessages.remove(messageKey);
    }

    //debug 
    public void printSentMessages(){
        System.out.println("Sent Messages: ");
        for (String key : sentMessages.keySet()) {
            System.out.println(key);
        }
    }

}
