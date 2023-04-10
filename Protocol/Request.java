package Protocol;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Request implements Serializable {
    public static AtomicInteger key;
    private int id;
    private String data;
    private boolean affinity;
    public int getId(){ return id; }
    public String getStringData(){ return data; }
    public boolean isAffinity(){ return affinity; }
    public Request(String data, Boolean affinity) {
        this.data = data;
        this.affinity = affinity;
        id = key.get();
        key.set(key.get()+1);
    }
}
