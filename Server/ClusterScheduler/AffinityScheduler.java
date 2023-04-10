package Server.ClusterScheduler;

import Server.ClusterConnectionPool;

import java.util.Hashtable;
import java.util.Map;

public class AffinityScheduler implements Scheduler{
    Map<Integer,Integer> prevIndex = new Hashtable<>(); // concurrentHashMap 추후 사용, 성능 더 좋음
    public int getScheduleIndex(Integer clientId, RoundRobinScheduler rrScheduler){
        synchronized (clientId) { // 클라이언트가 가지고 있는 Integer 인스턴스를 lock 걸어서 동기화 블록 사용, 여러 클라이언트 다같이 이 메서드 이용 가능
            if (prevIndex.containsKey(clientId)) { // affinity option 호출이 처음이 아닌 경우, 사용했던 흔적을 참조해 address 반환
                int componentInd = prevIndex.get(clientId);
                if (ClusterConnectionPool.availableList.contains(componentInd)) return componentInd;
                else {
                    System.out.println("Affinity Error : Affinity index has some problem.");
                    return -1;
                }
            } else {
                // 클라이언트가 affinity 처음 호출하는 경우, round-robin 스케쥴러에서 사용 가능한 address 발급 받음
                int componentInd = rrScheduler.getScheduleIndex();
                if (componentInd == -1) return -1;
                prevIndex.put(clientId, componentInd);
                return componentInd;
            }
        }
    }
}
