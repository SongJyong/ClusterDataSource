package Client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientService {
    SocketChannel socketChannel; // 클라이언트 통신을 위해 Socket 필드 선언
    AtomicInteger count = new AtomicInteger();
    public ClientService(){
        this.startClient();
    }
    // 연결 시작 코드
    private void startClient() {
        // 작업 스레드 생성 => connect(), receive() 에서 블로킹이 일어나기 때문
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    socketChannel = SocketChannel.open(); // 통신용 블로킹 SocketChannel 생성
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress("localhost", 5001)); // localhost 5001 포트로 연결 요청을 한다.
                } catch (Exception e) {
                    if (socketChannel.isOpen()) {
                        stopClient(); // SocketChannel이 닫혀있지 않으면 stopClient() 메소드 호출
                    }
                    // return; // 작업 종료
                }
            }
        };
        thread.start(); // 작업 스레드를 시작한다.
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

    protected int getConnection(int m) {
        int result = 0;
        try {
            for (int i=0; i<m; i++) {
                Charset charset = Charset.forName("UTF-8");
                ByteBuffer byteBuffer = charset.encode("getConnection");
                socketChannel.write(byteBuffer);
                count.set(count.get() + 1);
                System.out.println("getConnection"+Thread.currentThread().getName());
                Thread.sleep(5); // 한 채널에 너무 빠른 입력 방지 (같은 클라이언트가 동시에 여러 요청하는건 말이 안됨) callback , sync 처리 필요?
                result += 1;
            }
        } catch (Exception e) {
            System.out.println("getConnection Failed");
            System.out.println(e.getMessage());
            stopClient();
        }
        return result;
    }

    protected int getCount() {
        return count.get();
    }
}
