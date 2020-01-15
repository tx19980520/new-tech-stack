package com.ty0207;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    String hostIp;
    int hostPort;
 
    public Client(String hostIp, int hostPort) {
        this.hostIp = hostIp;
        this.hostPort = hostPort;
    }
 
    public void runClient() throws IOException {
        Socket clientSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
 
        try {
            clientSocket = new Socket(hostIp, hostPort);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + hostIp);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't connect to: " + hostIp);
            System.exit(1);
        }
 
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
 
        System.out.println("Client connected to host : " + hostIp + " port: " + hostPort);
        System.out.println("Type (\"Bye\" to quit)");
        System.out.println("Tell what your name is to the Server.....");
 
        while ((userInput = stdIn.readLine()) != null) {
 
            out.println(userInput);
 
            // Break when client says Bye.
            if (userInput.equalsIgnoreCase("Bye"))
                break;
 
            System.out.println("Server says: " + in.readLine());
        }
 
        out.close();
        in.close();
        stdIn.close();
        clientSocket.close();
    }

    public static void main(String[] args) throws IOException {
        Client c = new Client("127.0.0.1", 9090);
        c.runClient();
    }
}