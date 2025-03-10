package com.sec.depchain.common;

import java.net.*;

public class FairLossLinks {
    private DatagramSocket socket;
    private int port;
    private DeliverCallback deliverCallback;

    // Interface funcional para definir a callback
    public interface DeliverCallback {
        void deliverReceivedMessage(String srcIP, int srcPort, String message);
    }

    public FairLossLinks(int port) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
    }

    // Define a callback para entrega de mensagens
    public void setDeliverCallback(DeliverCallback callback) {
        this.deliverCallback = callback;
    }

    // Envia uma mensagem para um destino específico 
    public void send(String destIP, int destPort, String message) throws Exception {
        InetAddress destAddress = InetAddress.getByName(destIP);
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, destPort);
        socket.send(packet);
        System.out.println("Sent: '" + message + "' to " + destIP + ":" + destPort);
    }

    // Método para receber mensagens e chamar a callback
    public void deliver() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    String srcIP = packet.getAddress().getHostAddress();
                    int srcPort = packet.getPort();

                    // System.out.println("\nReceived from " + srcIP + ":" + srcPort + " -> " +
                    // message);
                    System.out.print("> "); // Mostrar prompt novamente após receber

                    // Se a callback foi definida, chamá-la
                    if (deliverCallback != null) {
                        deliverCallback.deliverReceivedMessage(srcIP, srcPort, message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
