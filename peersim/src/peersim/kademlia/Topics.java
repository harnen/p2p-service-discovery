package peersim.kademlia;

import java.math.BigInteger;
import java.util.PriorityQueue;


public class Topics {

  

    public static void main(String args[])  //static method  
    {
        Registration r1 = new Registration("aaa", 0, 0);
        Registration r2 = new Registration("aaa", 0, 1);  
        System.out.println("r1.compareTo(r2): " + r1.compareTo(r2));  
        System.out.println("r2.compareTo(r1): " + r2.compareTo(r1));  
    }  

    
}