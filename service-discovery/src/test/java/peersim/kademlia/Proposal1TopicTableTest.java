package peersim.kademlia;

import java.math.BigInteger;
import java.util.Random;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


import peersim.kademlia.Proposal1TopicTable;
import peersim.kademlia.UniformRandomGenerator;
import peersim.core.CommonState;
import peersim.config.ParsedProperties;
import peersim.config.Configuration;
import java.lang.Math; 


public class Proposal1TopicTableTest{
    
    @BeforeAll
    protected static void setUpBeforeClass() {
        String[] array = new String[] {"config/simple.cfg"};
        Configuration.setConfig( new ParsedProperties(array) );
        CommonState.setEndTime(Long.parseLong("100"));
        CommonState.setTime(Long.parseLong("0"));
        
    }

    @Test
    public void capacityTopics() {
        int v = (int) Math.pow(2, KademliaCommonConfig.BITS) - 1;
        Proposal1TopicTable tt = new Proposal1TopicTable(new BigInteger(Integer.toString(v)));
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
        tt.setCapacity(3);

        for(int i = 0; i < 10; i++){
            
            Topic t = new Topic(new BigInteger("0"), "topic"+i);
            Registration r = new Registration(new KademliaNode(urg.generate(), "127.0.0.1", 0));
            //System.out.println("Adding topic:" +  t + "with registration:" + r);
            Boolean result = tt.register(r, t);
        }
        //System.out.println("tt:" + tt);
        assert(tt.getSize() == tt.getCapacity());


    }

    @Test
    public void capacityRegistrations() {	
        int v = (int) Math.pow(2, KademliaCommonConfig.BITS) - 1;
        Proposal1TopicTable tt = new Proposal1TopicTable(new BigInteger(Integer.toString(v)));
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1231);
        tt.setCapacity(5);

        Topic t = new Topic(new BigInteger("0"), "topic");
        for(int i = 0; i < 10; i++){
            Registration r = new Registration(new KademliaNode(urg.generate(), "127.0.0.1", 0));
            //System.out.println("Adding topic:" +  t + "with registration:" + r);
            Boolean result1 = tt.register(r, t);
            
        }
        //System.out.println("tt:" + tt);
        assert(tt.getSize() == tt.getCapacity());
    }

    @Test
    public void sameRegistrations() {	
        int v = (int) Math.pow(2, KademliaCommonConfig.BITS) - 1;
        Proposal1TopicTable tt = new Proposal1TopicTable(new BigInteger(Integer.toString(v)));
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1231);
        tt.setCapacity(3);

        Topic t = new Topic(new BigInteger("0"), "topic");
        Registration r = new Registration(new KademliaNode(urg.generate(), "127.0.0.1", 0));
        for(int i = 0; i < 3; i++){
            //System.out.println("Adding topic:" +  t + "with registration:" + r);
            Boolean result1 = tt.register(r, t);
            //System.out.println("tt:" + tt);
        }
        //System.out.println("tt:" + tt);
        assert(tt.getSize() == 1);
    }


}