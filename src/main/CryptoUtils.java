package src.main;

import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    public static String computeMAC(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            //TODO change the key when we have the PKI
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] macBytes = mac.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(macBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute MAC", e);
        }
    }

    // Verify HMAC-SHA256 for a message
    public static boolean verifyMAC(String message, String receivedMAC, String secretKey) {
        String computedMAC = computeMAC(message, secretKey);
        return computedMAC.equals(receivedMAC);
    }

    // authenticate(send_proc p, rec_proc q, message m)

    public String authenticate(String senderID, String receiverID, String message)
    {
        //valid sender? only p can invoke authenticate(p,...)
        //TODO get the key from the PKI
        return computeMAC(message, "key"); //get the secret key
    }
    
    public boolean verifyauth(String senderID, String receiverID, String message, String MAC)
    {
        //verifyauth(p, q, m, a) returns true if and only if p had previously invoked authenticate(p, q, m) and obtained a

        //only q can invoke verifyauth(...,q,...)

        //TODO get the key
        return verifyMAC(message, MAC, "key");
    }
}

