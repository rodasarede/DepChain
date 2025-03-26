package com.sec.depchain.common;


import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.AccountStorageEntry;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.Bool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sec.depchain.common.SmartContractsUtil.helpers;

public class GenesisTest {
    private static SimpleWorld simpleWorld;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;

    @BeforeAll
    public static void setup() {
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
    public void testGenesisState() {
        System.out.println("Genesis Test!");



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

                int keyIndex = 0;
                while (true) {
                    // Retrieve the storage value for the current key
                    UInt256 storageValue = account.getStorageValue(UInt256.valueOf(keyIndex));

                    // If the value is 0, stop iterating
                    if (storageValue.isZero()) {
                        break;
                    }

                    // Print the storage key and value
                    System.out.println("    Key: " + keyIndex + " -> Value: " + storageValue.toString());

                    // Increment the key for the next iteration
                    keyIndex++;
                }
            }
            System.out.println();
        }
    }


    @Test
    public void testContractExecution(){

        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);

        Address ISTCoinContractAddress = Address.fromHexString("0x1234567891234567891234567891234567891234");
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





        // get ist coin bytecode(already runtime) from the simpleworld account
        String Bytecode = simpleWorld.get(Address.fromHexString("0x1234567891234567891234567891234567891234")).getCode().toHexString();
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(Bytecode));
        executor.sender(Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"));
        executor.receiver(Address.fromHexString("0x1234567891234567891234567891234567891234"));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

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


    }




}



        
    

