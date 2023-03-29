package Client;

import Protocol.*;
import Server.ServerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientService {
    Selector selector;
    SocketChannel socketChannel; // 클라이언트 통신을 위해 Socket 필드 선언
    AtomicInteger count = new AtomicInteger();
    boolean ready = true;
    int requestCount = 0;

    public ClientService(){
        this.startClient();
    }
    // 연결 시작 코드
    private void startClient() {
        // 작업 스레드 생성 => connect(), receive() 에서 블로킹이 일어나기 때문
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open(); // 통신용 블로킹 SocketChannel 생성
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress("localhost", 5001)); // localhost 5001 포트로 연결 요청을 한다.
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            if (socketChannel.isOpen()) {
                stopClient(); // SocketChannel이 닫혀있지 않으면 stopClient() 메소드 호출
            }
            return;
        }
        Thread t = new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        int keyCount = selector.select();
                        if (keyCount == 0) {
                            continue;
                        }
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();

                        Iterator<SelectionKey> iterator = selectedKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            if (selectionKey.isReadable()) {
                                ready = true;
                            }
                            else if (selectionKey.isConnectable()) {
                                socketChannel.finishConnect();
                                selectionKey.interestOps(SelectionKey.OP_READ);

                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        if (socketChannel.isOpen()) {
                            stopClient();
                        }
                        break;
                    }
                }
            }
        };
        t.start();
    }

    // 연결 끊기 코드
    void stopClient() {
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close(); // socketChannel 필드가 null이 아니고, 현재 닫혀있지 않을 경우 SocketChannel을 닫는다
            }
        } catch (IOException e) {
        }
    }

    protected int getConnection() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int result = 0;
        while (requestCount > 0) {
            if (ready) {
                try {
                    count.set(count.get() + 1);
                    Future f = executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Request request = new Request("getConnection");
                                ByteBuffer byteBuffer = Utilities.convertObjectToBytes(request);
                                socketChannel.write(byteBuffer);
                                byteBuffer.clear();
                                //System.out.println("getConnection "+Thread.currentThread().getName());
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                    });
                    requestCount -= 1;
                    ready = false;
                    result += 1;
                    f.get();
                } catch (Exception e) {
                    System.out.println("getConnection Failed " + Thread.currentThread().getName());
                    System.out.println(e.getMessage());
                    stopClient();
                    break;
                }
            }
            else Thread.yield();
        }
        return result;
    }

    protected int getCount() {
        return count.get();
    }
}
