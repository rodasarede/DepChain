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
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.sec.depchain.common.SmartContractsUtil.helpers;
import com.sec.depchain.common.util.Constants;




public class Blockchain {

    private static final int DEBUG_MODE = 1;
    private static List<Block> chain = new ArrayList<>();
    private static List<Transaction> pendingTransactions = new ArrayList<>();
    private static Map<Address, AccountState> genesisState = new HashMap<>();
    private static SimpleWorld simpleWorld;
    private static EVMExecutor executor;
    private static ByteArrayOutputStream byteArrayOutputStream;
    private static String hardcodedRuntimeBytecode ;

    public Blockchain() {
       
        //node tracer
        byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
        Address contractAddress = Address.fromHexString("0x1234567891234567891234567891234567891234");

        //initialize local EVM node
        executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        executor.tracer(tracer);
        
        Block genesisBlock = new Block(Constants.GENESIS_BLOCK_FILE);
        chain.add(genesisBlock);
        genesisState.putAll(genesisBlock.getState());

        // initialize simpleworld state and update it with currentstate
        simpleWorld = new SimpleWorld();
        updateSimpleWorldStateWithGenesis();

        // ISTCoin contract creation bytecode from contract address code
        String Bytecode = simpleWorld.get(contractAddress).getCode().toHexString();
       
        executor.code(Bytes.fromHexString(Bytecode));
        //contract admin with all supply
        executor.sender(Address.fromHexString("b8124c42749e5f1908ab8c5afde9358005320306"));
        executor.receiver(contractAddress);
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        executor.execute();

        String runtimeBytecode = helpers.extractRuntimeBytecode(byteArrayOutputStream);
        hardcodedRuntimeBytecode = runtimeBytecode; // change in future
        MutableAccount contractAccount = (MutableAccount) simpleWorld.get(contractAddress);
        contractAccount.setCode(Bytes.fromHexString(runtimeBytecode));
        
        
        //runtime bytecode stored in the IST CONTRACT ADDRESS
        // System.out.println("Runtime bytecode: " + simpleWorld.get(contractAddress).getCode().toHexString());
        executor.code(Bytes.fromHexString(simpleWorld.get(contractAddress).getCode().toHexString()));
        executor.worldUpdater(simpleWorld.updater());
        executor.commitWorldState();
        
        


        //test call smart contract
        // executor.callData(Bytes.fromHexString("95d89b41"));
        // executor.execute();
        // String tokenSymbol = helpers.extractStringFromReturnData(byteArrayOutputStream);
        // System.out.println("Output of 'symbol()': " + tokenSymbol);
    }

    public  void addTransaction(Transaction tx) {
        pendingTransactions.add(tx);
    }
    public SimpleWorld getSimpleWorld() {
        return simpleWorld;
    }


    public  List<Block> getChain() {
        return chain;
    }
    public  Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }
    public  Map<Address, AccountState> getGenesisState() {
        return genesisState;
    }
    public int getChainSize() {
        return chain.size();
    }
    public EVMExecutor getExecutor() {
        return executor;
    }
    public ByteArrayOutputStream getbyteArrayOutputStream() {
        return byteArrayOutputStream;
    }
    public String getHardcodedRuntimeBytecode() {
        return hardcodedRuntimeBytecode;
    }
    public void updateSimpleWorldStateWithGenesis() {

        for (Map.Entry<Address, AccountState> entry : getGenesisState().entrySet()) {
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
    public  void printAccountsInfo() {
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
                    String paddedSenderAddress = helpers.padHexStringTo256Bit(Address.fromHexString("0xb8124c42749e5f1908ab8c5afde9358005320306").toHexString());
                    String balanceSlotMapping = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(paddedSenderAddress + helpers.convertIntegerToHex256Bit(0))));
                    System.out.println("    _balances[msg.sender]: " + account.getStorageValue(UInt256.fromHexString(balanceSlotMapping)).toLong());
                    System.out.println("    blacklist sender contract address: " + account.getStorageValue(UInt256.valueOf(5)));

                }
            }
            System.out.println();
        }
    }


}

