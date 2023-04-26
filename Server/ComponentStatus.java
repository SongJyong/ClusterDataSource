package Server;

public class ComponentStatus {
    private boolean failedMark = false;
    private boolean removedMark = false;
    private int referenceCount = 0;
    public void updateFailMark(){ failedMark ^= true; }
    public void updateRemoveMark(){ removedMark ^= true; }
    public synchronized void increaseCount(){ referenceCount += 1; }
    public boolean isFailed(){ return this.failedMark; }
    public boolean isRemoved() { return this.removedMark; }
    public int getCount(){ return this.referenceCount; }
}
