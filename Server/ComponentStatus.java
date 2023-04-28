package Server;

import java.util.concurrent.atomic.AtomicBoolean;

public class ComponentStatus {
    private int componentId;
    private AtomicBoolean primaryMark = new AtomicBoolean(false);
    private AtomicBoolean failedMark = new AtomicBoolean(false);
    private AtomicBoolean removedMark = new AtomicBoolean(false);
    private int referenceCount = 0;

    public void updatePrimaryMark(){ primaryMark.getAndSet(!primaryMark.get()); }
    public void updateFailMark(){ failedMark.getAndSet(!failedMark.get()); }
    public void updateRemoveMark(){ removedMark.getAndSet(!removedMark.get()); }

    public boolean isPrimary(){ return primaryMark.get(); }
    public boolean isFailed(){ return failedMark.get(); }
    public boolean isRemoved() { return removedMark.get(); }
    public boolean isPrimaryAvailable() { return (isPrimary() && !isRemoved() && !isFailed()); }
    public boolean isSubAvailable() { return (!isPrimary() && !isRemoved() && !isFailed()); }

    public synchronized void increaseCount(){ referenceCount += 1; }
    public int getCount(){ return referenceCount; }
    public int getComponentId() { return componentId; }
    public ComponentStatus(int id){
        this.componentId = id;
    }
}
