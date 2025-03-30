package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sec.depchain.common.FairLossLinks.DeliverCallback;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;

public class PerfectLinksTest{
    private FairLossLinks mockFairLossLinksSender;
    private FairLossLinks mockFairLossLinksReceiver;

    private PerfectLinks senderPerfectLinks;
    private PerfectLinks receiverPerfectLinks;

    private int senderNodeId = 1 ;
    private int receiverNodeId = 2;

    //private PrivateKey senderPrivateKey;
    //private PublicKey ReceiverPublicKey;

    @BeforeEach
    public void setUp() throws Exception{

        MockitoAnnotations.openMocks(this).close(); 

        mockFairLossLinksSender = mock(FairLossLinks.class);
        mockFairLossLinksReceiver = mock(FairLossLinks.class);

        senderPerfectLinks = new PerfectLinks(senderNodeId, mockFairLossLinksSender);
        receiverPerfectLinks = new PerfectLinks(receiverNodeId, mockFairLossLinksReceiver);

        senderPerfectLinks.setDeliverCallback((srcId, message) -> {});
        receiverPerfectLinks.setDeliverCallback((srcId, message) -> {});

        //privateKeySender = senderPerfectLinks.getPrivateKey();
        //publicKeyRec = receiverPerfectLinks.getPublicKey(2);
    }

    @Test
    void testSendMessageSucc() throws Exception{

        senderPerfectLinks.send(receiverNodeId, "Hello from the other side.");

        // fairLossLinks.send(...) is called in a new thread, wait for it
        Awaitility.await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(mockFairLossLinksSender, atLeastOnce()).send(anyString(), anyInt(), anyString());
        });
    }

    @Test
    public void testMessageRetransmission() throws Exception {
        // Send a message and simulate no ACK being received

        senderPerfectLinks.send(receiverNodeId, "testMessage");

        // Wait for retransmission to occur
        Thread.sleep(2000); // Allow time for retransmission

        // Verify that the message was retransmitted
        verify(mockFairLossLinksSender, atLeast(2)).send(anyString(), anyInt(), anyString());
    }
    //TODO
  /*   @Test
public void testOutOfOrderMessages() throws Exception {
    // Simulate out-of-order messages
    String msg1 = "Message 1";
    String msg2 = "Message 2";

    // Generate MACs for the messages
    String mac1 = CryptoUtils.generateMAC(privateKeySender, publicKeyRec, "1|1|" + msg1);
    String mac2 = CryptoUtils.generateMAC(privateKeySender, publicKeyRec, "1|2|" + msg2);
    // Set the deliverCallback to a mock object
    PerfectLinks.DeliverCallback mockDeliverCallbackCollect = mock(PerfectLinks.DeliverCallback.class);
    receiver.setDeliverCallback(mockDeliverCallbackCollect);

    Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
        "onFairLossDeliver", String.class, int.class, String.class
    );
    onFairLossDeliver.setAccessible(true); // Make the private method accessible

    // Deliver messages out of order
    onFairLossDeliver.invoke(receiver, "127.0.0.1", 6001, "1|2|" + msg2 + "|" + mac2); // Message 2 first
    onFairLossDeliver.invoke(receiver, "127.0.0.1", 6001, "1|1|" + msg1 + "|" + mac1); // Message 1 second


    // Verify that messages are delivered in the correct order
    InOrder inOrder = inOrder(mockDeliverCallbackCollect);
    inOrder.verify(mockDeliverCallbackCollect).deliver(1, msg1); // Message 1 first
    inOrder.verify(mockDeliverCallbackCollect).deliver(1, msg2); // Message 2 second
}*/
/* TODO erro

@Test
public void testDuplicateMessages() throws Exception {
    // Simulate a valid message
    String mac = CryptoUtils.generateMAC(privateKeySender, publicKeyRec, "1|1|Message 1");
    Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
        "onFairLossDeliver", String.class, int.class, String.class
    );
    onFairLossDeliver.setAccessible(true); // Make the private method accessible
    // Deliver the same message twice
    onFairLossDeliver.invoke(receiver, "127.0.0.1", 5011, "1|1|" + "Message 1" + "|" + mac); // Message 1 
    onFairLossDeliver.invoke(receiver, "127.0.0.1", 5001, "1|1|" + "Message 1" + "|" + mac); // Message 1 second

    // Verify that the message is delivered only once
    verify(receiver.getDeliverCallback(), times(1)).deliver(1, "Message 1");
} */

/*@Test
public void testInvalidMAC() throws Exception {
    // Simulate a message with an invalid MAC
    String invalidMAC = "invalid-mac";
    Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
        "onFairLossDeliver", String.class, int.class, String.class
    );
    // Deliver the message with an invalid MAC
    onFairLossDeliver.setAccessible(true); // Make the private method accessible
    // Deliver the same message twice
    onFairLossDeliver.invoke(receiver, "127.0.0.1", 5011, "1|1|" + "Message 1" + "|" + invalidMAC); // Message 1 
    // Verify that the message is not delivered
    verify(receiver.getDeliverCallback(), never()).deliver(anyInt(), anyString());
}*/
/*@Test
public void testTamperedMAC() throws Exception {
    // Simulate a valid message
    String validMAC = CryptoUtils.generateMAC(privateKeySender, publicKeyRec, "1|1|Message 1");

    // Tamper with the MAC
    String tamperedMAC = validMAC.substring(0, validMAC.length() - 1) + "X"; // Change the last character

    // Deliver the message with a tampered MAC
    receiver.onFairLossDeliver("127.0.0.1", 6001, "1|1|Message 1|" + tamperedMAC);

    // Verify that the message is not delivered
    verify(receiver.getDeliverCallback(), never()).deliver(anyInt(), anyString());
}*/

/*@Test
public void testTamperedPayload() throws Exception {
    // Simulate a valid message
    String validMAC = CryptoUtils.generateMAC(privateKeySender, publicKeyRec, "1|1|Message 1");

    // Tamper with the payload
    String tamperedMessage = "1|1|Tampered Message|" + validMAC;

    // Deliver the message with a tampered payload
    receiver.onFairLossDeliver("127.0.0.1", 6001, tamperedMessage);

    // Verify that the message is not delivered
    verify(receiver.getDeliverCallback(), never()).deliver(anyInt(), anyString());
}*/
}
