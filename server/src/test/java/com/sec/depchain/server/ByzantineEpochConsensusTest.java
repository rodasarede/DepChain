package com.sec.depchain.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ByzantineEpochConsensusTest {
    private static int LEADER_ID = 1;
    private SystemMembership systemMembership;
    private PerfectLinks perfectLinks;
    private ByzantineEpochConsensus bep;
    private BlockchainMember[] nodes;
    private static int N_NODES = 4;
    @BeforeAll
    void setup() throws Exception{
        System.setProperty("sun.net.client.defaultConnectTimeout", "2000");
        System.setProperty("sun.net.client.defaultReadTimeout", "2000");
        systemMembership = Mockito.mock(SystemMembership.class);
        when(systemMembership.getNumberOfNodes()).thenReturn(N_NODES);
        when(systemMembership.getLeaderId()).thenReturn(LEADER_ID);
        BlockchainMember.setSystemMembership(systemMembership);
        perfectLinks = Mockito.mock(PerfectLinks.class); // Initialize PerfectLinks mock
        nodes = new BlockchainMember[N_NODES];
        for (int i = 0; i < N_NODES; i++) {
            nodes[i] = new BlockchainMember(i + 1);
            nodes[i].setPerfectLinks(perfectLinks); // Use the same PerfectLinks mock for all nodes 
            nodes[i].start();
        }
    }
    
@AfterAll

    void tearDown() throws Exception {
        for(int i=0 ; i < N_NODES; i++)
        {
            nodes[i].cleanup();
        }
    }

    @Test
void onlyLeaderSendsReadMessages() throws Exception {
    String message = "<append-request:transaction1>";
    int clientId = 10;

    // Simulate broadcasting "APPEND" to all nodes
    for (BlockchainMember node : nodes) {
        System.out.println("Node " + node.getId() + " processing message: " + message);
        node.onPerfectLinksDeliver(clientId, message); // Directly call the method
    }

    // Verify that only the leader sends READ messages
    for (BlockchainMember node : nodes) {
        if (node.isLeader()) {
            System.out.println("Verifying READ messages for Leader Node " + node.getId());
            for (int nodeId : systemMembership.getMembershipList().keySet()) {
                verify(perfectLinks, times(1)).send(nodeId, anyString());
            }
        } else {
            System.out.println("Verifying no READ messages for Non-Leader Node " + node.getId());
            verify(perfectLinks, never()).send(anyInt(), anyString());
        }
    }
}
@Test
public void testDeliverRead_LeaderMessage_TriggersCollector() throws Exception {
    // Arrange

    ConditionalCollect cc = Mockito.mock(ConditionalCollect.class);
    int senderId = LEADER_ID; // Sender is the leader
    String message =  "<READ:0:" + senderId + ">";
    // Act
    nodes[2].getBep().setCc(cc);

    nodes[2].onPerfectLinksDeliver(senderId, message);
    // Assert
    verify(cc).onInit(); // Verify that onInit() is called
    verify(cc).input(anyString()); // Verify that input() is called with any string
}

@Test
public void testDeliverRead_NonLeaderMessage_DoesNotTriggerCollector() throws Exception {
    // Arrange
    
    int senderId = 2; // Sender is not the leader
    String message =  "<READ:0:" + senderId + ">";

    ConditionalCollect cc = Mockito.mock(ConditionalCollect.class);
    // Act
    nodes[2].getBep().setCc(cc);

    nodes[2].onPerfectLinksDeliver(senderId, message);
    // Assert
    verify(cc, never()).onInit(); // Verify that onInit() is never called
    verify(cc, never()).input(anyString()); // Verify that input() is never called
}

/*@Test
public void testSuccessConsensus() throws Exception
{
    String message = "<append-request:transaction1>";
    int clientId = 10;

    // Simulate broadcasting "APPEND" to all nodes
    for (BlockchainMember node : nodes) {
        System.out.println("Node " + node.getId() + " processing message: " + message);
        node.onPerfectLinksDeliver(clientId, message); // Directly call the method
    }
}
    @Test
    void testWriteQuorum() {
        bep = nodes[0].getBep();
        bep.deliverWrite(1, "value1");
        bep.deliverWrite(2, "value1");
        bep.deliverWrite(3, "value1");

        // Verify that a quorum was reached
        assertTrue(bep.getState().getValtsVal().getVal().equals("value1"), "Quorum did not reach expected value");

    }

    @Test
    void testWithDroppedMessages() throws Exception {
        bep = nodes[0].getBep();
    
        // Simulating WRITE messages, but dropping one
        bep.deliverWrite(1, "value1");
        bep.deliverWrite(2, "value1");
        // Node 3’s message is missing (simulating network delay or loss)
    
        // Verify that consensus is still reached
        assertTrue(bep.getState().getValtsVal().getVal().equals("value1"), "Consensus failed due to dropped messages");
    }

    @Test
void testConsensusWithOneIncorrectNode() throws Exception {
    bep = nodes[0].getBep();

    // Simulating correct WRITE messages from two nodes
    bep.deliverWrite(1, "value1");
    bep.deliverWrite(2, "value1");

    // Simulating one node sending an incorrect value
    bep.deliverWrite(3, "wrong_value");

    // Verify that consensus is still reached on the correct value
    assertTrue(bep.getState().getValtsVal().getVal().equals("value1"), "Consensus failed due to one incorrect node");
}*/
}
     

    