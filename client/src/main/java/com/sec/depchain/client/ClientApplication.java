package com.sec.depchain.client;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class ClientApplication {
    public static void main(String[] args) throws Exception {
        // Check if clientId argument is provided
        if (args.length < 1) {
            System.err.println("Usage: <clientId>");
            System.exit(1);
        }

        int clientId;
        try {
            clientId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: <clientId> must be an integer.");
            System.exit(1);
            return;
        }

        // Initialize the ClientLibrary with the provided clientId
        ClientLibrary clientLibrary = new ClientLibrary(clientId);

        // Set up the scanner to read user input
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a string to append (or type 'exit' to quit):");

        // User input loop to send append requests
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] caseArgs = input.split(" ", 2);

            switch (caseArgs[0].toLowerCase()) {
                case "exit":
                    System.out.println("Exiting the application...");
                    scanner.close();
                    return; // Exits the program immediately

                case "append":
                    if(caseArgs.length < 2) {
                        System.out.println("Error: Please provide a string to append.");
                        break;
                    }
                    // Create a CompletableFuture to wait for the callback response
                    CompletableFuture<Boolean> futureResponse = new CompletableFuture<>();

                    // Set a callback to handle the result of the append operation
                    clientLibrary.setDeliverCallback((result, appendedString, timestamp) -> {
                        futureResponse.complete(result);
                        if (result) {
                            System.out.println("Success: The string \"" + appendedString + "\" was appended at position " + timestamp + ".");
                        } else {
                            System.out.println("Failure: The string \"" + appendedString + "\" could not be appended to the blockchain.");
                        }
                    });

                    // Send the append request with the user's input to the ClientLibrary
                    System.out.println("Sending append request with " + caseArgs[1]);
                    clientLibrary.sendAppendRequest(caseArgs[1]);

                    // Wait for the response (blocking call)
                    // futureResponse.get();
                    break;

                default:
                    System.out.println("Invalid input. Please enter 'append' to append a string or 'exit' to quit.");
                    break;
            }
        }
    }
}
