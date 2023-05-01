package com.matheoauer;

import lombok.AllArgsConstructor;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class HandleClientRunnable implements Runnable{

    private String message;
    private DatagramSocket serverSocket;
    private List<Account> accounts;
    private InetAddress clientAddress;
    private int clientPort;

    // Regex pour valider et extraire les informations de chaque action dans le message
    static String regex = "([A-Z]+)(?::([\\w-]+)(?::([\\w-]+))*)?";
    static Pattern pattern = Pattern.compile(regex);


    @Override
    public void run() {
        Account accountConnected = null;
        Matcher matcher = pattern.matcher(message);
        int actionCount = 0;
        StringBuilder response = new StringBuilder();
        while (matcher.find()) {
            actionCount++;
            String action = matcher.group(1);
            List<String> params = new ArrayList<>();
            for (int i = 2; i <= matcher.groupCount(); i++) {
                String param = matcher.group(i);
                if (param != null && !param.isEmpty()) {
                    params.add(param);
                }
            }
            if (action.equals("AUTH") && accountConnected == null) {
                if (params.size() == 2) {
                    // Traitement si le message contient l'action "AUTH" avec 2 paramètres
                    String username = params.get(0);
                    String password = params.get(1);
                    // Check if the username and password are correct
                    for (Account account : accounts) {
                        if (account.username.equals(username) && account.password.equals(password)) {
                            accountConnected = account;
                            System.out.println("Client connected: " + accountConnected.username);
                            break;
                        }
                    }
                } else {
                    // Send client unauthorized message
                    sendMessage("AUTH MALFORMED");
                    return;
                }
            } else {
                if (accountConnected == null) {
                    // Send client unauthorized message
                    sendMessage("AUTH KO");
                    return;
                }
            }

            if (accountConnected == null) {
                throw new RuntimeException("Account not found");
            }

            // Traitement en fonction de l'action
            System.out.println("Action: " + action + " - Params: " + Arrays.toString(params.toArray()));
            switch (action) {
                case "BAL":
                    // Traitement si le message contient l'action "BAL" sans paramètres
                    response.append(handleBAL(accountConnected)).append(" ");
                    break;
                case "TRA":
                    if (params.size() == 2) {
                        // Traitement si le message contient l'action "TRA" avec 2 paramètres
                        String username = params.get(0);
                        String amount = params.get(1);
                        response.append(handleTRA(accountConnected, username, amount)).append(" ");
                    } else {
                        response.append("TRA MALFORMED ");
                    }
                    break;
                case "DEP":
                    if (params.size() == 1) {
                        // Traitement si le message contient l'action "DEP" avec 1 paramètres
                        String amount = params.get(0);
                        response.append(handleDEP(accountConnected, amount)).append(" ");
                    } else {
                        // Traitement si le message "DEP" est mal formé (nombre de paramètres incorrect)
                        response.append("DEP MALFORMED ");
                    }
                    break;
                case "WIT":
                    if (params.size() == 1) {
                        // Traitement si le message contient l'action "WIT" avec 1 paramètres
                        String amount = params.get(0);
                        response.append(handleWIT(accountConnected, amount)).append(" ");
                    } else {
                        // Traitement si le message "WIT" est mal formé (nombre de paramètres incorrect)
                        response.append("WIT MALFORMED ");
                    }
                    break;
                case "AUTH":
                    if (actionCount == 1) {
                        response.append("AUTH OK ");
                    }
                    break;
                default:
                    response.append("UNKNOWN ACTION: ").append(action).append(" ");
                    break;
            }
        }
        if (actionCount == 0) {
            response.append("MALFORMED ");
        }
        sendMessage(response.toString());
    }

    private void sendMessage(String message){
        byte[] responseData = message.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
        try {
            serverSocket.send(responsePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String handleTRA(Account account, String username, String amount) {
        Account accountToTransfer = null;
        for (Account oneAccount : accounts) {
            if (oneAccount.username.equals(username)) {
                accountToTransfer = oneAccount;
                break;
            }
        }
        if (accountToTransfer != null) {
            if (account.amount >= Double.parseDouble(amount)) {
                account.amount -= Double.parseDouble(amount);
                accountToTransfer.amount += Double.parseDouble(amount);
                return "TRA OK";
            } else {
                return "TRA MISSING FUND";
            }
        } else {
            return "TRA UNKNOWN ACCOUNT";
        }
    }

    private String handleBAL(Account account) {
        return "BAL " + account.amount;
    }

    private String handleDEP(Account account, String amount) {
        account.amount += Double.parseDouble(amount);
        return "DEP OK";
    }

    private String handleWIT(Account account, String amount) {
        if (account.amount >= Double.parseDouble(amount)) {
            account.amount -= Double.parseDouble(amount);
            return "WIT OK";
        } else {
            return "WIT MISSING FUND";
        }
    }
}
