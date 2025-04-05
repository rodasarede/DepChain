package com.sec.depchain.common;

import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.SmartContractsUtil.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
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
    // all server nodes will do this to initialize simpleworld with genesis
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
        //updates simple world with current blockchain state 
        updateSimpleWorldState();
        // get ISTCoin contract creation bytecode 
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
        // receives a transaction with native transfer from a client already decided by the consensus to be apended next
        
        //mock transaction to be appended 
        Transaction tx = new Transaction(
            Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"), // from
            Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeee"), // to
            BigInteger.valueOf(100), // value
            "", // data
            BigInteger.valueOf(1), // nonce
            "signature" // signature
        );
        tx.execute(blockchain);
        if(tx.isSuccess()){
            System.out.println("Transaction executed successfully");
            System.out.println("Updating world state");
            updateSimpleWorldState();
        }else{
            System.out.println("Transaction execution failed");
        }        

        // debug to see if the values are correctly updated
        printAccountsInfo();
        blockchain.getLatestBlock().printBlockDetails();

    }

    public static void updateSimpleWorldState() {

        for (Map.Entry<Address, AccountState> entry : blockchain.getGenesisState().entrySet()) {
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
                if(account.getAddress().equals( Address.fromHexString("1234567891234567891234567891234567891234")) ){
                    
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
