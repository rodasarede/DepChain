package com.sec.depchain.server;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BlockChainMemberTest {
    private PerfectLinks perfectLinks;
    private SystemMembership systemMembership;
    private ByzantineEpochConsensus bep;
    private BlockchainMember blockchainMember;
    private final static int SENDER_ID = 1;
    
    @BeforeEach
    void setUp()
    {
        blockchainMember = new BlockchainMember();
        systemMembership = Mockito.mock(SystemMembership.class);
        when(systemMembership.getNumberOfNodes()).thenReturn(4);
        when(systemMembership.getMaximumNumberOfByzantineNodes()).thenReturn(1);

        perfectLinks = Mockito.mock(PerfectLinks.class);
        bep = Mockito.mock(ByzantineEpochConsensus.class);

        blockchainMember.setBep(bep); // Certifica-se de usar o mock corretamente

        //BlockchainMember.clientTransactions = new HashMap<>();

    }

    //Test if its deciding its ok for a lot of decided transactions
    @Test
public void testHighLoadTransactions() throws Exception {

    for (int i = 0; i < 100; i++) {
        blockchainMember.decide("tx-" + i);
    }

    Thread.sleep(10000); // Allow time for consensus

    // Ensure all transactions are committed
    assertEquals(100, blockchainMember.getBlockchain().size());
}
private void invokeOnPerfectLinksDeliver(int senderId, String message) throws Exception {
    Method method = BlockchainMember.class.getDeclaredMethod("onPerfectLinksDeliver", int.class, String.class);
    method.setAccessible(true);
    method.invoke(null, senderId, message);  // Call the static method
}

@Test
public void testOnPerfectLinksDeliver_AppendRequest() throws Exception {
    String message = "<append-request:transaction1>";

    invokeOnPerfectLinksDeliver(SENDER_ID, message);

    // Verify transaction is stored
    assertEquals("transaction1", blockchainMember.getClientTransactions().get(SENDER_ID));

    // Verify propose is called
    verify(bep, times(1)).propose("transaction1");
}

@Test
public void testOnPerfectLinksDeliver_Read() throws Exception {
    String message = "<READ>";

    invokeOnPerfectLinksDeliver(SENDER_ID, message);

    // Verify deliverRead is called
    verify(bep, times(1)).deliverRead(SENDER_ID);
}

@Test

public void testOnPerfectLinksDeliver_Write() throws Exception {
    String message = "<WRITE:value1:valuetowrite>";

    invokeOnPerfectLinksDeliver(SENDER_ID, message);

    // Verify deliverWrite is called with correct value
    verify(bep, times(1)).deliverWrite(SENDER_ID, "valuetowrite");
}

@Test
public void testOnPerfectLinksDeliver_Accept() throws Exception {
    String message = "<ACCEPT:value2:valuetoaccept>";

    invokeOnPerfectLinksDeliver(SENDER_ID, message);

    // Verify deliverAccept is called with correct value
    verify(bep, times(1)).deliverAccept(SENDER_ID, "valuetoaccept");
}

@Test
public void testOnPerfectLinksDeliver_UnknownMessage() throws Exception {
    String message = "<UNKNOWN>";

    invokeOnPerfectLinksDeliver(SENDER_ID, message);

    // Ensure no interactions with bep for unknown message type
    verifyNoInteractions(bep);
}
}