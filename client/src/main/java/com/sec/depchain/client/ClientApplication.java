package com.sec.depchain.client;

import org.hyperledger.besu.datatypes.Address;

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
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter 'transfer <to> <value> [<data>]' to transfer , or 'exit' to quit:");

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
                                "[INFO] Enter 'transfer <to> <value> [<data>]' to transfer , or 'exit' to quit:");
                        break;    
                    default:
                        System.out.println(
                                "[ERROR] Enter 'transfer <to> <value> [<data>]' to transfer , or 'exit' to quit:");
                        break;
                }
            }
        }
    }

    private void handleTransactionRequest(String[] caseArgs, ClientLibrary clientLibrary) throws Exception {
        System.out.println(caseArgs.length);
        if (caseArgs.length < 3) { // transfer <to> <balance> [data]
            System.out.println("[ERROR] Please provide the command: transfer <to> <value>");
            return;
        }

        String toId = caseArgs[1];
        BigInteger value = new BigInteger(caseArgs[2]);
        String data = (caseArgs.length == 4) ? caseArgs[3] : "";

        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
            String message = result
                    ? String.format("[SUCCESS] '%s' appended at position %s.", appendedString, timestamp)
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(result);
        });

        if (DEBUG_MODE == 1)
            System.out.println("CLIENT APP - DEBUG: Sending transaquion request: {" + toId + "}");

        // TODO id logic right now is to address
        Transaction tx = new Transaction(Address.fromHexString(wallet.getAddress()), Address.fromHexString(toId), value,
                data, wallet.getNonce(), null);

        wallet.incremetNonce();
        String signature = wallet.signTransaction(tx);
        tx.setSignature(signature);
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
        
        
        System.out.println("Enter 'ContractCall <to> <value> [<data>]', or 'exit' to leave SmartContract execution :");
        while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                String[] caseArgs = input.split(" ", 4);

                switch (caseArgs[0].toLowerCase()) {
                    case "exit":
                        return;

                    case "transfer":
                        // transferFrom(address,address,uint256) -> 23b872dd
                        String data = "a9059cbb";
                        if(caseArgs.length != 4) { // transfer  <contractAddress> [<data>]
                            System.out.println("[ERROR] Please provide the command: transferFrom <contractAddress> <to> <value>)");
                            break;
                        }
                        int value = Integer.parseInt(caseArgs[3]);
                        String finalData = data + helpers.padHexStringTo256Bit(caseArgs[2]) + helpers.convertIntegerToHex256Bit(value);
                        System.out.println("[INFO] data: " + finalData);
                        SmartContractExecutionRequest(caseArgs, clientLibrary, finalData);

                    default:
                        System.out.println("[ERROR] Choose one Contract call:");
                        break;
                }
            }
        

    }

    private void SmartContractExecutionRequest(String[] caseArgs, ClientLibrary clientLibrary, String data) throws Exception {
        

        String toId = caseArgs[1];
        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
            String message = result
                    ? String.format("[SUCCESS] '%s' appended at position %s.", appendedString, timestamp)
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(result);
        });

        if (DEBUG_MODE == 1)
            System.out.println("CLIENT APP - DEBUG: Sending transaquion request: {" + toId + "}");

        
        // TODO id logic right now is to address
        Transaction tx = new Transaction(Address.fromHexString(wallet.getAddress()), Address.fromHexString(toId), BigInteger.ZERO,
                data, wallet.getNonce(), null);

        wallet.incremetNonce();
        String signature = wallet.signTransaction(tx);
        tx.setSignature(signature);
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
