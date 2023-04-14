package Server.ClusterScheduler;

import Server.ClusterConnectionPool;

public class FailbackScheduler implements Scheduler{
    @Override
    public int getScheduleAddress(){ // 단순하게 failbackqueue 에 있는 복구된 component address 를 반환
        try {
            int componentAddress = ClusterConnectionPool.failbackQueue.poll();
            ClusterConnectionPool.availableList.add(componentAddress);
            return componentAddress;
        } catch (NullPointerException e){
            System.out.println("failback queue is empty");
            return -1;
        }
    }
}
