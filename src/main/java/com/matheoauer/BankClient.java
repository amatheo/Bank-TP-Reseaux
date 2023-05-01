package com.matheoauer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;

public class BankClient {
    private static final int BUFFER_SIZE = 1024;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    public BankClient(String serverHostname, int serverPort) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(serverHostname);
        this.serverPort = serverPort;
    }

    public void sendRequest(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
        socket.send(packet);
    }

    public String getResponse() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        BankClient client = new BankClient("localhost", BankServer.PORT);

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String input;
        do {
            System.out.print("Enter message: ");
            input = userInput.readLine();
            client.sendRequest(input);
            String response = client.getResponse();
            System.out.println("Server response: " + response);

        } while (!input.equals("exit"));

        client.close();
    }
}
