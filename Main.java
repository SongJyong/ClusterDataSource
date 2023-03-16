import Client.Clients;
import Server.SingletonServer;

import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Hello world!");

        test(50,10);


    }
    public static void test(int n, int m) throws InterruptedException {
        SingletonServer s = SingletonServer.getInstance();
        s.startServer();
        Thread.sleep(1000); // callback , sync 처리 필요
        s.start(10);
        Clients c = new Clients();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    c.start(n, m);
                    while(true){
                        if (c.count == n*m)break;
                        else Thread.yield();
                    }
                    s.remove(5);
                    s.add(5);
                    c.start(n, m);
                    s.add(5);
                    s.remove(1);
                    s.add(1);
                    while(true){
                        if (c.count == n*m)break;
                        else Thread.yield();
                    }
                } catch (InterruptedException | ExecutionException e) {
                }
            }
        };
        t.start();
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    t.join();
                    System.out.printf("client request  : %d \n", c.getData());
                    System.out.printf("server response : %d \n", s.getData());
                } catch(InterruptedException e){
                }
            }
        };
        t1.start();
    }

}