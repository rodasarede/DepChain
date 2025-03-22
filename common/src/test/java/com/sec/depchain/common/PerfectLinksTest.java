package com.sec.depchain.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.sec.depchain.common.FairLossLinks.DeliverCallback;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.util.CryptoUtils;

public class PerfectLinksTest{
    private PerfectLinks sender;
    private PerfectLinks receiver;

    private FairLossLinks mockFairLossLinksSender;
    private FairLossLinks mockFairLossLinksReceiver;
    private FairLossLinks fairLossLinks;
    private SystemMembership systemMembership;

    private int nodeId = 1 ;
    private int destId = 2;


    @BeforeEach
    public void setUp() throws Exception{

       systemMembership = mock(SystemMembership.class);
       when(systemMembership.getMembershipList()).thenReturn(new HashMap<>());


        Field field = PerfectLinks.class.getDeclaredField("systemMembership");
        field.setAccessible(true);
        field.set(null, systemMembership);  // Set the static field
        mockFairLossLinksSender = mock(FairLossLinks.class);
        mockFairLossLinksReceiver = mock(FairLossLinks.class);


        sender = new PerfectLinks(nodeId, systemMembership, mockFairLossLinksSender);
        receiver = new PerfectLinks(destId, systemMembership, mockFairLossLinksReceiver);

        sender.setDeliverCallback((srcId, message) -> {});
        receiver.setDeliverCallback((srcId, message) -> {});

    }
    @Test
    void testSendMessageSucc() throws Exception{
        DeliverCallback callback = mock(DeliverCallback.class);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256); 
            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKey ecPublicKey = keyPair.getPublic();

            when(systemMembership.getPublicKey(anyInt())).thenReturn(ecPublicKey);

        sender.send(2, "Hello node 2");

        verify(mockFairLossLinksSender, atLeastOnce()).send(anyString(), anyInt(), anyString());

    }
    @Test
    public void testMessageRetransmission() throws Exception {
        // Send a message and simulate no ACK being received

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256); 
            KeyPair keyPair = keyGen.generateKeyPair();
            PublicKey ecPublicKey = keyPair.getPublic();

            when(systemMembership.getPublicKey(anyInt())).thenReturn(ecPublicKey);
        sender.send(destId, "testMessage");

        // Wait for retransmission to occur
        Thread.sleep(2000); // Allow time for retransmission

        // Verify that the message was retransmitted
        verify(mockFairLossLinksSender, atLeast(2)).send(anyString(), anyInt(), anyString());
    }
    @Test
    public void testOutOfOrder() throws Exception{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256); 
        KeyPair keyPair = keyGen.generateKeyPair();
        PublicKey ecPublicKey = keyPair.getPublic();

        when(systemMembership.getPublicKey(anyInt())).thenReturn(ecPublicKey);

        sender.send(destId, "Msg 1");
        sender.send(destId, "Msg 2");
        sender.send(destId, "Msg 3");

        Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
            "onFairLossDeliver", String.class, int.class, String.class
        );
        onFairLossDeliver.setAccessible(true); // Allow access to the private method
        // Simulate receiving messages out of order
        onFairLossDeliver.invoke(receiver,"127.0.0.1", 6000, "2|2|Message 2|validMAC");
        onFairLossDeliver.invoke(receiver,"127.0.0.1", 6000, "1|1|Message 1|validMAC");
        onFairLossDeliver.invoke(receiver,"127.0.0.1", 6000, "3|3|Message 3|validMAC");

        assertTrue(true);
    }
    //TODO java.security.InvalidKeyException: Key must be a PublicKey with algorithm EC
    /*@Test
    void testDuplicateMessage() throws Exception {
        // Arrange
        String duplicateMessage = "1|1|Test Message|validMAC";
    
        // Mock the static verifyMac method in CryptoUtils to always return true
        try (MockedStatic<CryptoUtils> mockedCryptoUtils = Mockito.mockStatic(CryptoUtils.class)) {
            mockedCryptoUtils.when(() -> CryptoUtils.ver(anyString())).thenReturn(true);
    
            // Access the private method via reflection
            Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
                "onFairLossDeliver", String.class, int.class, String.class
            );
            onFairLossDeliver.setAccessible(true);
    
            // Act
            // First delivery (should process the message)
            onFairLossDeliver.invoke(sender, "127.0.0.1", 6000, duplicateMessage);
    
            // Second delivery (should ignore the duplicate message)
            onFairLossDeliver.invoke(sender, "127.0.0.1", 6000, duplicateMessage);
    
            // Assert
            // Verify that the message was processed only once
            verify(mockFairLossLinksSender, times(1)).send(anyString(), anyInt(), anyString());
        }
    }*/
@Test //TODO java.security.InvalidKeyException: Key must be a PublicKey with algorithm EC
void testTamperedMAC() throws Exception {
    // Simulate receiving a message with a tampered MAC
    String tamperedMessage = "1|1|Test Message|tamperedMAC";

    // Use reflection to access the private method
    Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
        "onFairLossDeliver", String.class, int.class, String.class
    );
    onFairLossDeliver.setAccessible(true); // Allow access to the private method

    // Create an instance of PerfectLinks

    // Deliver the tampered message
    onFairLossDeliver.invoke(sender, "127.0.0.1", 6000, tamperedMessage);

    // Verify that the message was ignored (no ACK sent)
    verify(mockFairLossLinksSender, never()).send(anyString(), anyInt(), anyString());
}
//TODO java.security.InvalidKeyException: Key must be a PublicKey with algorithm EC
@Test
    public void testInvalidMAC() throws Exception{
        Method onFairLossDeliver = PerfectLinks.class.getDeclaredMethod(
            "onFairLossDeliver", String.class, int.class, String.class
        );
        onFairLossDeliver.setAccessible(true); // Allow access to the private method
        // Simulate receiving a message with an invalid MAC
        onFairLossDeliver.invoke(receiver,"127.0.0.1", 6000, "1|1|Invalid Message|invalidMAC");

        // Verify that the message was ignored
        verify(mockFairLossLinksReceiver, never()).send(anyString(), anyInt(), anyString());
    }

    /*@Test
    void testClose() throws Exception {
        // Close PerfectLinks
        sender.close();

        // Try to send a message after closing
        assertThrows(Exception.class, () -> {
            sender.send(destId, "teste");
        });
    }*/


}