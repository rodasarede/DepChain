package com.sec.depchain.server;

import com.sec.depchain.common.Transaction;
import java.math.BigInteger;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.besu.datatypes.Address;
import org.json.JSONObject;

import com.sec.depchain.common.SystemMembership;
import com.sec.depchain.common.util.Constants;
import com.sec.depchain.common.Block;
import com.sec.depchain.common.Blockchain;
import com.sec.depchain.common.PerfectLinks;

/**
 * Represents a blockchain member node in the Byzantine fault-tolerant system.
 */
public class BlockchainMember {
    private int id;
    private boolean isLeader = false; // Should start as false
    private static SystemMembership systemMembership;
    private PerfectLinks perfectLinks;
    private ByzantineEpochConsensus bep;
    private Map<Integer, String> clientTransactions = new ConcurrentHashMap<>();
    private static Blockchain blockchain;
    // private Mempool mempool;
    private MempoolFifo mempoolFifo;
    private int DEBUG_MODE = 1;
    private static final int LEADER_VALUE_TEST = 0;
    private ByzantineEpochConsensus bepBlock;
    private Timer consensusTimer;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("BLOCKCHAIN MEMBER - ERROR: Usage: <id>");
            return;
        }
        int nodeId = Integer.parseInt(args[0]);
        systemMembership = new SystemMembership(Constants.PROPERTIES_PATH);
        BlockchainMember node = new BlockchainMember(nodeId);
        node.start();
    }

    public BlockchainMember(int id) throws Exception {
        this.id = id;
        this.isLeader = (id == systemMembership.getLeaderId());
        this.blockchain = new Blockchain();
        // this.mempool = new Mempool();
        this.mempoolFifo = new MempoolFifo();
        if (DEBUG_MODE == 1) {
            System.out
                    .println("BLOCKCHAIN MEMBER - DEBUG: Initialized with ID {" + id + "}, Leader: {" + isLeader + "}");
        }
        this.perfectLinks = new PerfectLinks(id);

        perfectLinks.setDeliverCallbackCollect((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                System.out.println("BLOCKCHAIN MEMBER - ERROR: Exception in deliverCallbackCollect: " + e);
            }
        });
        perfectLinks.setDeliverCallback((nodeId, message) -> {
            try {
                onPerfectLinksDeliver(nodeId, message);
            } catch (Exception e) {
                System.out.println("BLOCKCHAIN MEMBER - ERROR: Exception in deliverCallback:" + e);
            }
        });
        this.bepBlock = new ByzantineEpochConsensus(systemMembership.getLeaderId(), 0, perfectLinks, systemMembership, id, this, blockchain);
    }

    public void start() throws Exception {
        bepBlock.init();
    }

    public void onPerfectLinksDeliver(int senderId, String message) throws Exception {
        if (DEBUG_MODE == 1) {
            System.out.println(
                    "BLOCKCHAIN MEMBER - DEBUG: Received message from {" + senderId + "} -> {" + message + "}");
        }

        JSONObject json = new JSONObject(message.trim()); 
        String messageType = "";
        if (json.has("type")) {
            messageType = json.getString("type"); // Returns "tx-request"
        }
        switch (messageType) {
            case Constants.MessageType.TX_REQUEST:
                Transaction tx = deserializeTransactionJson(message);
                tx.setClientId(senderId);
                if (!tx.isValid(blockchain, true)) {
                    System.out.println("BLOCKCHAIN MEMBER - ERROR: Invalid transaction signature from client {"
                            + senderId + "}: {" + tx.computeTxHash() + "}");
                    tx.setResponse("Transaction not valid");
                    JSONObject responseMessage = Formatter.formatTx_ResponseMessage(tx, null);
                    perfectLinks.send(senderId, responseMessage.toString());
                    break;
                }
                if(id == systemMembership.getLeaderId()) {
                    // change the tx value
                    if (LEADER_VALUE_TEST == 1 && isLeader()) {
                        System.out.println("BLOCKCHAIN MEMBER - LEADER VALUE TEST: old tx value: " + tx.getValue() );
                        // value = value.add(BigInteger.ONE);
                        tx.setValue(tx.getValue().add(BigInteger.ONE));
                        System.out.println("BLOCKCHAIN MEMBER - LEADER VALUE TEST: new tx value: " + tx.getValue() );
                    }
                    mempoolFifo.addTransactionToMempool(tx);
                    startConsensusTimer();
                }
                if (mempoolFifo.size() >= Constants.THRESHOLD) { 
                    cancelConsensusTimer();
                    triggerConsensus();
                }

                break;
            case Constants.MessageType.READ:
                bepBlock.deliverRead(senderId);
                break;
            case Constants.MessageType.WRITE:

                JSONObject messageToJson = new JSONObject(message);
                JSONObject blockJson = messageToJson.getJSONObject("value");
                Block v = Formatter.jsonToBlock(blockJson);
                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: WRITE: bep.deliverWrite(senderId:{" + senderId
                            + "}, value:{" + v.calculateHash() + "})");
                }
                bepBlock.deliverWrite(senderId, v);
                break;
            case Constants.MessageType.ACCEPT:
                messageToJson = new JSONObject(message);
                blockJson = messageToJson.getJSONObject("value");
                v = Formatter.jsonToBlock(blockJson);
                if (DEBUG_MODE == 1) {

                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: ACCEPT: bep.deliverAccept(senderId:{" + senderId
                            + "}, value:{" + v.calculateHash() + "})");
                }
                bepBlock.deliverAccept(senderId, v);
                break;
            default:
                if (DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: Unknown message type -> {" + message + "}");
                }
                break;
        }
    }

    private void startConsensusTimer() {
        if (consensusTimer != null) {
            consensusTimer.cancel(); // Cancel previous timer if running
        }

        consensusTimer = new Timer();
        consensusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mempoolFifo.size() != 0) {
                    triggerConsensus();
                }
            }
        }, Constants.TIMEOUT_MS);
    }

    private void triggerConsensus() {
        if (mempoolFifo.size() == 0)
            return;
        List<Transaction> transactions = new ArrayList<>(mempoolFifo.getTransactions());

        Block newBlock = new Block(
                blockchain.getLatestBlock().getBlockHash(),
                transactions,
                blockchain.getLatestBlock().getHeight() + 1);

        if (DEBUG_MODE == 1) {
            System.out.println("BLOCKCHAIN MEMBER - DEBUG: Proposing block {" + newBlock.getBlockHash() + "}");
        }
        if (newBlock.getTimestamp() > blockchain.getLatestBlock().getTimestamp()) {
            bepBlock.propose(newBlock);
            //startConsensusTimer();
        }
    }
    private void cancelConsensusTimer() {
        if (consensusTimer != null) {
            consensusTimer.cancel();
            consensusTimer = null;
        }
    }

    public void decideBlock(Block block) {
        System.out.println("block decided");

        if (block.getTimestamp() > blockchain.getLatestBlock().getTimestamp()) {
            blockchain.getChain().add(block);
            String blockname = block.getHeight()+ ".json";
            String blockPath = Constants.BLOCKS_DIR + "/blockchain_" + id;
            
            
            // 2. Execute transactions to update the world state
            for (Transaction transaction : block.getTransactions()) {
                transaction.execute(blockchain);
                int clientId = transaction.getClientId();
                JSONObject responseMessage = Formatter.formatTx_ResponseMessage(transaction, block);

                if(DEBUG_MODE == 1) {
                    System.out.println("BLOCKCHAIN MEMBER - DEBUG: Sending response to client {" + clientId + "}: {"
                            + responseMessage.toString() + "}");
                }
                perfectLinks.send(clientId, responseMessage.toString());
            }

            
            // blockchain.printAccountsInfo();

            // 3. Remove included transactions from mempool
            for (Transaction transaction : block.getTransactions()) {
                mempoolFifo.removeTransactionFromMempool(transaction);
            }
            if (DEBUG_MODE == 1) {
                System.out.println("BLOCKCHAIN MEMBER - DEBUG: Mempool size after commit: {" + mempoolFifo.size() + "}");
            }
            // 4. Persist the block to JSON
            block.persistBlockToJson( blockPath, blockname);
            // 5. reset for next consensus
            bepBlock.init();
            
        }
        
        
    }


    public boolean isLeader() {
        return isLeader;
    }

    public void setBep(ByzantineEpochConsensus bep) {
        this.bep = bep;
    }

    public Map<Integer, String> getClientTransactions() {
        return clientTransactions;
    }

    public int getId() {
        return id;
    }

    public ByzantineEpochConsensus getBep() {
        return bep;
    }

    public void setPerfectLinks(PerfectLinks perfectLinks) {
        this.perfectLinks = perfectLinks;
    }

    public static void setSystemMembership(SystemMembership membership) {
        systemMembership = membership;
    }

    public PerfectLinks getPerfectLinks() {
        return perfectLinks;
    }

    public void cleanup() {
        this.perfectLinks.close();
    }

    public Transaction deserializeTransactionJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            JSONObject txJson = json.getJSONObject("transaction");

            // Extract and convert fields
            String senderAddress = txJson.getString("from");
            String toAddress = txJson.getString("to");
            BigInteger value;
            if (txJson.get("amount") instanceof Integer) {
                value = BigInteger.valueOf(txJson.getInt("amount"));
            } else {
                value = new BigInteger(txJson.getString("amount"));
            }
            String data = txJson.getString("data");
            BigInteger nonce;
            if (txJson.get("nonce") instanceof Integer) {
                nonce = BigInteger.valueOf(txJson.getInt("nonce"));
            } else {
                nonce = new BigInteger(txJson.getString("nonce"));
            }
            String signature = txJson.getString("signature"); // Note: Typo in your JSON? Should be "signature"

            // Using current timestamp since it's not in the JSON

            return new Transaction(Address.fromHexString(senderAddress), Address.fromHexString(toAddress), value, data,
                    nonce, signature);

        } catch (Exception e) {
            System.err.println("Failed to deserialize transaction: " + e.getMessage());
            return null;
        }
    }

    public Blockchain getblockchain() {
        return blockchain;
    }

}
