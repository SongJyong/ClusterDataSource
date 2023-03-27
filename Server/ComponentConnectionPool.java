package Server;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentConnectionPool {
    List<DatabaseDummy> connections = new Vector<DatabaseDummy>();
    AtomicInteger count = new AtomicInteger();
    int componentId;
    protected void makeComponent(int num, String dbname) throws InterruptedException {
        for (int i=0;i<num;i++){
            DatabaseDummy d = new DatabaseDummy();
            d.connectionDB(dbname);
            connections.add(d);
        }
    }
    protected void getConnect(){
        count.set(count.get()+1);
    }
    private ComponentConnectionPool(){}
    public ComponentConnectionPool(int id){
        this.componentId = id;
    }

}
