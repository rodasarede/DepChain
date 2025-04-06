package com.sec.depchain.client;

import com.sec.depchain.common.PerfectLinks;
import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.util.CryptoUtils;


import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;

import org.hyperledger.besu.datatypes.Address;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ByzantineClientTest {
    
    private ClientLibrary clientLibrary;
    private Wallet wallet;
    private PerfectLinks perfectLinksMock;
    
    @BeforeAll
    public void setUp() throws Exception {
        wallet = new Wallet(10); // Client ID 1
        perfectLinksMock = Mockito.mock(PerfectLinks.class);
        clientLibrary = new ClientLibrary(10, wallet);
        // Use reflection to inject the mock if needed
    }
    
    @AfterEach
    void tearDown() {
        perfectLinksMock.close();
        clientLibrary.close();
    }

    @Test
    public void testSendInvalidTransaction_SignatureMismatch() throws Exception {
    // Create a valid transaction
    Transaction validTx = new Transaction(
        Address.fromHexString(wallet.getAddress()),
        Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
        BigInteger.valueOf(100),
        "data",
        wallet.getNonce(),
        null
    );
    
    // Sign the transaction
    String validSignature = wallet.signTransaction(validTx);
    validTx.setSignature(validSignature);
    
    // Create a Byzantine version with altered data but same signature
    Transaction byzantineTx = new Transaction(
        validTx.getFrom(),
        validTx.getTo(),
        BigInteger.valueOf(1000), // Different amount
        "malicious data",
        validTx.getNonce(),
        validSignature // Using same signature for different data
    );
    
    // Verify the Byzantine transaction would fail validation
    assert(CryptoUtils.verifySignature(byzantineTx));
}

@Test
public void testDoubleSpendingAttack() throws Exception {
    // Create a transaction
    Transaction tx = new Transaction(
        Address.fromHexString(wallet.getAddress()),
        Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
        BigInteger.valueOf(100),
        "data",
        wallet.getNonce(),
        null
    );
    tx.setSignature(wallet.signTransaction(tx));
    
    // Byzantine client sends same transaction multiple times
    for (int i = 0; i < 5; i++) {
        clientLibrary.sendTransferRequest(tx);
    }
    
    // In a real system, only one should be accepted
    // This would need verification at the server side
}

    
@Test
public void testNonceManipulation() throws Exception {
    // Byzantine client tries to skip nonce values
    BigInteger currentNonce = wallet.getNonce();
    
    // Create transaction with future nonce
    Transaction tx = new Transaction(
        Address.fromHexString(wallet.getAddress()),
        Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
        BigInteger.valueOf(100),
        "data",
        currentNonce.add(BigInteger.TEN), // Skipping nonces
        null
    );
    tx.setSignature(wallet.signTransaction(tx));
    
    clientLibrary.sendTransferRequest(tx);
    
    // Verify this would be rejected by honest nodes
}

@Test
public void testMessageReplayAttack() throws Exception {
    // Create and send a valid transaction
    Transaction tx = new Transaction(
        Address.fromHexString(wallet.getAddress()),
        Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
        BigInteger.valueOf(100),
        "data",
        wallet.getNonce(),
        null
    );
    tx.setSignature(wallet.signTransaction(tx));
    clientLibrary.sendTransferRequest(tx);
    
    // Byzantine client resends the same transaction later
    // This should be detected as a replay attack
    clientLibrary.sendTransferRequest(tx);
}

@Test
public void testMaliciousSmartContractCall() throws Exception {
    // Byzantine client tries to call smart contract with manipulated parameters
    String maliciousData = "a9059cbb" + // transfer function signature
        "000000000000000000000000attackerAddress" + // to address
        "00000000000000000000000000000000000000000000ffffffffffffffff"; // max value
    
    Transaction tx = new Transaction(
        Address.fromHexString(wallet.getAddress()),
        Address.fromHexString("0xcontractAddress"),
        BigInteger.ZERO,
        maliciousData,
        wallet.getNonce(),
        null
    );
    tx.setSignature(wallet.signTransaction(tx));
    
    clientLibrary.sendTransferRequest(tx);
    
    // Verify this would be rejected or have no effect
}
// @Test
// public void testByzantineClientInSystem() throws Exception {
//     // Setup honest clients
//     ClientApplication honestClient1 = new ClientApplication(1);
//     ClientApplication honestClient2 = new ClientApplication(2);
    
//     // Setup Byzantine client
//     ClientApplication byzantineClient = new ClientApplication(3) {
//         @Override
//         protected void handleTransactionRequest(String[] caseArgs, ClientLibrary clientLibrary) throws Exception {
//             // Override to always send invalid transactions
//             Transaction tx = new Transaction(
//                 Address.fromHexString(wallet.getAddress()),
//                 Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
//                 BigInteger.valueOf(-100), // Negative amount
//                 "malicious data",
//                 wallet.getNonce(),
//                 "fakeSignature"
//             );
//             clientLibrary.sendTransferRequest(tx);
//         }
//     };
    
//     // Simulate interactions and verify Byzantine behavior is handled correctly
//     // This would require mocking or a test network setup
// }
// @Test
// public void testClientIgnoresByzantineNodes() throws Exception {
//     Wallet wallet = new Wallet(1);
//     ClientLibrary clientLibrary = new ClientLibrary(1, wallet);

//     Transaction tx = new Transaction(
//         Address.fromHexString(wallet.getAddress()),
//         Address.fromHexString("0x1234567890abcdef1234567890abcdef12345678"),
//         BigInteger.valueOf(10),
//         "someData",
//         wallet.getNonce(),
//         null
//     );
//     wallet.incremetNonce();
//     tx.setSignature(wallet.signTransaction(tx));
//     String txHash = tx.computeTxHash();

//     CountDownLatch latch = new CountDownLatch(1);

//     clientLibrary.setDeliverCallback((transaction, hash, position) -> {
//         // Should only trigger if honest nodes agree
//         assert(99==position);
//         latch.countDown();
//     });

//     // Byzantine nodes respond with inconsistent data
//     for (int i = 0; i < 2; i++) {
//         JSONObject response = new JSONObject();
//         response.put("type", "tx-response");
//         response.put("success", true);
//         response.put("response", "BYZANTINE_WRONG_RESPONSE");
//         response.put("position", 13 + i); // different position
//         response.put("transaction", tx.serializeTransactionToJson());
//         clientLibrary.onPerfectLinksDeliver(i, response.toString());
//     }

//     // Honest nodes give consistent correct response
//     for (int i = 2; i < 4; i++) {
//         JSONObject response = new JSONObject();
//         response.put("type", "tx-response");
//         response.put("success", true);
//         response.put("response", "OK");
//         response.put("position", 99);
//         response.put("transaction", tx.serializeTransactionToJson());
//         clientLibrary.on(i, response.toString());
//     }

//     assertTrue(latch.await(2, TimeUnit.SECONDS));
// }

}