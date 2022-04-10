package com.Minin.server;


import com.Minin.server.chat.MyServer;

public class ServerApp {

    private static final int DEFAULT_PORT = 8189;

    public static void main(String[] args) {
        new MyServer().start(DEFAULT_PORT);
    }
}
