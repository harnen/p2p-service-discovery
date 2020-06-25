package peersim.kademlia;

import java.math.BigInteger;

import junit.framework.Test;
import junit.framework.TestCase;

import peersim.kademlia.Proposal1TopicTable;
import peersim.kademlia.UniformRandomGenerator;


public class Proposal1TopicTableTest extends TestCase{
    Proposal1TopicTable tt1;
    Proposal1TopicTable tt2;

    protected void setUp() {
        tt1 = new Proposal1TopicTable(new BigInteger("100000000000000000000000000000000000000000000000"));
        tt2 = new Proposal1TopicTable(new BigInteger("999999999999999999999999999999999999999999999999"));

        tt1.setCapacity(2);
        tt2.setCapacity(5);
    }

    public void testCapacity() {	
        for(int i = 0; i < 10; i++){
            UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
            Topic t = new Topic(new BigInteger("0"), "topic"+i);
            Registration r = new Registration(urg.generate());
            Boolean result1 = tt1.register(r, t);
            Boolean result2 = tt2.register(r, t);
        }
        assert(tt1.getSize() == tt1.getCapacity());
        assert(tt2.getSize() == tt2.getCapacity());
    }
    
}