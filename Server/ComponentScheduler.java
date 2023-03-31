package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentScheduler {
    List<Integer> availableIndex = new Vector<Integer>();
    List<AtomicInteger> componentCount = new Vector<AtomicInteger>();
    public static AtomicInteger index = new AtomicInteger();
    List<Integer> failIndex = new Vector<>();
    public void addPool(int id){
        availableIndex.add(id); // 실제 phsyical component pool 을 지우지 않는단 가정하에 인덱스가 맞음 (추후 변경 필요)
        componentCount.add(new AtomicInteger()); // count index == componentId
    }
    public int getSize(){ return availableIndex.size(); }

    public synchronized Integer getIndex(int ind){
        return availableIndex.get(ind);
    }
    public void setCount(int mapIndex){
        componentCount.get(mapIndex).set(componentCount.get(mapIndex).get()+1);
    }
    public int getScheduleIndex(){
        int len = getSize();
        if (len == 0){
            System.out.println("Connection Error : any component pool doesn't exist");
            return -1;
        }
        int ind = index.get()%len;
        index.set(ind+1);
        if (failIndex.contains(ind)) return getScheduleIndex();
        return ind;
    }
    public void remove(int last){
        availableIndex.remove(last);
    }
    protected int getData(){
        int total = 0;
        for (int c = 0; c < componentCount.size(); c++){
            System.out.printf("component pool id : %d, count : %d\n", c , componentCount.get(c).get());
            total += componentCount.get(c).get();
        }
        return total;
    }
}
