package Protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private int requestId;
    private String data;
    private int numberOfConnection;
    public int getRequestId(){ return requestId; }
    public String getData(){ return data; }
    public int getNumberOfConnection(){ return numberOfConnection; }
    public void setRequestId(int id){ requestId = id; }
    public Request(String data, int count) {
        this.data = data;
        this.numberOfConnection = count;
    }
}