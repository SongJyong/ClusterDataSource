package Client;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Clients {
    public int count = 0;
    List<ClientService> clients = new Vector<ClientService>();
    public void start(int n, int m) throws InterruptedException, ExecutionException {
        this.count = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < n; i++){
            Future future = executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ClientService c = new ClientService();
                    clients.add(c);
                    System.out.println(Thread.currentThread().getName());
                }
            });
            future.get();
        }

        AtomicInteger index = new AtomicInteger(clients.size() - n);
        class Count{
            int value = 0;
            synchronized void addValue(int value){
                this.value += value;
            }
        }
        class Task implements Runnable{
            Count count;
            Task(Count count){
                this.count = count;
            }
            @Override
            public void run(){
                ClientService temp = clients.get(index.get());
                count.addValue(temp.getConnection(m));
                index.set(index.get()+1);
            }
        }
        Count count = new Count();
        for (int j = 0; j < n; j++){
            Task t = new Task(count);
            Future<Count> f = executorService.submit(t, count);
            count = f.get();
            this.count = count.value;
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
