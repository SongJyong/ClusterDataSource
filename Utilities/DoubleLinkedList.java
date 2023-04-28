package Utilities;

import java.util.concurrent.atomic.AtomicInteger;

public class DoubleLinkedList {
    private Node head = null;
    private Node tail = null;
    public AtomicInteger length = new AtomicInteger();

    public Node getHead(){ return this.head; }
    public Node findNodeIndexOf(int data) {
        Node foundNode = this.head;
        while (foundNode != null) {
            if (data != -1 && data == foundNode.address) { return foundNode; }
            foundNode = foundNode.next;
        }
        //못 찾은 경우
        return null;
    }
    public synchronized void addFirst(int address){
        Node target = new Node(address);
        if (this.length.get() == 0){
            this.tail = target;
        }
        else{
            this.head.prev = target;
            target.next = this.head;
        }
        this.head = target;
        this.length.incrementAndGet();
    }
    public synchronized void addLast(int data) {
        Node target = new Node(data);
        if (this.length.get() == 0) {
            //최초 노드 삽입인 경우
            this.head = target;
        }
        else {
            //맨 뒤에 노드 삽입인 경우
            target.prev = this.tail;
            this.tail.next = target;
        }
        this.tail = target;
        this.length.incrementAndGet();
    }

    public synchronized void customRemove(Node node){
        if(node == null) return;
        this.length.decrementAndGet();
        Node prev = node.prev;
        if (prev == null) {
            this.head = node.next;
            if (this.head != null) this.head.prev = null;
        }
        else {
            prev.next = node.next;
            if (node.next != null) node.next.prev = prev;
        }
        //node = null;
    }

    public synchronized Node removeElement(int address){
        Node target = findNodeIndexOf(address);
        if (target != null){ customRemove(target); }
        return target;
    }
}
