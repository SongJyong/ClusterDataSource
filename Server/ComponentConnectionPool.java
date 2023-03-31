package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentConnectionPool {
    List<DatabaseDummy> connections = new Vector<DatabaseDummy>();
    AtomicInteger count = new AtomicInteger();
    boolean failedMark = false;
    int componentId;
    Semaphore semaphore;
    protected void acquireAll() throws InterruptedException {
        semaphore.acquire(connections.size());
    }
    protected void releaseAll(){
        semaphore.release(connections.size());
    }
    protected void makeComponent(int num, String dbname) throws InterruptedException {
        for (int i=0;i<num;i++){
            DatabaseDummy d = new DatabaseDummy();
            d.physcalConnect(dbname);
            connections.add(d);
        }
        semaphore = new Semaphore(num);
    }
    protected boolean getConnect(){
        if (failedMark) return false;
        count.set(count.get()+1);
        //get logical connection from connections pool.
        return true;
    }

    protected void setFailedMark() throws InterruptedException {
        this.failedMark = true;
        Thread.sleep(5000); // need to be random
        this.failedMark = false;
    }

    private ComponentConnectionPool(){}
    public ComponentConnectionPool(int id){
        this.componentId = id;
    }

}
