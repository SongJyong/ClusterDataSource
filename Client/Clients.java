package Client;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Clients {
    public int count = 0;
    List<ClientService> clients = new Vector<ClientService>();
    public synchronized void setCount(int c){ this.count += c; }
    public void start(int n, int m) throws InterruptedException, ExecutionException {
        //this.count = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < n; i++){
            Future future = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientService c = new ClientService();
                    clients.add(c);
                }
            });
            future.get(); // 제대로 waiting 안됨 (ex 클라 1개 일때, 초기화 안된 상태로 넘어감)
        }

        AtomicInteger index = new AtomicInteger(clients.size() - n);
        for (int j = 0; j < n; j++) {
            ClientService temp = clients.get(index.get());
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    setCount(temp.getConnection(m));
                }
            });
            index.set(index.get() + 1);
        }
    }

    public int getData(){
        int total = 0;
        for (ClientService cli : clients){
            System.out.printf("client %d \n",cli.getCount());
            total += cli.getCount();
        }
        return total;
    }
}
