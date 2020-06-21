package peersim.kademlia;

import java.math.BigInteger;

import junit.framework.Test;
import junit.framework.TestCase;

public class TopicTest extends TestCase {
    Topic t1, t2;

    protected void setUp() {
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
        BigInteger hostID = new BigInteger("100000000000000000000000000000000000000000000000");
        t1 = new Topic(hostID, "topic1");
        //System.out.println("t1:" + t1);
        t2 = new Topic(hostID, "topic2");
        //System.out.println("t2:" + t2);
     }

    public void testComparison() {	
        assertTrue(t1.compareTo(t2) == 1);
   }


    
}