package Server;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class ComponentConnectionPool {
    Queue<PhysicalConnectionDummy> connections; // 한 컴포넌트 내에 존재하는 physical connection Queue (사실상 pool)
    boolean failedMark = false; // 고장난 상황을 표현하기 위해 임의로 추가.
    int componentId;
    Semaphore semaphore;
    protected void acquireAll() throws InterruptedException { semaphore.acquire(connections.size()); }// pool 사이즈 만큼 입장권 발급
    protected void releaseAll(){ semaphore.release(connections.size()); }
    protected void makeComponent(int num, String dbUrl) throws InterruptedException {
        connections = new LinkedBlockingQueue<>(num); // size num 고정, 자료구조 선택
        for (int i=0;i<num;i++){
            PhysicalConnectionDummy d = new PhysicalConnectionDummy();
            d.getConnect(dbUrl);
            connections.offer(d);
        }
        semaphore = new Semaphore(num);
    }
    //이 메서드는 항상 앞에 semaphore.acquire() 필요
    //입장권 받고 써야함. (로직으로 제한되어 있진 않음)
    protected LogicalConnection getConnect(){
        if (failedMark) return null; // getConnect 실패
        try {
            LogicalConnection logicalConnection = new LogicalConnection(connections.poll(), this);
            return logicalConnection;
        }
        catch (Exception e){
            System.out.println("getConnect failed");
            return null;
        }
    }

    protected void setFailedMark() throws InterruptedException {
        this.failedMark = true;
        Random random = new Random();
        Thread.sleep(random.nextInt(10000)); // 0~10s 랜덤 정지
        this.failedMark = false;
    }

    private ComponentConnectionPool(){}
    public ComponentConnectionPool(int id){
        this.componentId = id;
    }

}
