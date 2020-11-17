package peersim.kademlia;

import java.math.BigInteger;


import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TopicTest{
    Topic t1, t2;

     @Test
    public void testComparison() {
        //BigInteger hostID = new BigInteger("1000000000000000000000000000000000000000000000000000000000000000000");
        //BigInteger hostID = new BigInteger("44315433558570760528211370774268539196007336381590584937709995345258309820811");
        BigInteger hostID = new BigInteger("44315433558570760528211370774268539196007336381590584937709995345258309820812");
        t1 = new Topic(hostID, "topic1");
        t2 = new Topic(hostID, "topic2");
        System.out.println("t1 compared to t2" + t1.compareTo(t2));
        assertEquals(t1.compareTo(t2), -1);
   }


    
}