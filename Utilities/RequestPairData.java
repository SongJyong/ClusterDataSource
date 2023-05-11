package Utilities;

import Protocol.Request;

public class RequestPairData {
    private Request requestData;
    private int clientId;
    private int requestId;
    public RequestPairData(Request requestData, int clientId, int requestId) { //순서대로 requestData 와 이 요청을 보낸 클라이언트의 session Id 값 저장하는 객체
        this.requestData = requestData;
        this.clientId = clientId;
        this.requestId = requestId;
    }
    public Request getRequestData(){
        return requestData;
    }
    public int getClientId(){ return clientId; }
    public int getRequestId() { return requestId; }
}
