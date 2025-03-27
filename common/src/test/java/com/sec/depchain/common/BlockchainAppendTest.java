package com.sec.depchain.common;

import org.junit.jupiter.api.Test;

import com.sec.depchain.common.SmartContractsUtil.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.BeforeAll;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;


public class BlockchainAppendTest {

    private static Blockchain blockchain;
    private static SimpleWorld simpleWorld;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;
    
    @BeforeAll
    public static void setup(){
        System.out.println("Load first block(genesis block) in json into the blockchain");

        //initialize local blockchain with genesis block
        blockchain = new Blockchain();

        testGenesisBlock();

        //initialize local EVM node
        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        
        simpleWorld = new SimpleWorld();
        for (Map.Entry<Address, AccountState> entry : blockchain.getCurrentState().entrySet()) {
            Address address = entry.getKey();
            AccountState accountState = entry.getValue();
            BigInteger balance = accountState.getBalance();
            
            if (accountState.getCode() != null) {
                // Contract account
                Bytes contractCode = Bytes.fromHexString(accountState.getCode());
                simpleWorld.createAccount(address, 0, Wei.of(balance));
                MutableAccount contractAccount = (MutableAccount) simpleWorld.get(address);
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
                simpleWorld.createAccount(address, 0, Wei.of(balance));
            }
        }
        // get ISTCoin contract creation bytecode 
        String Bytecode = simpleWorld.get(Address.fromHexString("0x1234567891234567891234567891234567891234")).getCode().toHexString();
       

        executor.code(Bytes.fromHexString(Bytecode));
        executor.sender(Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"));
        executor.receiver(Address.fromHexString("1234567891234567891234567891234567891234"));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        String runtimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);
        //runtime bytecode
        executor.code(Bytes.fromHexString(runtimeBytecode));

        executor.callData(Bytes.fromHexString("95d89b41"));
        executor.execute();
        String tokenSymbol = helpers.extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'symbol()': " + tokenSymbol);

        assert(tokenSymbol.equals("IST"));





    }


    public static void testGenesisBlock(){
        System.out.println("Check genesis block details");

        Block currentBlock = blockchain.getLatestBlock();
        currentBlock.printBlockDetails();

    } 

    @Test 
    public void appendBlockWithNativeTransferTx(){
        assert(true);



    }





}
