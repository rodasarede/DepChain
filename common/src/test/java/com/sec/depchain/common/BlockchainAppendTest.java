package com.sec.depchain.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.BeforeAll;
import org.hyperledger.besu.datatypes.Address;


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

        //initialize local EVM node
        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);



    }

    @Test
    public void testGenesisBlock(){
        System.out.println("Check genesis block details");

        Block currentBlock = blockchain.getLatestBlock();
        currentBlock.printBlockDetails();

    } 

    @Test 
    public void appendBlockWithNativeTransferTx(){
        



    }





}
