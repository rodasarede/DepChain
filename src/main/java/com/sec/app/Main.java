package com.sec.app;
import java.util.Scanner;

public class Main {
    private static void onPerfectLinksDelivery(String srcIP, int srcPort, String message) {
        System.out.println("Perfect Deliver from " + srcIP + ":" + srcPort + " -> " + message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java Main <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        PerfectLinks sl = new PerfectLinks(port);

        sl.setDeliverCallback(Main::onPerfectLinksDelivery);

        Scanner scanner = new Scanner(System.in);
        System.out.println("PerfectLinks running on port " + port);
        System.out.println("Usage: <ip_address>:<port> <message>");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                break;
            }

            // Parse input: "<ip>:<port> <message>"
            String[] parts = input.split(" ", 2);
            if (parts.length < 2) {
                System.out.println("Invalid format! Use: <ip>:<port> <message>");
                continue;
            }

            String[] address = parts[0].split(":");
            if (address.length != 2) {
                System.out.println("Invalid address format! Use: <ip>:<port>");
                continue;
            }

            String destIP = address[0];
            int destPort;
            try {
                destPort = Integer.parseInt(address[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number!");
                continue;
            }

            String message = parts[1];
            sl.send(destIP, destPort, message);
        }

        scanner.close();
    }
}
