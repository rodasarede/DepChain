package com.sec.depchain.client;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Client application that allows users to append strings to the blockchain.
 * Communicates with the ClientLibrary to send and receive responses.
 */
public class ClientApplication {
    public static void main(String[] args) throws Exception {
        // Verify if the clientId argument is provided
        if (args.length < 1) {
            System.err.println("Usage: <clientId>");
            System.exit(1);
        }

        int clientId;
        try {
            clientId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] <clientId> must be an integer.");
            System.exit(1);
            return;
        }

        // Initialize the ClientLibrary with the provided clientId
        ClientLibrary clientLibrary = new ClientLibrary(clientId);

        // Set up the scanner to read user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("[INFO] Enter a string to append (or type 'exit' to quit):");

        // User input loop to send append requests
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] caseArgs = input.split(" ", 2);

            switch (caseArgs[0].toLowerCase()) {
                case "exit":
                    System.out.println("[INFO] Exiting the application...");
                    scanner.close();
                    return; // Exit the program immediately

                case "append":
                    if (caseArgs.length < 2) {
                        System.out.println("[ERROR] Please provide a string to append.");
                        break;
                    }

                    // Create a CompletableFuture to wait for the callback response
                    CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

                    // Set a callback to handle the result of the append operation
                    clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
                        futureResponse.complete(result);
                        if (result) {
                            System.out.println("[SUCCESS] The string '" + appendedString + "' was appended at position " + timestamp + ".");
                        } else {
                            System.out.println("[FAILURE] The string '" + appendedString + "' could not be appended to the blockchain.");
                        }
                    });

                    // Send the append request with the user's input to the ClientLibrary
                    System.out.println("[INFO] Sending append request with '" + caseArgs[1] + "'.");
                    clientLibrary.sendAppendRequest(caseArgs[1]);

                    // Wait for the response (blocking call)
                    futureResponse.get();
                    break;

                default:
                    System.out.println("[ERROR] Invalid input. Please enter 'append' to append a string or 'exit' to quit.");
                    break;
            }
        }
    }
}