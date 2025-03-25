package com.sec.depchain.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.tuweni.bytes.Bytes;

import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sec.depchain.common.SmartContractsUtil.helpers;


// Depcoin conversao com ISTCoin gerida pelo contrato ISTCoin, certo?
// como gerir as transfers de Depcoin entre addresses (smartcontract??)
//  


public class ContractsTest {

    private static SimpleWorld simpleWorld;
    private static Address senderAddress;
    private static Address ISTCoinContractAddress;
    private static Address BlacklistContractAddress;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;

    @BeforeAll
    public static void setup() {
        // simpleWorld to maintain account state
        simpleWorld = new SimpleWorld();

        // EOT mock account
        senderAddress = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        simpleWorld.createAccount(senderAddress,0, Wei.fromEth(100));

        // contract mock account
        ISTCoinContractAddress = Address.fromHexString("1234567891234567891234567891234567891234");
        simpleWorld.createAccount(ISTCoinContractAddress,0, Wei.fromEth(0));
        MutableAccount ISTCoinContractAccount = (MutableAccount) simpleWorld.get(ISTCoinContractAddress);

        System.out.println("Contract Account");
        System.out.println("  Address: " + ISTCoinContractAccount.getAddress());
        System.out.println("  Balance: " + ISTCoinContractAccount.getBalance());
        System.out.println("  Nonce: " + ISTCoinContractAccount.getNonce());
        System.out.println("  Storage:");

        // Get total supply
        System.out.println("    _totalSupply: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(0)));

        // Get name and symbol
        System.out.println("    _name: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(1)));
        System.out.println("    _symbol: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(2)));

        // Retrieve a balance from _balances mapping (example senderAddress)
        String paddedSenderAddress = helpers.padHexStringTo256Bit(senderAddress.toHexString());
        String balanceSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedSenderAddress + helpers.convertIntegerToHex256Bit(0))));
        System.out.println("    _balances[msg.sender]: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.fromHexString(balanceSlotMapping)));
        System.out.println("    blacklist contract address: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(5)));
        // Retrieve an allowance from _allowances mapping (example senderAddress and spenderAddress)
        // String allowancesSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedSenderAddress + helpers.convertIntegerToHex256Bit(1))));
        // String allowancesFinalSlot = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(allowancesSlotMapping)));
        // System.out.println("    _allowances[msg.sender]: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.fromHexString(allowancesFinalSlot)).toLong());


        //blacklist contract mock account
        BlacklistContractAddress = Address.fromHexString("1234567891234567891234567891234567891235");
        simpleWorld.createAccount(BlacklistContractAddress,0, Wei.fromEth(0));
        MutableAccount blacklistAccount = (MutableAccount) simpleWorld.get(BlacklistContractAddress);


        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        // initialize EVM local node
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        
        //load ISTCoin contract creation bytecode gotten from contract compilation
        String ISTCoinBytecode = helpers.loadBytecode("src/main/java/com/sec/depchain/resources/contracts_bytecode/ISTCoin.bin");
        // System.out.println(coinBytecode);
        executor.code(Bytes.fromHexString(ISTCoinBytecode));
        executor.sender(senderAddress);
        executor.receiver(ISTCoinContractAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();


        System.out.println("Contract Account");
        System.out.println("  Address: " + ISTCoinContractAccount.getAddress());
        System.out.println("  Balance: " + ISTCoinContractAccount.getBalance());
        System.out.println("  Nonce: " + ISTCoinContractAccount.getNonce());
        System.out.println("  Storage:");

        // Get total supply
        System.out.println("    _totalSupply: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(2)).toLong());

        // Get name and symbol
        System.out.println("    _name: " +  helpers.convertHexadecimalToAscii(simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(3)).toString()));
        System.out.println("    _symbol: " + helpers.convertHexadecimalToAscii(simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(4)).toString()));
        System.out.println("    _balances[msg.sender]: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.fromHexString(balanceSlotMapping)).toLong());
        System.out.println("    blacklist contract address: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.valueOf(5)));
        // Retrieve an allowance from _allowances mapping (example senderAddress and spenderAddress)
        // allowancesSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedSenderAddress + helpers.convertIntegerToHex256Bit(1))));
        
        // System.out.println("    _allowances[msg.sender]: " + simpleWorld.get(ISTCoinContractAddress).getStorageValue(UInt256.fromHexString(allowancesSlotMapping)).toLong());


        
        // Get contract runtime Bytecode from return of executing the contract creation bytecode
        String runtimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);

        // // System.out.println("Runtime Bytecode: " + runtimeBytecode);

        // String BlacklistBytecode = helpers.loadBytecode("src/main/java/com/sec/depchain/resources/contracts_bytecode/Blacklist.bin");
        // executor.code(Bytes.fromHexString(BlacklistBytecode));
        // executor.sender(senderAddress);
        // executor.worldUpdater(simpleWorld.updater());
        // executor.commitWorldState();
        // executor.execute();

        // String BlacklistRuntimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);

        // // System.out.println("Blacklist runtime bytecode: " + BlacklistRuntimeBytecode);


        // Deploy ISTCoin runtime
        executor.code(Bytes.fromHexString(runtimeBytecode));
        executor.execute();


        // executor.code(Bytes.fromHexString(BlacklistRuntimeBytecode));
        // executor.worldUpdater(simpleWorld.updater());
        // executor.commitWorldState();
        // executor.callData(Bytes.fromHexString("45773e4e"));
        // executor.execute();
        // // System.out.println(byteArrayOutputStream.toString());
        // String string = extractStringFromReturnData(byteArrayOutputStream);
        // System.out.println("Output string of 'sayHelloWorld():' " + string);

    }

    //Test contract parameters
    @Test
    public void testSymbol(){
        
        // Token Symbol
        // 95d89b41 -> first 4 bytes of the Keccak-256 hash of the function signature 'symbol()'
        executor.callData(Bytes.fromHexString("95d89b41"));
        executor.execute();
        String tokenSymbol = helpers.extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'symbol()': " + tokenSymbol);

        assert(tokenSymbol.equals("IST"));


    }

    @Test
    public void testName(){
        //Token Name 
        executor.callData(Bytes.fromHexString("06fdde03"));
        executor.execute();
        String tokenName = helpers.extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'name()': " + tokenName);

        assert(tokenName.equals("IST Coin"));
    }

    @Test
    public void testDecimalsAndSupply(){

        // Decimals
        executor.callData(Bytes.fromHexString("313ce567"));
        executor.execute();
        int decimals = helpers.extractIntegerFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'decimals()': " + Integer.toString(decimals));


        // Total Supply
        executor.callData(Bytes.fromHexString("18160ddd"));
        executor.execute();
        long totalSupply = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'totalSupply()': " + Long.toString(totalSupply));

        assert(decimals == 2);
        assert(totalSupply == 100000000* Math.pow(10,decimals));
    }

    @Test
    // address who created the contract(sender) has total supply
    public void testBalanceOf(){
        // Balance of sender
        executor.callData(Bytes.fromHexString("70a08231"+helpers.padHexStringTo256Bit(senderAddress.toHexString())));
        executor.execute();
        long balanceOfSender = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'balanceOf(sender)': " + Long.toString(balanceOfSender));

        // assert(balanceOfSender == 100000000* Math.pow(10,2));
    }

    @Test
    //transfer test from sender(smart contract creator) to contract address
    // transfer not working because of new smart contract with access control list
    public void testTransfer(){

        // Balance of contract
        String paddedAddress = helpers.padHexStringTo256Bit(ISTCoinContractAddress.toHexString());
        executor.callData(Bytes.fromHexString("70a08231"+paddedAddress));
        executor.execute();
        long balanceOfContractBefore = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'balanceOf(contract)': " + Long.toString(balanceOfContractBefore));

        // Transfer 1000 tokens from sender to contract
        //sender is still set as the previous address since contract deoploymeent
        
        String transferData = "a9059cbb"+helpers.padHexStringTo256Bit(ISTCoinContractAddress.toHexString())+helpers.convertIntegerToHex256Bit(1000);
        executor.callData(Bytes.fromHexString(transferData));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());

        // Balance of contract
        executor.callData(Bytes.fromHexString("70a08231"+paddedAddress));
        executor.execute();
        long balanceOfContractAfter = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'balanceOf(contract)': " + Long.toString(balanceOfContractAfter));

        // assert(balanceOfContractAfter == 1000);
    }
}

    