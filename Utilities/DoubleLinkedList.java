package Utilities;

public class DoubleLinkedList {
    private Node head = null;
    private Node tail = null;
    public int length = 0;

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

    public synchronized void addLast(int data) {
        Node target = new Node(data);
        if (this.length == 0) {
            //최초 노드 삽입인 경우
            this.head = target;
        }
        else {
            //맨 뒤에 노드 삽입인 경우
            target.prev = this.tail;
            this.tail.next = target;
        }
        this.tail = target;
        this.length += 1;
    }

    public synchronized void customRemove(Node node){
        if(node == null) return;
        Node prev = node.prev;
        prev.next = node.next;
        if(node.next != null) node.next.prev = prev;
        //node = null;
        this.length -= 1;
    }

    public Node removeElement(int address){
        Node target = findNodeIndexOf(address);
        if (target != null){ customRemove(target); }
        return target;
    }
}
