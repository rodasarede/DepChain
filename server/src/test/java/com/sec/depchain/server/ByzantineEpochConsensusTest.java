package com.sec.depchain.server;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.SystemMembership;

public class ByzantineEpochConsensusTest {
    private static int LEADER_ID = 1;
    private SystemMembership systemMembership;
    private PerfectLinks perfectLinks;
    private ByzantineEpochConsensus bep;
    private BlockchainMember[] nodes;
    private static int N_NODES = 4;
    @BeforeEach
    void setup() throws Exception{
        systemMembership = Mockito.mock(SystemMembership.class);
        when(systemMembership.getNumberOfNodes()).thenReturn(N_NODES);
        when(systemMembership.getLeaderId()).thenReturn(LEADER_ID);
        BlockchainMember.setSystemMembership(systemMembership);
        perfectLinks = Mockito.mock(PerfectLinks.class); // Initialize PerfectLinks mock
        nodes = new BlockchainMember[N_NODES];
        for (int i = 0; i < N_NODES; i++) {
            nodes[i] = new BlockchainMember(i + 1);
            nodes[i].setPerfectLinks(perfectLinks); // Use the same PerfectLinks mock for all nodes
        }
    }
    @AfterEach
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
}
     

