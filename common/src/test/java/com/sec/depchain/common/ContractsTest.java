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

import org.junit.jupiter.api.Test;


public class ContractsTest {


    @Test
    public void testContract(){
        // simpleWorld to maintain account state
        SimpleWorld simpleWorld = new SimpleWorld();

        // EOT mock account
        Address senderAddress = Address.fromHexString("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        simpleWorld.createAccount(senderAddress,0, Wei.fromEth(100));

        // contract mock account
        Address contractAddress = Address.fromHexString("1234567891234567891234567891234567891234");
        simpleWorld.createAccount(contractAddress,0, Wei.fromEth(0));
        MutableAccount contractAccount = (MutableAccount) simpleWorld.get(contractAddress);
        // System.out.println("Contract Account");
        // System.out.println("  Address: "+contractAccount.getAddress());
        // System.out.println("  Balance: "+contractAccount.getBalance());
        // System.out.println("  Nonce: "+contractAccount.getNonce());
        // System.out.println("  Storage:");
        // System.out.println("    Slot 0: "+simpleWorld.get(contractAddress).getStorageValue(UInt256.valueOf(0)));
        // String paddedAddress = padHexStringTo256Bit(senderAddress.toHexString());
        // String stateVariableIndex = convertIntegerToHex256Bit(1);
        // String storageSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedAddress + stateVariableIndex)));
        // System.out.println("    Slot SHA3[msg.sender||1] (mapping): "+simpleWorld.get(contractAddress).getStorageValue(UInt256.fromHexString(storageSlotMapping)));
        // System.out.println();

        //initialize execution tracer for return and debug information
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        
        // initialize EVM local node
        var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        //load ISTCoin contract creation bytecode gotten from contract compilation
        String ISTCoinBytecode = loadBytecode("src/main/java/com/sec/depchain/resources/contracts_bytecode/ISTCoin.bin");
        // System.out.println(coinBytecode);
        executor.code(Bytes.fromHexString(ISTCoinBytecode));
        executor.sender(senderAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();
            

        
        // Get contract runtime Bytecode from return of executing the contract creation bytecode
        String runtimeBytecode = extractRuntimeBytecode(byteArrayOutputStream);
        // System.out.println("Runtime Bytecode: " + runtimeBytecode);

        // Deploy ISTCoin contract
        executor.code(Bytes.fromHexString(runtimeBytecode));

        


        //Test contract parameters
        // Token Symbol
        // 95d89b41 -> first 4 bytes of the Keccak-256 hash of the function signature 'symbol()'
        executor.callData(Bytes.fromHexString("95d89b41"));
        executor.execute();
        String tokenSymbol = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'symbol()': " + tokenSymbol);


        //Token Name 
        executor.callData(Bytes.fromHexString("06fdde03"));
        executor.execute();
        String tokenName = extractStringFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'name()': " + tokenName);


        // Decimals
        executor.callData(Bytes.fromHexString("313ce567"));
        executor.execute();
        int decimals = extractIntegerFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'decimals()': " + Integer.toString(decimals));

        // Total Supply
        executor.callData(Bytes.fromHexString("18160ddd"));
        executor.execute();
        long totalSupply = extractLongFromReturnData(byteArrayOutputStream);
        System.out.println("Output of 'totalSupply()': " + Long.toString(totalSupply));

  
        assert(tokenSymbol.equals("IST"));
        assert(tokenName.equals("IST Coin"));
        assert(decimals == 2);
        assert(totalSupply == 100000000* Math.pow(10,decimals));

    }
  


    public String loadBytecode(String path) {
        try {
            //print base path
            // System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractRuntimeBytecode(ByteArrayOutputStream byteArrayOutputStream) {
        try {
            String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
            JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
            
            
            // If return_data doesn't exist, try to extract from memory using stack values
            String memory = jsonObject.get("memory").getAsString();
            JsonArray stack = jsonObject.get("stack").getAsJsonArray();
            
            if (stack.size() >= 2) {
                int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
                int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
                
                // Make sure to remove the '0x' prefix from memory if it exists
                if (memory.startsWith("0x")) {
                    memory = memory.substring(2);
                }
                
                // Extract the relevant portion of memory based on offset and size
                return memory.substring(offset * 2, offset * 2 + size * 2);
            }
            
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode(returnData);
    }

    
    public static BigInteger extractBigIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        System.out.println("return data: " + returnData);
        return new BigInteger(returnData, 16); // Use BigInteger to parse very large numbers
    }

    public static long extractLongFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    
        String memory = jsonObject.get("memory").getAsString();
    
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
    
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Long.decode("0x" + returnData); // Use Long.decode to parse larger numbers
    }
    
    

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length-1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size()-1).getAsString());
        int size = Integer.decode(stack.get(stack.size()-2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        int stringOffset = Integer.decode("0x"+returnData.substring(0, 32 * 2));
        int stringLength = Integer.decode("0x"+returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
        String hexString = returnData.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

        return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    }

    
    public static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            int value = Integer.parseInt(hexString.substring(i, i + 2), 16);
            byteArray[i / 2] = (byte) value;
        }

        return byteArray;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);

        return String.format("%064x", bigInt);
    }
    public static String converStringToHexString(String str) {
        return Numeric.toHexString(str.getBytes());
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }
}

