package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterConnectionPool {
    AtomicInteger componentId = new AtomicInteger();
    List<ComponentConnectionPool> components = new Vector<ComponentConnectionPool>();
    String dbname = "YourSql"; // 추후 enum 으로 여러 db 네임 다루기
    ComponentScheduler componentScheduler = new ComponentScheduler();

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
    protected void start(int n) throws InterruptedException {
        for (int i=0; i<n; i++) {
            int id = componentId.get();
            ComponentConnectionPool c = new ComponentConnectionPool(id);
            c.makeComponent(5, dbname);
            components.add(c);
            componentScheduler.addPool(id);
            componentId.set(id + 1);
        }
    }

    // round-robin (waiting queue 추후 필요)
    protected void getConnect(){
        int ind = componentScheduler.getScheduleIndex();
        if (ind == -1){
            //System.out.println("Connection Error : any component pool doesn't exist");
            return;
        }
        try {
            int mapIndex = componentScheduler.availableIndex.get(ind);
            ComponentConnectionPool c = components.get(mapIndex);
            c.semaphore.acquire();
            if (!c.getConnect()){
                componentScheduler.failIndex.add(ind);
                getConnect();
            }
            else{
                componentScheduler.setCount(mapIndex);
            }
            c.semaphore.release();
        } catch (IndexOutOfBoundsException | InterruptedException e){ // lock의 앞선 대기자 중 remove가 있는 경우, 참조를 못하게 됨.
            getConnect();
        }
    }
    protected void remove(int n){
        int last = componentScheduler.getSize()-1;
        for (int i=last; i>Math.max(last-n,0); i--) {  // 현재 cluster pool 보다 더 많이 제거 명령할 시 따로 처리
            int mapIndex = componentScheduler.availableIndex.get(i);
            ComponentConnectionPool c = components.get(mapIndex);
            try {
                c.acquireAll();
                synchronized (componentScheduler.getIndex(i)) {
                    componentScheduler.remove(i);
                }
                c.releaseAll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void setFailedMark() throws InterruptedException {
        components.get(componentScheduler.getIndex(0)).setFailedMark();
    }

    protected int failOver() {
        if (componentScheduler.failIndex.size() > 0){
            for (int i : componentScheduler.failIndex){
                int j = componentScheduler.getIndex(i);
                if (components.get(j).getConnect()){
                    componentScheduler.failIndex.remove(i);
                    return j;
                }
            }
        }
        return -1;
    }
}
