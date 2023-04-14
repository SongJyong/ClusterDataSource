package Protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private int requestId;
    private String data;
    private boolean affinity;
    public int getId(){ return requestId; }
    public String getData(){ return data; }
    public boolean isAffinity(){ return affinity; }
    public Request(int id, String data, Boolean affinity) {
        this.data = data;
        this.affinity = affinity;
        this.requestId = id;
    }
}