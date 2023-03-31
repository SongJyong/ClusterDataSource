package Server;

public class SingletonServer {
    private SingletonServer(){};
    private static SingletonServer singletonServer;
    public ServerService serverService;
    public static SingletonServer getInstance(){
        if(singletonServer == null){
            synchronized (SingletonServer.class){
                if(singletonServer == null) singletonServer = new SingletonServer();
            }
        }
        return singletonServer;
    } // double-checked locking , singleton 보장 (multi-thread 환경) , 추후 lazy holder 등 다른 방식으로 변경 필요

    public void startServer(){
        serverService = ServerService.getInstance();
        serverService.startServer();
    }
    public void start(int n) throws InterruptedException {
        this.serverService.businessLogic.cluster.start(n);
    }
    // 추후 기능이 달라진다면 cluster 에서 따로 구현 추가
    public void add(int n) throws InterruptedException {
        this.start(n);
    }
    public void remove(int n) throws InterruptedException {
        this.serverService.businessLogic.cluster.remove(n);
    }
    public int getData(){
        return this.serverService.businessLogic.cluster.componentScheduler.getData();
    }

    public void setFailedMark() throws InterruptedException {
        this.serverService.businessLogic.cluster.setFailedMark();
    }
}
