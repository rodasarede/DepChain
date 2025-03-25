package com.sec.depchain.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.Bool;

import com.sec.depchain.common.SmartContractsUtil.helpers;

public class BlacklistTest {

    private static SimpleWorld simpleWorld;
    private static Address senderAddress;
    private static Address BlacklistContractAddress;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;
    
    @BeforeAll
    public static void setup(){
        // simpleWorld to maintain account state
        simpleWorld = new SimpleWorld();

        // EOT mock account
        senderAddress = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        simpleWorld.createAccount(senderAddress,0, Wei.fromEth(100));

        // contract mock account
        BlacklistContractAddress = Address.fromHexString("1234567891234567891234567891234567891235");
        simpleWorld.createAccount(BlacklistContractAddress,0, Wei.fromEth(0));
        MutableAccount contractAccount = (MutableAccount) simpleWorld.get(BlacklistContractAddress);

        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        // initialize EVM local node
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);

        String BlacklistBytecode = helpers.loadBytecode("src/main/java/com/sec/depchain/resources/contracts_bytecode/Blacklist.bin");
        executor.code(Bytes.fromHexString(BlacklistBytecode));
        executor.contract(BlacklistContractAddress);
        executor.sender(senderAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        String BlacklistRuntimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);

        executor.code(Bytes.fromHexString(BlacklistRuntimeBytecode));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();
    }

    @Test
    //test if an address is blacklisted before and after beeing added
    public void testIsBlacklist(){

        executor.callData(Bytes.fromHexString("fe575a87"+ helpers.padHexStringTo256Bit(senderAddress.toString())));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());
        System.out.println("Is Blacklisted: "+ helpers.extractBooleanFromReturnData(byteArrayOutputStream));

        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();

        executor.callData(Bytes.fromHexString("fe575a87"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());
        Boolean isBlacklisted = helpers.extractBooleanFromReturnData(byteArrayOutputStream);
        System.out.println("Is Blacklisted: "+ isBlacklisted);

        assert(isBlacklisted);
    }

    @Test
    public void unauthorizedRemoveBlacklistTest(){
        executor.sender(BlacklistContractAddress);
        executor.callData(Bytes.fromHexString("537df3b6"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();
        String result = helpers.extractErrorMessage(byteArrayOutputStream);
        System.out.println("Returned: "+ result);

        assert(result.equals("Not authorized"));
    }

    @Test
    public void unauthorizedAddBlacklistTest(){
        executor.sender(BlacklistContractAddress);
        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();
        String result = helpers.extractErrorMessage(byteArrayOutputStream);
        System.out.println("Returned: "+ result);

        assert(result.equals("Not authorized"));
    }



    @Test 
    public void addAlreadyBlacklistedTest(){

        executor.sender(senderAddress);
        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();

        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();
        String result = helpers.extractErrorMessage(byteArrayOutputStream);
        System.out.println("Returned: "+ result);

        assert(result.equals("Already blacklisted"));
    }

    @Test 
    public void removeNoBlacklistedTest(){

        executor.sender(senderAddress);
        executor.callData(Bytes.fromHexString("537df3b6"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();

        executor.callData(Bytes.fromHexString("537df3b6"+ helpers.padHexStringTo256Bit(BlacklistContractAddress.toString())));
        executor.execute();
        String result = helpers.extractErrorMessage(byteArrayOutputStream);
        System.out.println("Returned: "+ result);

        assert(result.equals("Not blacklisted"));
    }

   

}
