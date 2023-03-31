package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import Utilities.*;
public class ServerService {
    Selector selector;
    ServerSocketChannel serverSocketChannel; // 클라이언트 연결을 수락하는 ServerSocketChannel 필드 선언
    Map<Integer, Client> connections = new Hashtable<>(); // thread safe
    AtomicInteger clientId = new AtomicInteger();
    BufferPool bufferPool = new BufferPool();

    // 연결된 클라이언트를 저장하는 List<Client> 타입의 connections 필드 선언
    // 스레드에 안전한 Vector로 초기화
    public BusinessLogic businessLogic = new BusinessLogic();
    private ServerService(){}
    private static ServerService serverService;
    public static ServerService getInstance(){
        if(serverService == null){
            synchronized (ServerService.class){
                if(serverService == null) serverService = new ServerService();
            }
        }
        return serverService;
    } // double-checked locking , singleton 보장 (multi-thread 환경)

    // 서버 시작 시 호출되는 메소드
    public void startServer() {
        bufferPool.addBufferPool(2000000,90);
        try {
            selector = Selector.open(); // selector 생성
            serverSocketChannel = ServerSocketChannel.open(); // ServerSocketChannel 생성
            serverSocketChannel.configureBlocking(false); // 넌블로킹 모드 설정
            serverSocketChannel.bind(new InetSocketAddress("localhost",6565));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // selector를 등록, 작업 유형을 OP_ACCEPT로 지정
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        Thread t = new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        int keyCount = selector.select(); // blocking
                        if (keyCount == 0) {
                            continue;
                        }
                        Set<SelectionKey> selectedKeys = selector.selectedKeys(); // thread-unsafe

                        Iterator<SelectionKey> iterator = selectedKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            if (selectionKey.isAcceptable()) accept(selectionKey);
                            else if (selectionKey.isReadable()) {
                                selectionKey.interestOps(0);
                                executorService.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        int cid = (int) selectionKey.attachment();
                                        connections.get(cid).receive(selectionKey);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        if (serverSocketChannel.isOpen()) {
                            stopServer();
                        }
                        break;
                    }
                }
            }
        };
        t.start();
        Thread failOver = new Thread(){
            @Override
            public void run() {
                while (true) {
                    int log = businessLogic.cluster.failOver();
                    if (log == -1) Thread.yield();
                    else{
                        System.out.printf("connection pool id : %d failover completed \n", log);
                    }
                }
            }
        };
        failOver.start();
    }

    // 서버 종료 시 호출되는 메소드
    void stopServer() {
        try {
            Iterator<Map.Entry<Integer,Client>> iterator = connections.entrySet().iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next().getValue();
                client.socketChannel.close();
                iterator.remove();
            }
            if (serverSocketChannel!=null && serverSocketChannel.isOpen()) {
                serverSocketChannel.close();
            }
            if (selector!=null && selector.isOpen()) {
                selector.close();
            }
        } catch (Exception e) {

        }
    }

    void accept(SelectionKey selectionKey) {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            Client client = new Client(clientId.get(), socketChannel);
            connections.put(clientId.get(),client);
            clientId.set(clientId.get()+1);
            String message = "[연결 수락: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
            System.out.println(message);
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
        }
    }

    // 연결된 Client를 표현, Client를 내부 클래스로 선언
    class Client {
        int clientId; // session id 개념 구분자.
        SocketChannel socketChannel;
        Client(int id, SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            this.clientId = id;
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this.clientId); // id 첨부 구분
        }

        void receive(SelectionKey selectionKey) {
            try {
                ByteBuffer byteBuffer = bufferPool.getBuffer();
                int byteCount = socketChannel.read(byteBuffer);
                // 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우 => read() 메소드는 -1을 리턴하고 IOException을 강제로 발생
                if (byteCount == -1) {
                    System.out.printf("client %d closed",clientId);
                    throw new IOException();
                }
                else if (byteCount == 0) System.out.println("####");
                else if (byteCount > 0) {
                    selectionKey.interestOps(SelectionKey.OP_READ);
                    selector.wakeup();
                    businessLogic.addRequest(byteBuffer);
                    businessLogic.work();
                    String message = "[요청 처리: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                    System.out.println(message);
                    // 정상적으로 데이터를 받았을 경우 "[요청처리: 클라이언트 IP: 작업 스레드 이름]"으로 구성된 문자열 출력
                    byteBuffer.clear();
                    bufferPool.releaseBuffer(byteBuffer);
                }

            } catch (Exception e) {
                try {
                    //connections.remove(this);
                    String message = "[클라이언트 통신 안됨: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                    System.out.println(message);
                    //socketChannel.close();
                } catch (IOException e2) {

                }
            }
        }
    }
}
