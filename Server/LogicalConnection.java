package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class LogicalConnection {
    List<Integer> logicalIndex = new Vector<Integer>();
    List<AtomicInteger> logicalCount = new Vector<AtomicInteger>();
    public static AtomicInteger index = new AtomicInteger();
    public void addPool(int id){
        logicalIndex.add(id); // 실제 phsyical component pool 을 지우지 않는단 가정하에 인덱스가 맞음 (추후 변경 필요)
        logicalCount.add(new AtomicInteger()); // count index == componentId
    }
    public int getSize(){ return logicalIndex.size(); }

    public synchronized Integer getIndex(int ind){
        return logicalIndex.get(ind);
    }
    public void setCount(int mapIndex){
        logicalCount.get(mapIndex).set(logicalCount.get(mapIndex).get()+1);
    }
    public int getScheduleIndex(){
        int len = getSize();
        if (len == 0){
            System.out.println("Connection Error : any component pool doesn't exist");
            return -1;
        }
        int ind = index.get()%len;
        index.set(ind+1);
        return ind;
    }
    public void remove(int last){
        logicalIndex.remove(last);
    }
    protected int getData(){
        int total = 0;
        for (int c = 0; c < logicalCount.size(); c++){
            System.out.printf("component pool id : %d, count : %d\n", c ,logicalCount.get(c).get());
            total += logicalCount.get(c).get();
        }
        return total;
    }
}
