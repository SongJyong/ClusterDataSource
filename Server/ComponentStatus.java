package Server;

import java.util.concurrent.atomic.AtomicBoolean;

public class ComponentStatus {
    private int componentId;
    private AtomicBoolean primaryMark = new AtomicBoolean(false);
    private AtomicBoolean failedMark = new AtomicBoolean(false);
    private AtomicBoolean activeMark = new AtomicBoolean(true);
    private int referenceCount = 0;

    public void updatePrimaryMark(){ primaryMark.getAndSet(!primaryMark.get()); }
    public void updateFailMark(){ failedMark.getAndSet(!failedMark.get()); }
    public void updateActiveMark(){ activeMark.getAndSet(!activeMark.get()); }

    public boolean isPrimary(){ return primaryMark.get(); }
    public boolean isFailed(){ return failedMark.get(); }
    public boolean isActive() { return activeMark.get(); }
    public boolean isPrimaryAvailable() { return (isPrimary() && isActive() && !isFailed()); }
    public boolean isSubAvailable() { return (!isPrimary() && isActive() && !isFailed()); }

    public synchronized void increaseCount(){ referenceCount += 1; }
    public int getCount(){ return referenceCount; }
    public int getComponentId() { return componentId; }
    public ComponentStatus(int id){
        this.componentId = id;
    }
}
