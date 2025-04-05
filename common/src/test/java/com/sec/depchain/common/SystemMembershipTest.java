package com.sec.depchain.common;


import com.sec.depchain.common.util.Constants;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

class SystemMembershipTest {

    private SystemMembership systemMembership;

    private static final int NUMBER_OF_NODES = 4;
    private static final int LEADER_ID = 1;
    private static final int MAX_N_BYZANTINE_NODES = 1;
    
    @BeforeEach
    void setUp() {
        // Assuming "test_config.properties" is a valid configuration file for testing
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
    }

    @Test
    void testConstructorInitialization() {
        assertNotNull(systemMembership.getMembershipList());
        assertNotNull(systemMembership.getPublicKeys());
        assertEquals(LEADER_ID, systemMembership.getLeaderId()); // Assuming leaderId is 1 in the config file
        assertEquals(NUMBER_OF_NODES, systemMembership.getNumberOfNodes()); // Assuming 4 nodes in the config file
    }

    @Test
    void testNumberOfNodesValidation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            systemMembership.setNumberOfNodes(0);
        });
        assertEquals("Number of nodes must be at least 1.", exception.getMessage());
    }

    @Test
    void testMaximumNumberOfByzantineNodesCalculation() {
        // Test with 1 node
        systemMembership.setNumberOfNodes(1);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(0, systemMembership.getMaximumNumberOfByzantineNodes()); // (1-1)/3 = 0
    
        // Test with 3 nodes
        systemMembership.setNumberOfNodes(3);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(0, systemMembership.getMaximumNumberOfByzantineNodes()); // (3-1)/3 = 0.666 -> floors to 0
    
        // Test with 4 nodes
        systemMembership.setNumberOfNodes(4);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(1, systemMembership.getMaximumNumberOfByzantineNodes()); // (4-1)/3 = 1
    
        // Test with 5 nodes
        systemMembership.setNumberOfNodes(5);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(1, systemMembership.getMaximumNumberOfByzantineNodes()); // (5-1)/3 = 1.333 -> floors to 1
    
        // Test with 7 nodes
        systemMembership.setNumberOfNodes(7);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(2, systemMembership.getMaximumNumberOfByzantineNodes()); // (7-1)/3 = 2
    
        // Test with 10 nodes
        systemMembership.setNumberOfNodes(10);
        systemMembership.setMaximumNumberOfByzantineNodes(systemMembership.getNumberOfNodes());
        assertEquals(3, systemMembership.getMaximumNumberOfByzantineNodes()); // (10-1)/3 = 3
    
        // Test with 0 nodes (should throw an exception)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            systemMembership.setNumberOfNodes(0);
        });
        assertEquals("Number of nodes must be at least 1.", exception.getMessage());
    
        // Test with negative nodes (should throw an exception)
        exception = assertThrows(IllegalArgumentException.class, () -> {
            systemMembership.setNumberOfNodes(-1);
        });
        assertEquals("Number of nodes must be at least 1.", exception.getMessage());
    }

    @Test
    void testMembershipList() {
        HashMap<Integer, Member> membershipList = systemMembership.getMembershipList();
        assertNotNull(membershipList);
        assertEquals(NUMBER_OF_NODES, membershipList.size()); // Assuming 4 nodes in the config file
        Member member = membershipList.get(1);
        assertNotNull(member);
        assertEquals("127.0.0.1", member.getAddress()); // Assuming address is 127.0.0.1 for node 1
        assertEquals(5000, member.getPort());
    }

    @Test
    void testPublicKeys() {
        Map<Integer, PublicKey> publicKeys = systemMembership.getPublicKeys();
        assertNotNull(publicKeys);
        PublicKey publicKey = systemMembership.getPublicKey(1);
        assertNotNull(publicKey);
    }

    @Test
    void testLeaderId() {
        systemMembership.setLeaderId(2);
        assertEquals(2, systemMembership.getLeaderId());
    }


}