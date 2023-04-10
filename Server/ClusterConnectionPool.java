package Server;

import Server.ClusterScheduler.AffinityScheduler;
import Server.ClusterScheduler.FailbackScheduler;
import Server.ClusterScheduler.RoundRobinScheduler;

import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConnectionPool {
    static public List<Integer> availableList = new Vector<>();
    //얘가 가지고 있는 값(value)은 현재 이용 가능한 component pool Id(index) 값 - 앞으로 componentIndex 라고 부름
    //스케쥴러 매개변수에 일일이 넣지 않기 위해 static 선언
    static public List<Integer> failedList = new Vector<>();
    //얘가 가지고 있는 값은 connect 실패로 인해 현재 접근하지 못하게 막아둔 componentIndex 값
    static public Queue<Integer> failbackQueue = new ConcurrentLinkedQueue<>();
    //얘가 가지고 있는 값은 failOver 과정중 failedList에서 복구되어 다시 연결 되는 componentIndex 값을 추가해
    //다음 스케쥴링 과정시 최우선 적으로 제공해줌.
    AtomicInteger componentId = new AtomicInteger(); // 컴포넌트 생성시 고유 id 부여
    List<ComponentConnectionPool> componentList = new Vector<ComponentConnectionPool>(); // 실제 컴포넌트 보유 리스트
    List<AtomicInteger> responseCount = new Vector<AtomicInteger>(); // 컴포넌트별 응답 카운트
    String dbUrl = "jdbc:h2:~/test";
    // 스케쥴러들 인스턴스 하나만 가지게 여기서 초기화, 이후 필요시 매개변수로 넘겨줌 (얘네도 static?)
    RoundRobinScheduler rrScheduler = new RoundRobinScheduler(); // avaialbleList 이용한 기본 스케쥴러 (내부 index 가지고 있음)
    AffinityScheduler affinityScheduler = new AffinityScheduler(); // 요청시 affinity option 인 경우 같은 componentId 스케쥴링 (내부 해시)
    FailbackScheduler failbackScheduler = new FailbackScheduler(); // failbackQueue 에 복구된 componentId가 있다면 이를 우선 스케쥴링 해줌.

    private ClusterConnectionPool(){};
    private static ClusterConnectionPool cluster;
    public static ClusterConnectionPool getInstance(){
        if(cluster == null){
            synchronized (ClusterConnectionPool.class){
                if(cluster == null) cluster = new ClusterConnectionPool();
            }
        }
        return cluster;
    } //이거 싱글톤일 필요 없지만 지금 클러스터 하나 가지고 여러 객체 구조 다루느라 편의성 위해
    // 일단 싱글톤으로 사용하고 있음.

    protected void start(int n) throws InterruptedException {
        for (int i=0; i<n; i++) {
            int id = componentId.getAndIncrement();
            ComponentConnectionPool c = new ComponentConnectionPool(id);
            c.makeComponent(5, dbUrl); // 임의로 컴포넌트 풀마다 physical connection 5개 초기화
            componentList.add(c);
            availableList.add(id); // 컴포넌트 생성될 때 availableList 에도 같이 추가
            responseCount.add(new AtomicInteger()); // 요청에 대한 응답 카운트 atomic integer 이용, 초기값 0 설정
        }
    }
    protected void remove(int n){ // 지금은 끝에서부터 차례대로 n 개 remove 해주는 기능 (추후 변경 필요)
        int last = availableList.size()-1;
        for (int i=last; i>Math.max(last-n,0); i--) {  // 현재 cluster pool 보다 더 많이 제거 명령할 시 따로 처리
            int componentIndex = availableList.get(i);
            ComponentConnectionPool c = componentList.get(componentIndex);
            try {
                c.acquireAll(); // 컴포넌트 c의 모든 semaphore 를 acquire(), remove중에 다른 작업이 겹치지 않도록
                availableList.remove(i); // last index 부터 차례대로 지움.
                c.setRemovedMark(); // getConnect 하면 null 값 되게 설정(추후 정책 변경 필요)
                c.releaseAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    // 요청과 정책에 맞게 componentIndex를 설정해 그 주소에 있는 component pool 의 connection 을 logical로 감싸진 객체를 가져와 반환
    public LogicalConnection getLogicalConnect(Integer clientId, boolean isAffinity){
        int componentInd = -1;
        if (isAffinity) componentInd = affinityScheduler.getScheduleIndex(clientId,rrScheduler); // affinity 최우선 확인
        else if (!failbackQueue.isEmpty()) componentInd = failbackScheduler.getScheduleIndex(); // failback 여부 확인
        else componentInd = rrScheduler.getScheduleIndex(); // 다른 조건이 없으면 기본 round-robin 스케쥴러 이용

        //모종의 이유로 인덱스 배정이 실패한 경우, 다시 시도 (but affinity는 포기)
        //추후 로직 변경 필요 (현재는 affinity 요청 접근하다가 목적 component pool이 죽으면 affinity 포기하고 다른 pool 찾으러 가버림.
        //그리고 affinity 정책이 현재는 클라이언트랑 component 짝짓기 느낌으로 되어 있는데 (요청 그룹마다 구분지어야 하면 수정 필요)
        if (componentInd == -1) return getLogicalConnect(clientId, false);

        ComponentConnectionPool c = componentList.get(componentInd); // 배정 받은 인덱스로 주소 참조해서 component 가져옴
        try {
            c.semaphore.acquire(); // 입장권 하나 요청
            LogicalConnection logicalConnection = c.getConnect(); // 입장권 받았으면 pool 에서 connection 가져오기 시도
            if (logicalConnection == null){
                if (!c.isRemoved() && availableList.contains(Integer.valueOf(componentInd))){
                    // connection 실패 시, pool 이 지워진 것도 아니고 별다른 이유가 없다면 고장으로 판단 failedList로 옮김.
                    availableList.remove(Integer.valueOf(componentInd));
                    failedList.add(componentInd);
                }
                c.semaphore.release(); // 입장권 반납
                return getLogicalConnect(clientId,false); // 다시 스케쥴링 부터 시작
            }
            else{
                updateCount(componentInd); // logical connection 무사히 받았으면 count 올린 후 메서드 호출한 곳으로 반환
                return logicalConnection; // 이 때 logical connection 이 close 되기 전까지 semaphore 계속 들고 있음.
            }
        } catch (InterruptedException | IndexOutOfBoundsException e) {
            return getLogicalConnect(clientId,false); // index 받아서 기다리고 있는데 앞에서 remove 해서 indexout 나는 경우 새로 시작
        }
    }
    protected void updateCount(int componentIndex){
        responseCount.get(componentIndex).getAndIncrement(); // response count update (thread-safe 동기화)
    }

    protected int getCount(){ // response count 출력 메서드 total 반환
        int total = 0;
        for (int c = 0; c < responseCount.size(); c++){
            System.out.printf("component pool id : %d, count : %d\n", c , responseCount.get(c).get());
            total += responseCount.get(c).get();
        }
        return total;
    }

    //fail test, 지금은 availableList의 가장 첫번째 주소에 위치한 component 랜덤 시간동안 getConnect() 실패하게 만듬)
    protected void setFailedMark() throws InterruptedException {
        componentList.get(availableList.get(0)).setFailedMark();
    }

    //주기적을 돌아가는 백그라운드 스레드에서 호출하는 메서드 (문제가 있는 component들 주기적으로 getConnect() 요청해보고 되면 failbackqueue로 복귀시킴)
    protected int failOver() {
        if (failedList.size() > 0){
            for (int componentInd : failedList){
                if (componentList.get(componentInd).getConnect() != null){
                    failbackQueue.offer(componentInd);
                    failedList.remove(Integer.valueOf(componentInd));
                    return componentInd;
                }
            }
        }
        return -1;
    }
}
