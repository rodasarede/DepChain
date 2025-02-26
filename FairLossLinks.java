import java.net.*;
import java.util.Scanner;

public class FairLossLinks {
    private DatagramSocket socket;
    private int port;

    public FairLossLinks(int port) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
    }

    // Send a message to a specific destination
    public void send(int destPort, String message) throws Exception {
        InetAddress destAddress = InetAddress.getByName("127.0.0.1");
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, destPort);
        socket.send(packet);
        System.out.println("Sent: '" + message + "' to port " + destPort);
    }

    // Deliver messages (receive method)
    public void deliver() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("\nReceived from port " + packet.getPort() + " -> " + message);
                    System.out.print("> "); // Show prompt again after receiving
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java FairLossLinks <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        FairLossLinks fl = new FairLossLinks(port);

        // Start listening
        fl.deliver();

        // Read user input for sending messages
        Scanner scanner = new Scanner(System.in);
        System.out.println("Fair Loss Links running on port " + port);
        System.out.println("Enter messages in the format: <dest_port> <message>");

        while (true) {
            System.out.print("> ");
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                // Split input into destination port and message
                String[] parts = input.split(" ", 2);
                if (parts.length < 2) {
                    System.out.println("Invalid input. Use: <dest_port> <message>");
                    continue;
                }

                try {
                    int destPort = Integer.parseInt(parts[0]);
                    String message = parts[1];
                    fl.send(destPort, message);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number.");
                }
            }
        }
    }
}
