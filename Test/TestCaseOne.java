package Test;

import Client.Clients;
import Server.SingletonServer;

import java.util.concurrent.ExecutionException;

public class TestCaseOne {
    public static void test(SingletonServer s, Clients c, int n, int m, long start) throws InterruptedException {
        s.start(10);
        Thread.sleep(1000); // callback , sync 처리 필요

        try {
            c.start(n, m);
            Thread.sleep(1000); // 지워도 작동하지만, connection 와중에 pool remove 되는 경우를 보기 위한 test
            s.remove(5);
            s.add(5);
            c.start(n, m);
            Thread.sleep(1000);
            s.add(5);
            s.remove(5);
            s.add(5);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Test 1 Execution Failed or Interrupted");
        }

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (c.count == 2*n*m) {
                            Thread.sleep(5000);
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
        t.start();
    }
}
