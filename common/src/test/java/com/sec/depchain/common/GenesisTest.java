package com.sec.depchain.common;


import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sec.depchain.common.SmartContractsUtil.helpers;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GenesisTest {
    private static SimpleWorld simpleWorld;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;
    private static Address ISTCoinContractAddress = Address.fromHexString("1234567891234567891234567891234567891234");
    private static Address senderAddress = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
    private static Address clientAddress = Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeee");

    @BeforeAll
    //loads the genesis file and creates the accounts in simple world
    public static void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);


        simpleWorld = new SimpleWorld();
        String genesisFilePath = "src/main/java/com/sec/depchain/common/SmartContractsUtil/Genesis.json";
        
        try {
            // Read and parse JSON using Gson
            Gson gson = new Gson();
            JsonObject genesisData = gson.fromJson(new FileReader(genesisFilePath), JsonObject.class);

            // Get the state object
            JsonObject state = genesisData.getAsJsonObject("state");

            for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                String addressHex = entry.getKey();
                Address address = Address.fromHexString(addressHex);
                System.out.println("Address: " + address);
                JsonObject accountData = entry.getValue().getAsJsonObject();

                // Get balance
                BigInteger balance = new BigInteger(accountData.get("balance").getAsString());

                if (accountData.has("code")) {
                    // Contract account
                    Bytes contractCode = Bytes.fromHexString(accountData.get("code").getAsString());
                    simpleWorld.createAccount(address, 0, Wei.of(balance));
                    MutableAccount contractAccount = (MutableAccount) simpleWorld.get(address);
                    contractAccount.setCode(contractCode);

                    // Handle storage if present
                    if (accountData.has("storage")) {
                        JsonObject storage = accountData.getAsJsonObject("storage");
                        for (Map.Entry<String, JsonElement> storageEntry : storage.entrySet()) {
                            UInt256 key = UInt256.fromBytes(Bytes32.fromHexString(storageEntry.getKey()));
                            UInt256 value = UInt256.fromBytes(Bytes32.fromHexString(storageEntry.getValue().getAsString()));
                            contractAccount.setStorageValue(key, value);
                        }
                    }
                } else {
                    // Externally Owned Account (EOA)
                    simpleWorld.createAccount(address, 0, Wei.of(balance));
                }
            }

            System.out.println("Genesis state loaded successfully!");

        } catch (IOException e) {
            System.err.println("Error reading genesis file: " + e.getMessage());
        }
    }


    @Test
    @Order(1)
    public void testContractExecution(){
        System.out.println("ContractExecutionTest");
        
        

        System.out.println("Before contract execution");
        printAccountsInfo();

        

    
        // get ist coin bytecode(already runtime) from the simpleworld account
        String Bytecode = simpleWorld.get(Address.fromHexString("0x1234567891234567891234567891234567891234")).getCode().toHexString();
        // String Bytecode = helpers.loadBytecode("src/main/java/com/sec/depchain/resources/contracts_bytecode/ISTCoin.bin");
        // System.out.println("Bytecode: " + Bytecode);
       

        executor.code(Bytes.fromHexString(Bytecode));
        executor.sender(Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"));
        executor.receiver(Address.fromHexString("1234567891234567891234567891234567891234"));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        System.out.println("After contract execution");
        printAccountsInfo();
       

        String runtimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);

        //run time
        executor.code(Bytes.fromHexString(runtimeBytecode));


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
        executor.sender(senderAddress);
        executor.callData(Bytes.fromHexString(transferData));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());

        // Balance of contract
        executor.callData(Bytes.fromHexString("70a08231"+paddedAddress));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());
        long balanceOfContractAfter = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'balanceOf(contract)': " + Long.toString(balanceOfContractAfter));

        // Balance of Sender
        executor.callData(Bytes.fromHexString("70a08231"+ helpers.padHexStringTo256Bit(senderAddress.toHexString())));
        executor.execute();
        long balanceOfSenderAfter = helpers.extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'balanceOf(sender)': " + Long.toString(balanceOfSenderAfter));

        assert(balanceOfContractAfter == 1000);
    }
    
    @Test
    public void unauthorizedAddBlacklistTest(){
        executor.sender(clientAddress);
        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(clientAddress.toString())));
        executor.execute();
        String result = helpers.extractErrorMessage(byteArrayOutputStream);
        System.out.println("Returned: "+ result);

        assert(result.equals("Not authorized"));
    }
    @Test
    //test if an address is blacklisted before and after beeing added
    public void testIsBlacklist(){

        executor.callData(Bytes.fromHexString("fe575a87"+ helpers.padHexStringTo256Bit(clientAddress.toString())));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());
        System.out.println(clientAddress + " is Blacklisted: "+ helpers.extractBooleanFromReturnData(byteArrayOutputStream));

        executor.sender(senderAddress);
        executor.callData(Bytes.fromHexString("44337ea1"+ helpers.padHexStringTo256Bit(clientAddress.toString())));
        executor.execute();

        executor.callData(Bytes.fromHexString("fe575a87"+ helpers.padHexStringTo256Bit(clientAddress.toString())));
        executor.execute();
        // System.out.println(byteArrayOutputStream.toString());
        Boolean isBlacklisted = helpers.extractBooleanFromReturnData(byteArrayOutputStream);
        System.out.println(clientAddress + " is Blacklisted: "+ isBlacklisted);

        assert(isBlacklisted);
    }

    @AfterAll
    public static void testFinalAccountsInfo(){
        System.out.println("Final Accounts Information:");
        printAccountsInfo();
            }
        
        
    public static void printAccountsInfo() {
        System.out.println("All Accounts Information:");
        // Iterate over all accounts
        // Get all accounts that have been touched (i.e., initialized in the state)
        Collection<? extends Account> accounts = simpleWorld.getTouchedAccounts();

        for (Account account : accounts) {
            System.out.println("Account Information:");
            System.out.println("  Address: " + account.getAddress());
            System.out.println("  Balance: " + account.getBalance().toLong());
            System.out.println("  Nonce: " + account.getNonce());

            if (account.getCode().isEmpty()) {
                System.out.println("  Type: Externally Owned Account (EOA)");
            } else {
                System.out.println("  Type: Smart Contract");
                System.out.println("  Code: " + account.getCode().toHexString());

                // If it's a smart contract, get and print storage entries
                System.out.println("  Storage:");
                if(account.getAddress().equals(ISTCoinContractAddress) ){
                    
                    // Get total supply
                    System.out.println("    _totalSupply: " + account.getStorageValue(UInt256.valueOf(2)).toLong());

                    // Get name and symbol
                    System.out.println("    _name: " +  helpers.convertHexadecimalToAscii(account.getStorageValue(UInt256.valueOf(3)).toString()));
                    System.out.println("    _symbol: " + helpers.convertHexadecimalToAscii(account.getStorageValue(UInt256.valueOf(4)).toString()));
                    String paddedSenderAddress = helpers.padHexStringTo256Bit(Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef").toHexString());
                    String balanceSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedSenderAddress + helpers.convertIntegerToHex256Bit(0))));
                    System.out.println("    _balances[msg.sender]: " + account.getStorageValue(UInt256.fromHexString(balanceSlotMapping)).toLong());
                    System.out.println("    blacklist sender contract address: " + account.getStorageValue(UInt256.valueOf(5)));

                }
            }
            System.out.println();
        }
    }




}



        
    

