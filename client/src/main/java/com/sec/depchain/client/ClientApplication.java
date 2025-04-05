package com.sec.depchain.client;

import org.hyperledger.besu.datatypes.Address;
import org.json.JSONObject;


import com.sec.depchain.common.Transaction;
import com.sec.depchain.common.SmartContractsUtil.helpers;

import java.math.BigInteger;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientApplication {
    private static final int DEBUG_MODE = 1;
    private final Wallet wallet;
    private ClientLibrary clientLibrary;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <clientId>");
            System.exit(1);
        }

        int clientId = parseClientId(args[0]);

        ClientApplication app = new ClientApplication(clientId);
    }

    public ClientApplication(int clientId) throws Exception {
        this.wallet = new Wallet(clientId);
        this.clientLibrary = new ClientLibrary(clientId, wallet);
        if (DEBUG_MODE == 1) {
            System.out.println("CLIENT APP - DEBUG: Starting: {" + clientId + "}");
            System.out.println("CLIENT APP - DEBUG: Address: {" + wallet.getAddress() + "}");
        }
        setupInputLoop(clientLibrary);
    }

    private static int parseClientId(String clientIdArg) {
        try {
            return Integer.parseInt(clientIdArg);
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] <clientId> must be an integer.");
            System.exit(1);
            return -1;
        }
    }

    private void setupInputLoop(ClientLibrary clientLibrary) throws Exception {
        // I want to open a json file and get certain data from it

        
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome! Enter <help> for the available commands , or 'exit' to quit:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                String[] caseArgs = input.split(" ", 4);

                switch (caseArgs[0].toLowerCase()) {
                    case "exit":
                        exitApplication(scanner, clientLibrary);
                        return;

                    case "transfer":
                        handleTransactionRequest(caseArgs, clientLibrary);
                        break;
                    case "contractexecution":
                        handleSmartContractExecution(scanner, clientLibrary);
                        break;
                    case "help":
                        System.out.println(
                                "[INFO] Available commands: transfer <to> <value>, contractexecution, exit");
                        break;    
                    default:
                        System.out.println(
                                "[ERROR] Unknown Command: Enter <help> for the  available commands.");
                        break;
                }
            }
        }
    }

    private void handleTransactionRequest(String[] caseArgs, ClientLibrary clientLibrary) throws Exception {
        
        if (caseArgs.length < 3) { // transfer <to> <balance> [data]
            System.out.println("[ERROR] Please provide the command: transfer <to> <value>");
            return;
        }

        String toAddress = caseArgs[1];
        BigInteger value;
        try{
            value = new BigInteger(caseArgs[2]);
        }catch (NumberFormatException e) {
            System.out.println("[ERROR] Please provide a valid destination address.");
            return;
        }
        
        String data = (caseArgs.length == 4) ? caseArgs[3] : "";

        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((transaction, appendedString, timestamp) -> {
            String message = transaction.isSuccess()
                    ? String.format("[SUCCESS] '%s' appended at position %s.", appendedString, timestamp)
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(transaction.isSuccess());
        });

        if (DEBUG_MODE == 1)
            System.out.println("CLIENT APP - DEBUG: Sending transaction request to {" + toAddress + "} with nonce {"
                    + wallet.getNonce() + "} and value {" + value + "}");

        
        
        Transaction tx = new Transaction(Address.fromHexString(wallet.getAddress()), Address.fromHexString(toAddress), value,
                data, wallet.getNonce(), null);

        wallet.incremetNonce();
        String signature = wallet.signTransaction(tx);
        tx.setSignature(signature);
        System.out.println("[INFO] Transaction Hash: " + tx.computeTxHash());
        clientLibrary.sendTransferRequest(tx);
        try {
            futureResponse.get();
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Interrupted while waiting for append response.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("[ERROR] Execution failed: " + e.getCause());
        }
    }

    private void handleSmartContractExecution( Scanner scanner,ClientLibrary clientLibrary) throws Exception {
        
        JSONObject calls = helpers.loadJsonFromFile("../common/src/main/java/com/sec/depchain/common/SmartContractsUtil/hashedCalls.json");
        System.out.println("Welcome to Contract Execution ! Enter <help> for the available contract calls , or 'exit' to quit:");
        while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                String[] caseArgs = input.split(" ", 5);
                String data;
                switch (caseArgs[0].toLowerCase()) {
                    case "exit":
                        return;

                    case "transfer":
                        // transferFrom(address,address,uint256) -> 23b872dd
                        // data = "a9059cbb";
                        data = calls.getString("transfer");
                        if(caseArgs.length != 4) { // transfer  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: transfer <contractAddress> <to> <value>");
                            break;
                        }
                        BigInteger value = new BigInteger(caseArgs[3]);
                        String finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]) + helpers.convertBigIntegerToHex256Bit(value);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;

                    case "approve":
                        // approve(address,uint256) -> 095ea7b3
                        // data = "095ea7b3";
                        data = calls.getString("approve");
                        if(caseArgs.length != 4) { // approve  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: approve <contractAddress> <to> <value>");
                            break;
                        }
                        value = new BigInteger(caseArgs[3]);
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]) + helpers.convertBigIntegerToHex256Bit(value);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;
                    case "transferfrom":
                        // transferFrom(address,address,uint256) -> 23b872dd
                        // data = "23b872dd";
                        data = calls.getString("transferFrom");
                        if(caseArgs.length != 5) { // transferFrom  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: transferFrom <contractAddress> <from> <to> <value>");
                            break;
                        }
                        value = new BigInteger(caseArgs[4]);
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]) + helpers.padHexStringTo256Bit(caseArgs[3]) + helpers.convertBigIntegerToHex256Bit(value);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;
                    case "balanceof":
                        // balanceOf(address) -> 70a08231
                        // data = "70a08231";
                        data = calls.getString("balanceOf");
                        if(caseArgs.length != 3) { // balanceOf  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: balanceOf <contractAddress> <address>");
                            break;
                        }
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;
                    case "isblacklisted":
                        // isBlacklisted(address) -> fe575a87
                        // data = "fe575a87";
                        data = calls.getString("isBlacklisted");
                        if(caseArgs.length != 3) { // isBlacklisted  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: isBlacklisted <contractAddress> <address>");
                            break;
                        }
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;

                    case "addtoblacklist":
                        // addBlacklist(address) -> 44337ea1
                        // data = "44337ea1";
                        data = calls.getString("addToBlacklist");
                        if(caseArgs.length != 3) { // addBlacklist  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: addToBlacklist <contractAddress> <address>");
                            break;
                        }
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;

                    case "removefromblacklist":
                        // removeFromBlacklist(address) -> 537df3b6
                        // data = "537df3b6";
                        data = calls.getString("removeFromBlacklist");
                        if(caseArgs.length != 3) { // removeFromBlacklist  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: removeFromBlacklist <contractAddress> <address>");
                            break;
                        }
                        finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);
                        break;
                    
                    case "help":
                        System.out.println("[INFO] Available contract calls: transfer, approve, transferFrom, isBlacklisted, addToBlacklist, removeFromBlacklist");
                        break;
                    default:
                        System.out.println("[ERROR] Choose one Contract call:");
                        break;
                }
            }
        

    }

    private void SmartContractExecutionRequest(String[] caseArgs, ClientLibrary clientLibrary, String data) throws Exception {
        

        String toAddress = caseArgs[1];
        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((transaction, appendedString, timestamp) -> {
            String message = transaction.isSuccess()
                    ? String.format("[SUCCESS] '%s' executed in block at position %s. And got the response: %s.", appendedString, timestamp, transaction.getResponse())
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(transaction.isSuccess());
        });

        if (DEBUG_MODE == 1)
        System.out.println("CLIENT APP - DEBUG: Sending transaction request to {" + toAddress + "} with nonce {"
+ wallet.getNonce() + "} ");


        
        
        Transaction tx = new Transaction(Address.fromHexString(wallet.getAddress()), Address.fromHexString(toAddress), BigInteger.ZERO,
                data, wallet.getNonce(), null);

        wallet.incremetNonce();
        String signature = wallet.signTransaction(tx);
        tx.setSignature(signature);
        System.out.println("[INFO] Transaction Hash: " + tx.computeTxHash());
        clientLibrary.sendTransferRequest(tx);
        try {
            futureResponse.get();
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Interrupted while request response.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("[ERROR] Execution failed: " + e.getCause());
        }
    }


    private static void exitApplication(Scanner scanner, ClientLibrary clientLibrary) {
        System.out.println("CLIENT APP - INFO: Client is exiting");
        if (scanner != null) {
            scanner.close();
        }
        clientLibrary.close();
        System.out.println("CLIENT APP - INFO: Client library closed.");
        Runtime.getRuntime().halt(0);

        System.exit(0);
    }
    
}
