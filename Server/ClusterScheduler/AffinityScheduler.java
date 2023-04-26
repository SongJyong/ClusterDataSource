package Server.ClusterScheduler;

import java.util.HashMap;
import java.util.Map;

public class AffinityScheduler implements Scheduler{
    Map<Integer,Integer> recentAddress = new HashMap<>(); // concurrentHashMap 추후 사용, 성능 더 좋음
    @Override
    public int getScheduleAddress(int requestId){
        int componentAddress = -1;
        if (recentAddress.containsKey(requestId)) componentAddress = recentAddress.get(requestId);
        return componentAddress;
        // 같은 요청에 커넥션 응답이 처음이 아닌 경우, 사용했던 흔적을 참조해 address 반환
    }
}
