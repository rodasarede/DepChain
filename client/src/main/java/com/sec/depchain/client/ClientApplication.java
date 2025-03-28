package com.sec.depchain.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sec.depchain.common.Transaction;

import java.math.BigInteger;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApplication.class);
    private static final int DEBUG_MODE = 1;
    private final Wallet wallet;
    private int nonce = 0;
    private ClientLibrary clientLibrary;
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <clientId>");
            System.exit(1);
        }

        int clientId = parseClientId(args[0]);

        ClientApplication app = new ClientApplication(clientId);
    }

    public ClientApplication(int clientId) throws Exception{
        this.wallet = new Wallet(clientId);
        this.clientLibrary = new ClientLibrary(clientId, wallet);
        if (DEBUG_MODE == 1) 
        {
            LOGGER.debug("Starting: '{}'", clientId);
            LOGGER.debug("Address: '{}'", wallet.getAddress());

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
            System.out.println("[INFO] Enter 'transfer <to> <value> <data>' to transfer , or 'exit' to quit:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                String[] caseArgs = input.split(" ", 4);

                switch (caseArgs[0].toLowerCase()) {
                    case "exit":
                        exitApplication();
                        return;

                    case "transfer":
                        handleTransactionRequest(caseArgs, clientLibrary);
                        break;
                    default:
                        System.out.println("[ERROR] Invalid command. Use 'append <string>' or 'exit'.");
                        break;
                }
            }
        }
    }

    private void handleTransactionRequest(String[] caseArgs, ClientLibrary clientLibrary) throws Exception {
        System.out.println(caseArgs.length);
        if (caseArgs.length < 4) { //transfer <to> <balance> <data>
            System.out.println("[ERROR] Please provide a string to append.");
            return;
        }

        String toId = caseArgs[1];
        BigInteger value = new BigInteger(caseArgs[2]);
        String data = caseArgs[3];
        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
            String message = result
                    ? String.format("[SUCCESS] '%s' appended at position %s.", appendedString, timestamp)
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(result);
        });

        if (DEBUG_MODE == 1) LOGGER.debug("Sending transaquion request: '{}'",toId);

        Transaction tx = new Transaction(wallet.getAddress(), toId, value, data, nonce++, 0, null); //TS?
        //clientLibrary.sendAppendRequest(appendString);
        //byte [] signedTransaaction
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

    private static void exitApplication() {
        LOGGER.info("Client is exiting");
        System.exit(0);
    }
}
