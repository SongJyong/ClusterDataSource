package Server;

public class PhysicalConnectionDummy {
    private String url, user, pwd;
    public PhysicalConnectionDummy getConnect(String dbUrl){
        // db 접속, 커넥션 타입 리턴
        return this;
    }
}
