import Client.Clients;
import Server.SingletonServer;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("Hello world!");

        test(2,100);


    }
    public static void test(int n, int m) throws InterruptedException {
        SingletonServer s = SingletonServer.getInstance();
        s.startServer();
        Thread.sleep(1000); // callback , sync 처리 필요
        s.start(10);
        Thread.sleep(1000);
        Clients c = new Clients();
        c.start(n,m);
        Thread.sleep(2000);
        s.remove(5);
        s.add(5);
        c.start(n,m);
        Thread.sleep(n*100*m); // callback , sync 처리 필요
        System.out.printf("client request  : %d \n",c.getData());
        System.out.printf("server response : %d \n",s.getData());
    }

}