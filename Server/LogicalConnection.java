package Server;
public class LogicalConnection {
    private PhysicalConnectionDummy physicalConnection;
    private ComponentConnectionPool component;
    public void read(){}
    public void write(){}
    public void commit(){}
    public void close(){
        component.connections.offer(physicalConnection); // 컴포넌트 풀에 커넥션 되돌려줌.
        component.semaphore.release(); // 입장권 반납
    }
    public LogicalConnection(PhysicalConnectionDummy physicalConnection, ComponentConnectionPool component){
        this.physicalConnection = physicalConnection;
        this.component = component;
    }
}
