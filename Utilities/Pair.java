package Utilities;

import java.nio.ByteBuffer;

public class Pair {
    private ByteBuffer requestData;
    private Integer clientId;
    public Pair(ByteBuffer byteBuffer, Integer clientId) { //순서대로 requestData 와 이 요청을 보낸 클라이언트의 session Id 값 저장하는 객체
        this.requestData = byteBuffer;
        this.clientId = clientId;
    }
    public ByteBuffer getRequestData(){
        return requestData;
    }
    public Integer getClientId(){
        return clientId;
    }
}
