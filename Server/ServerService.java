package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import Utilities.*;
public class ServerService {
    Selector selector;
    ServerSocketChannel serverSocketChannel; // 클라이언트 연결을 수락하는 ServerSocketChannel 필드 선언
    Map<Integer, Client> connections = new Hashtable<>(); // thread safe, hash 구조 이용한 특정 client O(1) 안에 탐색
    AtomicInteger clientAtomicId = new AtomicInteger(); // 서버에서 클라이언트 마다 부여하는 고유 번호
    BufferPool bufferPool = new BufferPool(); // request 받는 buffer 를 미리 만들어 pool 형태로 재사용
    BusinessLogic businessLogic = new BusinessLogic(); // 추후 work thread 실행하는 객체

    // 서버 시작 시 호출하는 메소드
    public void startServer() {
        bufferPool.addBufferPool(1000000,93); // request bytebuffer size = 93 (current)
        //현재 따로 리퀘스트 사이즈 체크하는 과정이 없어 리퀘스트 변경하거나 값 바꿀 경우 IOException
        //추후 프로토콜 작성 시 해결 필요
        try {
            selector = Selector.open(); // selector 생성
            serverSocketChannel = ServerSocketChannel.open(); // ServerSocketChannel 생성
            serverSocketChannel.configureBlocking(false); // 넌블로킹 모드 설정
            serverSocketChannel.bind(new InetSocketAddress("localhost",6565));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
            return;
        }

        Thread t = new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        int keyCount = selector.select(); // blocking
                        if (keyCount == 0) {
                            continue;
                        }
                        Set<SelectionKey> selectedKeys = selector.selectedKeys();

                        Iterator<SelectionKey> iterator = selectedKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            if (selectionKey.isAcceptable()) accept(selectionKey);
                            else if (selectionKey.isReadable()) {
                                selectionKey.interestOps(0); // 선택된 셀렉션키의 잘못된 read 반복을 막기 위해 같은 키가
                                // 바로 다시 선택되지 않게 돌려버림 (현재 selector 싱글 스레드, 동기화 필요 x)
                                Thread readThread = new Thread(){
                                    @Override
                                    public void run() {
                                        int cid = (int) selectionKey.attachment(); // 키에 첨부된 client id 값 받아옴
                                        connections.get(cid).receive(selectionKey); // id값 이용해 hashtable 참조해
                                        // 특정 client socket channel 받은 후에 receive (안에서 read랑 OP_READ 설정 해줌)
                                    }
                                };
                                readThread.start();
                                // 추후 쓰레드풀 커스텀해서 변경 필요, 현재는 쓰레드 터질 위험 있지만
                                // 테스트를 위한 성능 때문에 놔둠.
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Selector thread error");
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
                    if (log == -1) {
                        try {
                            Thread.sleep(5000); // 특정 시간(현재는 5초) 간격으로
                            // failedList에 있는 component pool 들이 살아있는 지 확인하는 함수(failOver) 호출
                        } catch (InterruptedException e) {
                            System.out.println("failover thread interrupted");
                        }
                    }
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
            int cId = clientAtomicId.getAndIncrement(); // client 고유 id 부여
            Client client = new Client(cId, socketChannel); // 아래 선언된 내부 클래스인 Client 선언 (채널과 1대1 관계)
            connections.put(cId,client); // 나중에 빠른 탐색을 위한 Hash table 만들기 위해 값 넣어줌.
            String message = "[연결 수락: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
            System.out.println(message);
        } catch (Exception e) {
            if (serverSocketChannel.isOpen()) {
                stopServer();
            }
        }
    }

    // 연결된 Client를 표현, Client를 내부 클래스로 선언 (서버에서 연결된 클라이언트(채널)을 구분하기 위해 만듬)
    // 헷갈려서 추후 Session 으로 이름 바꿀까 고민중.
    class Client {
        Integer clientId; // 고유 id 구분, 추후 affinity 동기화를 위해 일부러 Integer 객체로 인스턴스 하나만 가지게 만듬.
        SocketChannel socketChannel;
        Client(int id, SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            this.clientId = Integer.valueOf(id); // 초기화 시 Integer 인스턴스 하나 설정 (고유 int 값)
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this.clientId); // 셀렉션키에 id 첨부
        }

        void receive(SelectionKey selectionKey) {
            try {
                ByteBuffer byteBuffer = bufferPool.getBuffer(); // 버퍼 풀에서 미리 생성해둔 버퍼 가져옴.
                int byteCount = socketChannel.read(byteBuffer);
                // 클라이언트가 정상적으로 SocketChannel의 close()를 호출했을 경우
                // read() 메소드는 -1을 리턴하고 IOException을 강제로 발생
                if (byteCount == -1) {
                    System.out.printf("client %d closed",clientId);
                    throw new IOException();
                }
                else if (byteCount == 0) System.out.println("#### read test ####");
                // 쓸모없는 read 반복이 실행되는 지 확인하는 프린트 디버깅
                else if (byteCount > 0) {
                    selectionKey.interestOps(SelectionKey.OP_READ); //read로 request하나 확인되면 바로 다시 read 받을 준비
                    selector.wakeup(); // blocking 되어 있는 select() 함수 깨워줌. (사실상 non-block select() 재호출 느낌)
                    businessLogic.addRequest(byteBuffer, clientId); //request data 저장
                    businessLogic.work(); //work thread 에서 따로 저장된 request 응답 처리
                    String message = "[요청 처리: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                    //System.out.println(message);
                    // 정상적으로 데이터를 받았을 경우 "[요청처리: 클라이언트 IP: 작업 스레드 이름]"으로 구성된 문자열 출력
                    byteBuffer.clear();
                    bufferPool.releaseBuffer(byteBuffer); // 사용한 버퍼 다시 풀로 되돌려줌
                }

            } catch (Exception e) {
                try {
                    String message = "[클라이언트 통신 안됨: " + socketChannel.getRemoteAddress() + ": " + Thread.currentThread().getName() + "]";
                    System.out.println(message);
                } catch (IOException e2) {

                }
            }
        }
    }
}
