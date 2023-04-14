package Server.ClusterScheduler;

import Server.ClusterConnectionPool;

import java.util.Hashtable;
import java.util.Map;

public class AffinityScheduler implements Scheduler{
    Map<String,Integer> recentAddress = new Hashtable<>(); // concurrentHashMap 추후 사용, 성능 더 좋음
    @Override
    public int getScheduleAddress(Integer clientId, int requestId, RoundRobinScheduler rrScheduler){
        synchronized (clientId) { // 클라이언트가 가지고 있는 Integer 인스턴스를 lock 걸어서 동기화 블록 사용, 여러 클라이언트 다같이 이 메서드 이용 가능
            String hashKey = Integer.toString(clientId) + "@" + Integer.toString(requestId); // 추후 hash function 사용
            int componentAddress = -1;
            if (recentAddress.containsKey(hashKey)) { // affinity option 호출이 처음이 아닌 경우, 사용했던 흔적을 참조해 address 반환
                componentAddress = recentAddress.get(hashKey);
                if (ClusterConnectionPool.availableList.contains(componentAddress)) return componentAddress;
                else if (ClusterConnectionPool.removedSet.contains(componentAddress)){
                    System.out.println("Recent Affinity Address is removed");
                    return -1;
                }
                else {
                    System.out.println("Affinity Error : Affinity address has some problem.");
                    return -1;
                }
            }

            // 클라이언트가 affinity 처음 호출하는 경우, round-robin 스케쥴러에서 사용 가능한 address 발급 받음
            componentAddress = rrScheduler.getScheduleAddress();
            if (componentAddress == -1) return -1;
            recentAddress.put(hashKey, componentAddress); // 예전 요청이 계속 남게되는 메모리 낭비 문제는 남아 있음.
            return componentAddress;
        }
    }
}
