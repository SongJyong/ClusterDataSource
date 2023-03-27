import Client.Clients;
import Server.SingletonServer;

import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Hello world!");

        long start = System.nanoTime();
        test(start,10,100);
    }
    public static void test(long start, int n, int m) throws InterruptedException {
        SingletonServer s = SingletonServer.getInstance();
        s.startServer();
        s.start(10);
        Thread.sleep(1000); // callback , sync 처리 필요
        Clients c = new Clients();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    c.start(n, m);
                    Thread.sleep(1000);
                    s.remove(5);
                    s.add(5);
                    c.start(n, m);
                    s.add(5);
                    s.remove(1);
                    s.add(1);
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        };
        t.start();
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (c.count == 2*n*m) {
                            Thread.sleep(10000);
                            System.out.printf("client request  : %d \n", c.getData());
                            System.out.printf("server response : %d \n", s.getData());
                            System.out.printf("%d \n", s.serverService.businessLogic.workQueue.size());
                            System.out.printf("%d \n", s.serverService.businessLogic.requestData.size());
                            System.out.printf("%d \n", s.serverService.businessLogic.index.get());
                            long end = System.nanoTime();
                            System.out.printf("running time : %d (micro seconds)\n", (end-start)/1000);
                            break;
                        }
                        else Thread.yield();
                    }
                } catch(Exception e){
                }
            }
        };
        t1.start();
    }

}