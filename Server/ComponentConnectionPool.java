package Server;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class ComponentConnectionPool {
    LinkedBlockingQueue<PhysicalConnectionDummy> connections; // 한 컴포넌트 내에 존재하는 physical connection Queue (사실상 pool)
    private int sizeOfPool;
    int componentId;
    public boolean isFull(){ return connections.size() == this.sizeOfPool; }
    protected void makeComponent(int num, String dbUrl) throws InterruptedException {
        connections = new LinkedBlockingQueue<>(num); // size num 고정, 자료구조 선택
        for (int i=0;i<num;i++){
            PhysicalConnectionDummy d = new PhysicalConnectionDummy();
            d.getConnect(dbUrl);
            connections.offer(d);
        }
        this.sizeOfPool = num;
    }

    protected LogicalConnection getConnect(){
        if (ClusterConnectionPool.statusMap.get(componentId).isFailed()) return null; // getConnect 실패
        try {
            LogicalConnection logicalConnection = new LogicalConnection(connections.take(), this);
            return logicalConnection;
        }
        catch (Exception e){
            System.out.println("getConnect failed");
            return null;
        }
    }
/*
    protected void setFailedMark() throws InterruptedException {
        this.failedMark = true;
        Random random = new Random();
        Thread.sleep(random.nextInt(10000)); // 0~10s 랜덤 정지
        this.failedMark = false;
    }
*/
    private ComponentConnectionPool(){}
    public ComponentConnectionPool(int id){
        this.componentId = id;
    }

}
