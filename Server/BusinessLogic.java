package Server;

import Protocol.Request;
import Protocol.Utilities;
import Utilities.RequestPairData;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BusinessLogic {
    public HashMap<Integer, RequestPairData> requestData = new HashMap<>(); // hash table 쓸 이유 없음.
    // 요청 들어온 [클라이언트 id(Integer), 요청 데이터 (Request)] pair로 저장
    public Queue<Integer> workQueue = new ConcurrentLinkedQueue<>(); // 아직 처리되지 않은 요청 index를 모아둔 큐
    public int requestId = 0; // 요청 구분하기 위해 만든 id (위에 큐에 사용)
    ClusterConnectionPool cluster = ClusterConnectionPool.getInstance();

    public synchronized void addRequest(ByteBuffer byteBuffer, int clientId){
        byteBuffer.flip();
        Request request = (Request) Utilities.convertBytesToObject(byteBuffer); // 요청 데이터인 ByteBuffer 로부터 request 객체 역직렬화
        //이거 역직렬화 여기서 하지말고, 아래 work로 옮겨주고, addRequest함수 synchronized 빼는 것으로 추후 변경 필요.
        requestData.put(this.requestId, new RequestPairData(request,clientId,this.requestId)); // 모든 요청 데이터 client id 와 짝 맺어서 저장해둠.
        workQueue.offer(this.requestId); // 사실상 작업 대기 큐에 추가 (requestId로 요청 구분)
        this.requestId += 1;
    }

    public void work(){
        if (!workQueue.isEmpty()){
            int requestId = workQueue.poll();
            Request request = requestData.get(requestId).getRequestData();
            for (int j = 0; j < request.getNeedNumberOfConnection(); j++) {
                // request로 부터 요청 커넥션 갯수(number of connection) 받아 cluster에 connection pool 연결 요청
                LogicalConnection logicalConnection = cluster.getLogicalConnect(requestId);
                // requestId 를 이용하여 정책에 맞는 logical connection 받음
                logicalConnection.close();
                // 잘 받았으면 바로 닫아줌. (컴포넌트 풀에 커넥션 돌려줌)
            }
        }
        /*
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!workQueue.isEmpty()){
                    int i = workQueue.poll();
                    Request request = (Request) Utilities.convertBytesToObject(requestData.get(i).getRequestData());
                    // 요청 데이터인 ByteBuffer 로부터 request 객체 역직렬화
                    LogicalConnection logicalConnection = cluster.getLogicalConnect(requestData.get(i).getClientId(), request.isAffinity(), request.getId());
                    // request로 부터 affinity option 받아 cluster에 connection pool 연결 요청, logical connection 받음
                    logicalConnection.close();
                    // 잘 받았으면 바로 닫아줌. (컴포넌트 풀에 커넥션 돌려주고 semaphore 여기서 해제됨)
                    //work(); 이거는 처리 안된 요청이 혹시나 남을까봐 아래 프린트하고 같이 테스트 해봄.
                }
                //else System.out.println("#########");
            }
        });

         */
    }

}
