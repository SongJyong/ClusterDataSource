package Server.ClusterScheduler;

import Server.ClusterConnectionPool;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinScheduler implements Scheduler{
    private AtomicInteger loadBalanceIndex = new AtomicInteger(); // 골고루 분배할 수 있게 내부에서 순서를 가지고 있어야 함.
    @Override
    public int getScheduleAddress(){
        int len = ClusterConnectionPool.availableList.size();
        if (len == 0){
            System.out.println("Connection Error : anyone component pool doesn't exist");
            return -1;
        }
        int ind = loadBalanceIndex.getAndIncrement()%len; // availableList 사이즈가 일정하지 않기 때문에 항상 확인하고 참조
        return ClusterConnectionPool.availableList.get(ind); // 내부 순서에 맞는 인덱스로 availableList 참조해 address 반환
    }
}
