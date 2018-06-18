package com.gryglicki.nonblocking;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        new Thread(new NioEchoServer()).start();
        System.out.println("NIO Echo Server: Started");

    }
}


