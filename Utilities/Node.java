package Utilities;

public class Node {
    public Node prev = null;
    public Node next = null;
    int address= -1;
    public Node(int address){
        this.address = address;
    }
    public int getAddress(){ return this.address; }
}
