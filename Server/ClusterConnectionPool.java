package Server;

import Server.ClusterScheduler.SchedulerFactory;
import Utilities.DoubleLinkedList;
import Utilities.Node;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConnectionPool {
    static public DoubleLinkedList primaryAvailableLinkedList = new DoubleLinkedList();
    static public DoubleLinkedList subAvailableLinkedList = new DoubleLinkedList();
    //얘가 가지고 있는 값(value)은 현재 이용 가능한 component pool Id(Address) 값 - 앞으로 componentAddress 라고 부름
    //스케쥴러 매개변수에 일일이 넣지 않기 위해 static 선언
    static public ConcurrentLinkedQueue<Integer> failedQueue = new ConcurrentLinkedQueue<>();
    //얘가 가지고 있는 값은 connect 실패로 인해 현재 접근하지 못하게 막아둔 componentAddress 값
    static public Queue<Integer> emptyMinHeap = new PriorityBlockingQueue<>();
    //관리자가 실제 component pool 을 지웠을 때, componentList 에 생기는 비어 있는 address를 최소 값부터 반환되게 모아둠. (새로 생성시 채우는 느낌)
    static public Map<Integer, ComponentStatus> statusMap = new HashMap<>();
    private List<ComponentStatus> archive = new ArrayList<>(); // 벡터일 이유 없음
    private AtomicInteger componentId = new AtomicInteger(); // 컴포넌트 생성시 고유 id 부여 (atomic일 필요 없음? add 동시 여러번 호출 될 때)
    private List<ComponentConnectionPool> componentList = new ArrayList<>(); // 실제 컴포넌트 보유 리스트
    String dbUrl = "jdbc:h2:~/test";
    SchedulerFactory schedulerFactory = new SchedulerFactory();

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

    //일단 synchronized , 추후 id, ind(size 할당) 동기화 필요
    protected synchronized void start(int n) throws InterruptedException {
        for (int i=0; i<n; i++) {
            int id = this.componentId.getAndIncrement();
            ComponentConnectionPool c = new ComponentConnectionPool(id);
            c.makeComponent(10, dbUrl); // 임의로 컴포넌트 풀마다 physical connection 10개 초기화
            if (emptyMinHeap.isEmpty()) {
                int ind = componentList.size();
                statusMap.put(ind,new ComponentStatus(id));
                componentList.add(c);
                subAvailableLinkedList.addFirst(ind);
            }
            else{
                int ind = emptyMinHeap.poll();
                statusMap.put(ind,new ComponentStatus(id));
                componentList.set(ind,c);
                subAvailableLinkedList.addFirst(ind);
            }
        }
    }
    protected void inactive(int address){ // component address에 해당하는 컴포넌트 inactive
        Node target = primaryAvailableLinkedList.removeElement(address);
        if (target == null) subAvailableLinkedList.removeElement(address);
        if(statusMap.get(address).isActive()) statusMap.get(address).updateActiveMark();
    }

    protected void active(int address){ // component address에 해당하는 컴포넌트 active
        if (!statusMap.get(address).isFailed()) {
            if (statusMap.get(address).isPrimary()) primaryAvailableLinkedList.addFirst(address);
            else subAvailableLinkedList.addFirst(address);
        }
        if(!statusMap.get(address).isActive()) statusMap.get(address).updateActiveMark();
    }

    protected void remove(int address){ // component address에 해당하는 컴포넌트 완전히 제거
        Node target = primaryAvailableLinkedList.removeElement(address);
        if (target == null) subAvailableLinkedList.removeElement(address);
        archive.add(statusMap.get(address));
        statusMap.put(address, null);
        emptyMinHeap.offer(address);
        /*
        ComponentConnectionPool c = componentList.get(address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (c.isFull()) {
                            archive.add(statusMap.get(address));
                            statusMap.put(address, null);
                            //componentList.set(address, null);
                            emptyMinHeap.offer(address);
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("removeThread Interrupted");
                    throw new RuntimeException(e);
                }
            }
        }).start();
         */
    }
    // 요청과 정책에 맞게 componentAddress를 설정해 그 주소에 있는 component pool 의 connection 을 logical로 감싸진 객체를 가져와 반환
    public LogicalConnection getLogicalConnect(int requestId){
        int componentAddress = schedulerFactory.getScheduleAddress(requestId);
        ComponentConnectionPool c = componentList.get(componentAddress); // 배정 받은 인덱스로 주소 참조해서 component 가져옴
        LogicalConnection logicalConnection = c.getConnect(); // pool 에서 connection 가져오기 시도 (take)
        if (logicalConnection == null){
            // connection 실패 시, 고장으로 판단 failedQueue로 옮김.
            synchronized (failedQueue){
                if(!failedQueue.contains(componentAddress) && !statusMap.get(componentAddress).isFailed()) {
                    if (primaryAvailableLinkedList.removeElement(componentAddress) == null)
                        subAvailableLinkedList.removeElement(componentAddress);
                    statusMap.get(componentAddress).updateFailMark();
                    failedQueue.offer(componentAddress);
                }
            }
            return getLogicalConnect(requestId); // 다시 스케쥴링 부터 시작 (failOver 기능)
        }
        else{
            schedulerFactory.setAffinity(requestId, componentAddress);
            updateCount(componentAddress); // logical connection 무사히 받았으면 count 올린 후 메서드 호출한 곳으로 반환
            return logicalConnection; // 이 때 logical connection 이 close 되기 전까지 semaphore 계속 들고 있음.
        }
    }
    protected void updateCount(int componentAddress){
        statusMap.get(componentAddress).increaseCount(); // response count update (thread-safe 동기화)
    }

    protected int getCount(){ // response count 출력 메서드 total 반환
        int total = 0;
        for (int c = 0; c < statusMap.size(); c++){
            if(statusMap.get(c) == null){
                System.out.printf("[%d] Null\n", c);
                continue;
            }
            if(statusMap.get(c).isPrimary()) System.out.print("p*");
            if(!statusMap.get(c).isActive()) System.out.print("i*");
            if(statusMap.get(c).isFailed()) System.out.print("f*");
            System.out.printf("[%d] Component Id : %d, Count : %d\n", c,statusMap.get(c).getComponentId(), statusMap.get(c).getCount());
            total+= statusMap.get(c).getCount();
        }
        System.out.println("\nbelow is completely removed components list");
        for (int c = 0; c < archive.size(); c++){
            System.out.printf("Component Id : %d, Count : %d\n", archive.get(c).getComponentId(), archive.get(c).getCount());
            total += archive.get(c).getCount();
        }
        return total;
    }
    protected void updatePrimary(int address){
        statusMap.get(address).updatePrimaryMark();
        if (statusMap.get(address).isPrimary()){
            if(subAvailableLinkedList.removeElement(address) != null) primaryAvailableLinkedList.addFirst(address);
        }
        else{
            if(primaryAvailableLinkedList.removeElement(address) != null) subAvailableLinkedList.addFirst(address);
        }
    }
    //fail test, 지금은 availableList의 가장 첫번째 주소에 위치한 component 랜덤 시간동안 getConnect() 실패하게 만듬)
    protected void setFailedMark(int address) throws InterruptedException { componentList.get(address).setFailedMark(); }

    //주기적을 돌아가는 백그라운드 스레드에서 호출하는 메서드 (문제가 있는 component들 주기적으로 getConnect() 요청해보고 되면 failbackqueue로 복귀시킴)
    //synchronized 필요 없음
    protected synchronized int failBack() { // 이름 바꾸기
        if (failedQueue.size() > 0){
            for (int i =0; i< failedQueue.size(); i++){ // iterator 로 변경?
                Integer componentAddress = failedQueue.poll();
                if (componentAddress == null){
                    return -1; // 이 체크를 위해 Integer 형태로 poll 받음.
                }
                ComponentConnectionPool c = componentList.get(componentAddress);
                if (c.getConnect() != null){
                    statusMap.get(componentAddress).updateFailMark();
                    if (statusMap.get(componentAddress).isPrimary()){
                        primaryAvailableLinkedList.addFirst(componentAddress);
                        return componentAddress;
                    }
                    subAvailableLinkedList.addFirst(componentAddress);
                    return componentAddress;
                }
                else failedQueue.offer(componentAddress);
            }
        }
        return -1;
    }
}
