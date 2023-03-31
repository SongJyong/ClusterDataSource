package Server;

import Protocol.Request;
import Protocol.Utilities;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BusinessLogic {
    public List<ByteBuffer> requestData = new Vector<>();
    public Queue<Integer> workQueue = new ConcurrentLinkedQueue<>();
    public AtomicInteger index = new AtomicInteger();
    ClusterConnectionPool cluster = ClusterConnectionPool.getInstance();
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public synchronized void addRequest(ByteBuffer byteBuffer){
        byteBuffer.flip();
        requestData.add(byteBuffer);
        workQueue.offer(index.get());
        index.set(index.get()+1);
    }

    public void work(){
        //ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!workQueue.isEmpty()){
                    int i = workQueue.poll();
                    Request request = (Request) Utilities.convertBytesToObject(requestData.get(i));
                    //System.out.printf("%d \n",request.getId()); // 추후 비즈니스 로직 변경
                    cluster.getConnect();
                    work();
                }
            }
        });
    }

}
