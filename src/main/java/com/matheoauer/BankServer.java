package com.matheoauer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class BankServer {
    private static final int BUFFER_SIZE = 1024;
    public static final int PORT = 12345;

    private DatagramSocket socket;

    private final List<Account> accounts;

    public BankServer(int port, List<Account> accounts) throws IOException {
        this.socket = new DatagramSocket(port);
        this.accounts = accounts;
        System.out.println("Server started on port " + port);
    }

    public void start() throws IOException {
        while (true) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength());
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            HandleClientRunnable runnable = new HandleClientRunnable(message, socket, accounts, clientAddress, clientPort);
            new Thread(runnable).start();
        }
    }

    public void stop() {
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        // Use Jacksopn to parse the JSON file in accounts.json into a list of Account objects
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("src/main/resources/accounts.json");
        Account[] accounts = objectMapper.readValue(file, Account[].class);
        List<Account> accountList = Arrays.asList(accounts);

        BankServer server = new BankServer(PORT, accountList);
        try {
            server.start();
        } finally {
            server.stop();
        }
    }
}


