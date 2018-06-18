package com.gryglicki.nonblocking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

class Handler implements Runnable {
    private static final int BUFFER_SIZE = 128;

    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private ByteBuffer byteBuffer;

    public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
        this.byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        //when connection is established and ready for READ
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);
//        selector.wakeup(); //let blocking select() return, to let it see currently registered sockerChannel (needed in multithreaded environment => eg. when you register a first handler and want the event loop to see this new handler)
    }

    @Override
    public void run() {
        try {
            if (selectionKey.isReadable()) {
                read();
            } else if (selectionKey.isWritable()) {
                write();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void read() throws IOException {
        try {
            final int numBytes = socketChannel.read(byteBuffer);
            System.out.println("read(): #bytes read to read buffer = " + numBytes);
            if (numBytes == -1) {
                selectionKey.cancel();
                socketChannel.close();
                System.out.println("read(): client connection might have been dropped!");
            } else {
//                NioEchoServer.getWorkerPool().execute(this::process);
                this.process();
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Process data by Echoing input to output
     */
//    synchronized void process() {
    void process() {
        byteBuffer.flip();
        System.out.println("process(): " + new String(byteBuffer.array(), 0, byteBuffer.limit(), StandardCharsets.UTF_8));

        //Set selectionKey interest to WRITE operation
        selectionKey.interestOps(SelectionKey.OP_WRITE);
//        selectionKey.selector().wakeup(); //let blocking select() return, to let it see currently registered sockerChannel

    }

    private void write() throws IOException {
        int numBytes = 0;
        try {
            numBytes = socketChannel.write(byteBuffer);
            System.out.println("write(): #bytes written from write buffer = " + numBytes);
            if (numBytes > 0) {
                byteBuffer.clear();

                //Set selectionKey interest back to READ operation
                selectionKey.interestOps(SelectionKey.OP_READ);
//                selectionKey.selector().wakeup(); //let blocking select() return, to let it see currently registered sockerChannel
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
