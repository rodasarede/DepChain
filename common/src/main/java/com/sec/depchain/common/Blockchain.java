package com.sec.depchain.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.*;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import com.sec.depchain.common.SmartContractsUtil.helpers;
import com.sec.depchain.common.util.Constants;




public class Blockchain {

    private static final int DEBUG_MODE = 1;

    private final List<Block> chain;
    private final List<Transaction> pendingTransactions;
    private final Map<Address, AccountState> currentState;
    private final SimpleWorld simpleWorld;
    private final EVMExecutor executor;
    private final ByteArrayOutputStream byteArrayOutputStream;

    public Blockchain() {
       
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        this.currentState = new HashMap<>();
        this.simpleWorld = new SimpleWorld();

        //node tracer
        this.byteArrayOutputStream = new ByteArrayOutputStream();

        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        //initialize local EVM node
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        
        Block genesisBlock = new Block(Constants.GENESIS_BLOCK_FILE);
        this.chain.add(genesisBlock);
        this.currentState.putAll(genesisBlock.getState());

        // initialize simpleworld state and update it with currentstate

        updateSimpleWorldState();

        // ISTCoin contract creation bytecode from contract address code
        String Bytecode = simpleWorld.get(Address.fromHexString("0x1234567891234567891234567891234567891234")).getCode().toHexString();
       
        executor.code(Bytes.fromHexString(Bytecode));
        //contract admin with all supply
        executor.sender(Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"));
        executor.receiver(Address.fromHexString("1234567891234567891234567891234567891234"));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        String runtimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);
        //runtime bytecode
        executor.code(Bytes.fromHexString(runtimeBytecode));

        //test call smart contract
        // executor.callData(Bytes.fromHexString("95d89b41"));
        // executor.execute();
        // String tokenSymbol = helpers.extractStringFromReturnData(byteArrayOutputStream);
        // System.out.println("Output of 'symbol()': " + tokenSymbol);
    }

    public  void addTransaction(Transaction tx) {
        pendingTransactions.add(tx);
    }

    public  List<Block> getChain() {
        return chain;
    }
    public  Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }
    public  Map<Address, AccountState> getCurrentState() {
        return currentState;
    }
    public int getChainSize() {
        return chain.size();
    }

    public void updateSimpleWorldState() {

        for (Map.Entry<Address, AccountState> entry : getCurrentState().entrySet()) {
            Address address = entry.getKey();
            AccountState accountState = entry.getValue();
            BigInteger balance = accountState.getBalance();
            
            if (accountState.getCode() != null) {
                // Contract account
                Bytes contractCode = Bytes.fromHexString(accountState.getCode());
                MutableAccount contractAccount;
                if(simpleWorld.get(address) == null){
                    System.out.println("Creating new account");
                    simpleWorld.createAccount(address, 0, Wei.of(balance));
                    contractAccount = (MutableAccount) simpleWorld.get(address);
                }else{
                    contractAccount = (MutableAccount) simpleWorld.get(address);
                    contractAccount.setBalance(Wei.of(balance));
                }
                
                contractAccount.setCode(contractCode);
                
                // Handle storage if present
                if (accountState.getStorage() != null) {
                    for (Map.Entry<String, String> storageEntry : accountState.getStorage().entrySet()) {
                        UInt256 key = UInt256.fromBytes(Bytes32.fromHexString(storageEntry.getKey()));
                        UInt256 value = UInt256.fromBytes(Bytes32.fromHexString(storageEntry.getValue()));
                        contractAccount.setStorageValue(key, value);
                    }
                }
            } else {
                // Externally Owned Account (EOA)
                if(simpleWorld.get(address) == null){
                    simpleWorld.createAccount(address, 0, Wei.of(balance));
                }else{
                    MutableAccount eoaAccount = (MutableAccount) simpleWorld.get(address);
                    eoaAccount.setBalance(Wei.of(balance));
                }
                
            }
        }

    }

    /*public Block mineBlock(){
        //TODO
        Block latestBlock = getLatestBlock();
        Map<Address, AccountState> newState = new HashMap<>(currentState);
        for(Transaction tx: pendingTransactions){
            //apply transactions
        }
        Block newBlock = new Block(null, pendingTransactions, newState);

        this.chain.add(newBlock);

        currentState.clear();
        currentState.putAll(newState);
        pendingTransactions.clear();
        updateSimpleWorldState();
        return newBlock;
    }*/
}

