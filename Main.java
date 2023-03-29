import Client.Clients;
import Server.SingletonServer;

import Test.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SingletonServer singletonServer = SingletonServer.getInstance();
        singletonServer.startServer();
        Clients clients = new Clients();
        long start = System.nanoTime();
        TestCaseOne.test(singletonServer,clients,10,100,start);
    }
}