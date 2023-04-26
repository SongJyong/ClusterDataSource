package Server.ClusterScheduler;

import Server.ClusterConnectionPool;
import Utilities.Node;

public class RoundRobinScheduler implements Scheduler{
    private Node primaryLoadBalanceNode = null; // 골고루 분배할 수 있게 내부에서 순서를 가지고 있어야 함.
    private Node subLoadBalanceNode = null;
    @Override
    public synchronized int getScheduleAddress(){
        if (subLoadBalanceNode != null) subLoadBalanceNode = subLoadBalanceNode.next;
        if (subLoadBalanceNode == null) subLoadBalanceNode = ClusterConnectionPool.availableLinkedList.getHead();
        if (subLoadBalanceNode != null) return subLoadBalanceNode.getAddress(); // 내부 순서에 맞는 인덱스로 availableList 참조해 address 반환
        return -2;
    }
}
