package com.sec.depchain.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ClientApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApplication.class);
    private static final int DEBUG_MODE = 1;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <clientId>");
            System.exit(1);
        }

        int clientId = parseClientId(args[0]);
        ClientLibrary clientLibrary = new ClientLibrary(clientId);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered. Exiting gracefully...");
            exitApplication(null, clientLibrary);
        }));

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

    private static void setupInputLoop(ClientLibrary clientLibrary) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("[INFO] Enter 'append <string>' to append, or 'exit' to quit:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine().trim();
                String[] caseArgs = input.split(" ", 2);

                switch (caseArgs[0].toLowerCase()) {
                    case "exit":
                        exitApplication(scanner, clientLibrary);
                        return;

                    case "append":
                        handleAppendRequest(caseArgs, clientLibrary);
                        break;

                    default:
                        System.out.println("[ERROR] Invalid command. Use 'append <string>' or 'exit'.");
                        break;
                }
            }
        }
    }

    private static void handleAppendRequest(String[] caseArgs, ClientLibrary clientLibrary) {
        if (caseArgs.length < 2) {
            System.out.println("[ERROR] Please provide a string to append.");
            return;
        }

        String appendString = caseArgs[1];
        CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

        clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
            String message = result
                    ? String.format("[SUCCESS] '%s' appended at position %s.", appendedString, timestamp)
                    : String.format("[FAILURE] Could not append '%s'.", appendedString);
            System.out.println(message);
            futureResponse.complete(result);
        });

        if (DEBUG_MODE == 1) LOGGER.debug("Sending append request: '{}'", appendString);

        clientLibrary.sendAppendRequest(appendString);

        try {
            futureResponse.get();
        } catch (InterruptedException e) {
            System.err.println("[ERROR] Interrupted while waiting for append response.");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("[ERROR] Execution failed: " + e.getCause());
        }
    }

    private static void exitApplication(Scanner scanner, ClientLibrary clientLibrary) {
        LOGGER.info("Client is exiting");
        if(scanner != null)
        {
        scanner.close();
        }
        clientLibrary.close();
        LOGGER.info("Client library closed.");
        Runtime.getRuntime().halt(0);

        System.exit(0);
    }
}
