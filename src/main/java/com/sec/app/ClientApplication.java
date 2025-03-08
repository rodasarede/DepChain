package com.sec.app;

import java.util.Scanner;

public class ClientApplication {
    public static void main(String[] args) throws Exception {
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

        ClientLibrary clientLibrary = new ClientLibrary(clientId);

        // Set the callback to handle responses from ClientLibrary
        clientLibrary.setCallback(message -> {
            System.out.println("ClientApplication Received from client Library: " + message);
        });

        // User input loop
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a string to append (or type 'exit' to quit):");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                break;
            }

            clientLibrary.sendAppendRequest(input);
            System.out.println("String sent to clientLibrary: " + input);
        }

        scanner.close();
    }
}
