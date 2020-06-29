package peersim.kademlia;

import java.math.BigInteger;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


import peersim.kademlia.Proposal1TopicTable;
import peersim.kademlia.UniformRandomGenerator;
import peersim.core.CommonState;
import peersim.config.ParsedProperties;
import peersim.config.Configuration;


public class Proposal1TopicTableTest{
    
    @BeforeAll
    protected static void setUpBeforeClass() {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!Setup the config file");
        String[] array = new String[] {"config/simple.cfg"};
        Configuration.setConfig( new ParsedProperties(array) );
        CommonState.setEndTime(Long.parseLong("100"));
        CommonState.setTime(Long.parseLong("0"));
        
    }

    @Test
    public void testCapacityTopics() {
        Proposal1TopicTable tt1 = new Proposal1TopicTable(new BigInteger("100000000000000000000000000000000000000000000000"));
        Proposal1TopicTable tt2 = new Proposal1TopicTable(new BigInteger("999999999999999999999999999999999999999999999999"));

        tt1.setCapacity(2);
        tt2.setCapacity(5);

        for(int i = 0; i < 10; i++){
            UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
            Topic t = new Topic(new BigInteger("0"), "topic"+i);
            Registration r = new Registration(urg.generate());
            System.out.println("Adding topic:" +  t + "with registration:" + r);
            Boolean result1 = tt1.register(r, t);
            Boolean result2 = tt2.register(r, t);
        }
        System.out.println("tt1:" + tt1);
        System.out.println("tt2:" + tt2);
        assert(tt1.getSize() == tt1.getCapacity());
        assert(tt2.getSize() == tt2.getCapacity());


    }
    @Test
    public void testCapacityRegistration() {	
        Proposal1TopicTable tt1 = new Proposal1TopicTable(new BigInteger("100000000000000000000000000000000000000000000000"));
        Proposal1TopicTable tt2 = new Proposal1TopicTable(new BigInteger("999999999999999999999999999999999999999999999999"));

        tt1.setCapacity(2);
        tt2.setCapacity(5);

        Topic t = new Topic(new BigInteger("0"), "topic");
        for(int i = 0; i < 10; i++){
            UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
            Registration r = new Registration(urg.generate());
            Boolean result1 = tt1.register(r, t);
            Boolean result2 = tt2.register(r, t);
        }
        System.out.println("tt1:" + tt1);
        System.out.println("tt2:" + tt2);
        assert(tt1.getSize() == tt1.getCapacity());
        assert(tt2.getSize() == tt2.getCapacity());


    }

}