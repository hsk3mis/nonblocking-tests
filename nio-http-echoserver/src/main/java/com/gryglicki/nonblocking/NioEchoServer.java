package com.gryglicki.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NioEchoServer implements Runnable {
    private int port;
//    private static ExecutorService workerPool;

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    public NioEchoServer() throws IOException {
//        this.workerPool = Executors.newFixedThreadPool(10);
        this.port = 9999;
        this.selector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(this.port));
        this.serverSocketChannel.configureBlocking(false);

        //Register serverSocketChannel with selector => selected when new connection incomes
        final SelectionKey serverSocketSelectionKey = this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketSelectionKey.attach(new Acceptor());
    }

//    static ExecutorService getWorkerPool() {
//        return workerPool;
//    }

    //TODO: MessageReader for each Channel to discover message start and message end
    //TODO: MessageReader's internal resizable Buffer to store partial messages => resize by copying the buffer OR by appending new array to the list of arrays that the buffer consists of OR maybe a message has a TLV format (Type, Length, Value) => then you can allocate the buffer at the beginning
    //TODO: MessageWriter for each Channel for writing partial messages

    @Override
    public void run() {
        try {
            //Event Loop
            while (true) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    it.remove();

                    if (sk.isValid()) {
                        Runnable handlerOrAcceptor = (Runnable) sk.attachment(); //handler or acceptor
                        if (handlerOrAcceptor != null) {
                            handlerOrAcceptor.run();
                        }
                    } else {
                        sk.cancel();
                    }

                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                final SocketChannel socketChannel = serverSocketChannel.accept();
                if (socketChannel != null) {
                    System.out.println("accept(): connection established from: " + socketChannel.socket().getInetAddress().toString() + " : " + socketChannel.socket().getPort());
                    new Handler(selector, socketChannel);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
