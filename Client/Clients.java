package Client;

import java.util.List;
import java.util.Vector;

public class Clients {
    List<ClientService> clients = new Vector<ClientService>();
    public void start(int n, int m) throws InterruptedException {
        for (int i=0; i<n; i++) {
            ClientService c = new ClientService();
            clients.add(c);
            Thread.sleep(100); // callback, sync 필요
        }
        for (int j=0; j<n; j++) {
            ClientService temp = clients.get(clients.size() - n + j);
            temp.getConnection(m);
        }
    }

    public int getData(){
        int total = 0;
        for (ClientService cli : clients){
            total += cli.getCount();
        }
        return total;
    }
}
