package Utilities;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool {
    Queue<ByteBuffer> bufferPool = new ConcurrentLinkedQueue<>();

    public void addBufferPool(int num, int size){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size * num);
        int pos = 0;
        for (int i=0; i < num; i ++){
            int end = pos + size;
            byteBuffer.limit(end);
            bufferPool.add(byteBuffer.slice());
            pos = end;
            byteBuffer.position(pos);
        }
    }

    public ByteBuffer getBuffer(){
        while (true) {
            ByteBuffer b = bufferPool.poll();
            if (b != null) return b;
            else Thread.yield();
        }
    }

    public void releaseBuffer(ByteBuffer b){ bufferPool.add(b); }
}
