package Server.ClusterScheduler;

import Server.ClusterConnectionPool;
import java.util.concurrent.ConcurrentHashMap;

public class AffinityScheduler implements Scheduler{
    ConcurrentHashMap<Integer,Integer> recentAddress = new ConcurrentHashMap<>(); // concurrentHashMap 추후 사용, 성능 더 좋음
    @Override
    public int getScheduleAddress(int requestId){
        if (recentAddress.containsKey(requestId)) {
            int componentAddress = recentAddress.get(requestId);
            if (ClusterConnectionPool.statusMap.get(componentAddress).isPrimaryAvailable()) return componentAddress;
            if (ClusterConnectionPool.statusMap.get(componentAddress).isSubAvailable()
                    && ClusterConnectionPool.primaryAvailableLinkedList.length.get() == 0) return componentAddress;
        }
        return -1;
        // 같은 요청에 커넥션 응답이 처음이 아닌 경우, 사용했던 흔적을 참조해 address 반환
    }
}
