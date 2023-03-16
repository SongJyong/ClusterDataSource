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
import java.util.concurrent.Future;

public class ServerService {
    Selector selector;
    ServerSocketChannel serverSocketChannel; // 클라이언트 연결을 수락하는 ServerSocketChannel 필드 선언
    List<Client> connections = new Vector<Client>();
    // 연결된 클라이언트를 저장하는 List<Client> 타입의 connections 필드 선언
    // 스레드에 안전한 Vector로 초기화
    ClusterConnectionPool cluster = ClusterConnectionPool.getInstance();
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
        try {
            selector = Selector.open(); // selector 생성
            serverSocketChannel = ServerSocketChannel.open(); // ServerSocketChannel 생성
            serverSocketChannel.configureBlocking(false); // 넌블로킹 모드 설정
            serverSocketChannel.bind(new InetSocketAddress(5001));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); // selector를 등록하되 작업 유형을 OP_ACCEPT로 지정
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
            return;
        }
        class AcceptThread extends Thread{
            SelectionKey s = null;
            public void setAccept(SelectionKey s){this.s = s;}
            @Override
            public void run(){
                while (true){
                    if (s != null && s.isAcceptable()){
                       accept(s);
                       s = null;
                    }
                    else Thread.yield();
                }
            }
        }
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //Runnable task = new Runnable() {
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
                            if (selectionKey.isAcceptable()) acceptThread.setAccept(selectionKey);
                            else if (selectionKey.isReadable()) {
                                Future future = executorService.submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        Client client = (Client) selectionKey.attachment();
                                        client.receive();
                                    }
                                });
                                future.get();
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
        //executorService.submit(task);
        t.start();
    }

    // 서버 종료 시 호출되는 메소드
    void stopServer() {
        try {
            Iterator<Client> iterator = connections.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
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
            String message = "[연결 수락: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
            System.out.println(message);

            Client client = new Client(socketChannel);
            connections.add(client);
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
        }
    }

    // 연결된 Client를 표현 (데이터 통신 코드를 포함한다.)
    // Client를 내부 클래스로 선언
    class Client {
        SocketChannel socketChannel;

        Client(SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);
        }

        synchronized void receive() {
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(100);

                // 클라이언트가 비정상 종료를 했을 경우 IOException 발생
                int byteCount = socketChannel.read(byteBuffer);
                // 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우 => read() 메소드는 -1을 리턴하고 IOException을 강제로 발생
                if (byteCount == -1) {
                    throw new IOException();
                }

                String message = "[요청 처리: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                System.out.println(message);
                cluster.getConnect();
                // 정상적으로 데이터를 받았을 경우 "[요청처리: 클라이언트 IP: 작업 스레드 이름]"으로 구성된 문자열 출력

            } catch (Exception e) {
                try {
                    connections.remove(this);
                    String message = "[클라이언트 통신 안됨: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                    System.out.println(message);
                    // "[클라이언트 통신 안됨: 클라이언트 IP: 작업 스레드 이름]" 으로 구성된 문자열 생성
                    socketChannel.close();
                } catch (IOException e2) {

                }
            }
        }
    }
}
