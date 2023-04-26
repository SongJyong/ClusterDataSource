package Utilities;

import Protocol.Request;


public class Pair {
    private Request requestData;
    private Integer clientId;
    public Pair(Request requestData, Integer clientId) { //순서대로 requestData 와 이 요청을 보낸 클라이언트의 session Id 값 저장하는 객체
        this.requestData = requestData;
        this.clientId = clientId;
    }
    public Request getRequestData(){
        return requestData;
    }
    public Integer getClientId(){
        return clientId;
    }
}
