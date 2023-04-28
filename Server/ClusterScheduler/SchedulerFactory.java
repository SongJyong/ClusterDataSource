package Server.ClusterScheduler;

import Server.ClusterConnectionPool;

public class SchedulerFactory {
    // 스케쥴러들 인스턴스 하나만 가지게 여기서 초기화, 이후 필요시 매개변수로 넘겨줌
    RoundRobinScheduler roundRobinScheduler = new RoundRobinScheduler(); // avaialbleList 이용한 기본 스케쥴러 (내부 index 가지고 있음)
    AffinityScheduler affinityScheduler = new AffinityScheduler(); // 요청시 affinity option 인 경우 같은 componentId 스케쥴링 (내부 해시테이블)

    public int getScheduleAddress(int requestId){
        int componentAddress = affinityScheduler.getScheduleAddress(requestId); // affinity 최우선 확인
        if (componentAddress == -1) componentAddress = roundRobinScheduler.getScheduleAddress(); // 다른 조건이 없으면 기본 round-robin 스케쥴러 이용
        if (componentAddress == -2) System.out.println("Connection Error : anyone component pool doesn't exist");
        return componentAddress;
    }

    public void setAffinity(int requestId, int componentAddress){ affinityScheduler.recentAddress.put(requestId,componentAddress); }
}
