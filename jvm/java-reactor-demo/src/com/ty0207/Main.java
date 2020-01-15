package com.ty0207;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Server server = new Server(9090, false);
        new Thread(server).start();
    }
}
