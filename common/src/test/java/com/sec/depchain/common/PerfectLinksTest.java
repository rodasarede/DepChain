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
    
}
