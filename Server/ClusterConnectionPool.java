package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConnectionPool {
    AtomicInteger index = new AtomicInteger();
    AtomicInteger componentId = new AtomicInteger();
    List<ComponentConnectionPool> components = new Vector<ComponentConnectionPool>();
    String dbname = "YourSql"; // 추후 enum 으로 여러 db 네임 다루기
    List<Integer> remainId = new Vector<Integer>(); // 추후 Pair class 선언?
    List<Integer> remainCount = new Vector<Integer>(); // 추후 Pair class 선언?
    private ClusterConnectionPool(){};
    private static ClusterConnectionPool cluster;
    public static ClusterConnectionPool getInstance(){
        if(cluster == null){
            synchronized (ClusterConnectionPool.class){
                if(cluster == null) cluster = new ClusterConnectionPool();
            }
        }
        return cluster;
    } // double-checked locking , singleton 보장 (multi-thread 환경)
    protected int getData(){
        int total = 0; // 이것도 쓰레드 위험? ..
        for (ComponentConnectionPool c : components){
            System.out.printf("component pool id : %d, count : %d\n",c.componentId,c.count.get());
            total += c.count.get();
        }
        for (int i = 0; i < remainId.size(); i++) {
            System.out.printf("component pool id : %d, count : %d\n", remainId.get(i), remainCount.get(i));
            total += remainCount.get(i);
        }
        return total;
    }
    protected void start(int n) throws InterruptedException {
        for (int i=0; i<n; i++) {
            int id = componentId.get();
            ComponentConnectionPool c = new ComponentConnectionPool(id);
            c.makeComponent(5, dbname);
            components.add(c);
            componentId.set(id + 1);
            //Thread.sleep(1); // 동기 처리, 콜백 필요
        }
    }

    // round-robin (waiting queue 추후 필요)
    protected void getConnect(){
        int len = components.size();
        int ind = index.get()%len;
        index.set(ind+1);
        try {
            synchronized (components.get(ind)) {
                components.get(ind).getConnect();
            }
        } catch (IndexOutOfBoundsException e){ // lock의 앞선 대기자 중 remove가 있는 경우, 참조를 못하게 됨.
            getConnect();
        }
    }
    protected void remove(int n){
        int len = components.size()-1;
        for (int i=len; i>Math.max(len-n,0); i--) {  // 현재 cluster pool 보다 더 많이 제거 명령할 시 따로 처리
            synchronized (components.get(i)) {
                ComponentConnectionPool c = components.remove(i);
                remainId.add(c.componentId);
                remainCount.add(c.count.get());
            }
        }
    }
}
