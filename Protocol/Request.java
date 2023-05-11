package Protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private final int needNumberOfConnection;
    public int getNeedNumberOfConnection(){ return needNumberOfConnection; }
    public Request(int number) {
        this.needNumberOfConnection = number;
    }
}