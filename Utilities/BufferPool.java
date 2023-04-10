package Utilities;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool {
    Queue<ByteBuffer> bufferPool = new ConcurrentLinkedQueue<>(); // size가 정해져있지 않아 LinkedQueue 이용, thread-safe

    public void addBufferPool(int num, int size){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * num); // 한번에 거대한 버퍼를 할당 (생성 시간 효율)
        int pos = 0;
        for (int i=0; i < num; i ++){
            int end = pos + size;
            byteBuffer.limit(end);
            bufferPool.add(byteBuffer.slice()); // 필요한 크기만큼 잘라서 pool을 만듬
            pos = end;
            byteBuffer.position(pos);
        }
    }

    public ByteBuffer getBuffer(){
        while (true) { // 버퍼풀에 버퍼가 부족한 경우 기다리게 할 필요성이 있음
            ByteBuffer b = bufferPool.poll(); // poll 은 ConcurrentLinkedQueue에 element 없으면 null 반환
            if (b != null) return b;
            else Thread.yield();
        } // 다른 방법으로 변경 필요
    }

    public void releaseBuffer(ByteBuffer b){ bufferPool.add(b); }
}
