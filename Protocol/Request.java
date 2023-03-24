package Protocol;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Request implements Serializable {
    public static AtomicInteger key;
    private int id;
    private String data;
    public int getId(){ return id;}
    public String getStringData(){ return data;}
    public Request(String data) {
        this.data = data;
        id = key.get();
        key.set(key.get()+1);
    }

}
