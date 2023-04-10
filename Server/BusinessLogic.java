package Server;

import Protocol.Request;
import Protocol.Utilities;
import Utilities.Pair;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BusinessLogic {
    public List<Pair> requestData = new Vector<>();
    // 요청 들어온 [클라이언트 id(Integer), 요청 데이터 (ByteBuffer)] pair로 저장
    public Queue<Integer> workQueue = new ConcurrentLinkedQueue<>(); // 아직 처리되지 않은 요청 index를 모아둔 큐
    public AtomicInteger index = new AtomicInteger(); // 요청 구분하기 위해 만든 index (위에 큐에 사용)
    ClusterConnectionPool cluster = ClusterConnectionPool.getInstance();
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public synchronized void addRequest(ByteBuffer byteBuffer, Integer clientId){
        byteBuffer.flip();
        requestData.add(new Pair(byteBuffer,clientId)); // 모든 요청 데이터 client id 와 짝 맺어서 저장해둠.
        workQueue.offer(index.getAndIncrement()); // 사실상 작업 대기 큐에 추가 (index로 요청 구분)
    }

    public void work(){
        // 쓰레드 풀 따로 커스텀 없이 그냥 사용중
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!workQueue.isEmpty()){
                    int i = workQueue.poll();
                    Request request = (Request) Utilities.convertBytesToObject(requestData.get(i).getRequestData());
                    // 요청 데이터인 ByteBuffer 로부터 request 객체 역직렬화
                    LogicalConnection logicalConnection = cluster.getLogicalConnect(requestData.get(i).getClientId(),request.isAffinity());
                    // request로 부터 affinity option 받아 cluster에 connection pool 연결 요청, logical connection 받음
                    logicalConnection.close();
                    // 잘 받았으면 바로 닫아줌. (컴포넌트 풀에 커넥션 돌려주고 semaphore 여기서 해제됨)
                    //work(); 이거는 처리 안된 요청이 혹시나 남을까봐 아래 프린트하고 같이 테스트 해봄.
                }
                //else System.out.println("#########");
            }
        });
    }

}
