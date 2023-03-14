package Server;

public class DatabaseDummy {
    private int Key, id;
    private String name, pwd; // 추상클래스? 인터페이스? 사용해서 여러 DB 종류 다룰 수 있게 해야 할 예정
    public DatabaseDummy connectionDB(String dbname){
        // db 접속, 커넥션 타입 리턴
        // 추후 콜백, 동기화 처리 필요
        return this;
    }
}
