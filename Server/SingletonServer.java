package Server;

public class SingletonServer {
    private SingletonServer(){};
    private static SingletonServer singletonServer;
    private ServerService serverService; // 소켓 통신 다루는 서비스
    private ClusterConnectionPool cluster = ClusterConnectionPool.getInstance(); // 데이터리소스 다루는 클러스터
    public static SingletonServer getInstance(){
        if(singletonServer == null){
            synchronized (SingletonServer.class){
                if(singletonServer == null) singletonServer = new SingletonServer();
            }
        }
        return singletonServer;
    } // double-checked locking , singleton 보장,
    // 추후 lazy holder 등 다른 방식으로 변경 필요 (현재 서버는 초기화 단계에서 무조건 하나이지만 명시적인 이유로 사용)

    public void startServer(){
        serverService = new ServerService();
        serverService.startServer();
    }
    public void start(int n) throws InterruptedException {
        this.cluster.start(n); // component pool n개만큼 추가 (현재는 아래 add와 같음)
    }
    // 추후 기능이 달라진다면 cluster 에서 따로 구현 추가
    public void add(int n) throws InterruptedException {
        this.start(n);
    }
    public void remove(int n) throws InterruptedException {
        this.cluster.remove(n); // component pool 나중에 추가된 순서부터 차례대로 n개 remove
    }
    public int getData(){
        return this.cluster.getCount();
    } // 서버에서 응답된 요청 count 출력 및 토탈 return

    public void setFailedMark() throws InterruptedException {
        this.cluster.setFailedMark();
        // (지금은 availableindex.get(0) 에 위치한 component pool 을 마킹해 랜덤 시간동안 connect 반환 안되게 만들어줌.)
    }
}
