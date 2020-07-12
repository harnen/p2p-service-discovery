package peersim.kademlia;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import peersim.config.Configuration;
import peersim.config.ParsedProperties;
import peersim.core.CommonState;

public class RadiusTest{
    BigDecimal targetRad =  new BigDecimal(new BigInteger("FFFFFFFFFFFFFFFF",16).divide(BigInteger.valueOf(100)));

    /*@BeforeAll
    protected static void setUpBeforeClass() {
        String[] array = new String[] {"config/simple.cfg"};
        Configuration.setConfig( new ParsedProperties(array) );
        CommonState.setEndTime(Long.parseLong("100"));
        CommonState.setTime(Long.parseLong("0"));
        
    }*/
    
    @Test
    public void prefix() {
	 UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	 BigInteger addr = new BigInteger("2be25ab67fc055f335ae34f14e3a6d001e274d163133fb71b62a6fbe47af2659",16);
   	 BigInteger prefix = Util.prefix(addr);
   	 assertEquals(prefix.compareTo(new BigInteger("3162189628241368563")),0);
    }

    @Test
    public void testBucketIdx() {
    	
    	 long now = CommonState.getTime();
         UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
         BigInteger nodeId = urg.generate();
         Topic t = new Topic(nodeId, "qwerty");
         TopicRadius rad = new TopicRadius(t);
         
    	for(int i=0;i<100;i++) {
    		rad.getBucketIdx(urg.generate());
    	}
    	BigInteger addr = new BigInteger("70986086576783945209219180375287403516254710493263781084957595945169174220220");
    	rad.getBucketIdx(addr);
    	
    	BigInteger addr2 = new BigInteger("41437296756308121279472734091420744713900280013306660066931948436079858994725");
    	rad.getBucketIdx(addr2);
    	
    	BigInteger addr3 = new BigInteger("105670402857126241309381629907842253737613637743993897586565594036938909726145");
    	rad.getBucketIdx(addr3);
    }

    
     @Test
     public void RadiusCalculation() {	
    	 long now = CommonState.getTime();
         UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
         BigInteger nodeId = urg.generate();
         Topic t = new Topic(nodeId, "qwerty");
         TopicRadius rad = new TopicRadius(t);
         int i=0;
         int bcnt=0,cnt=0;
         double sum=0.0;
         
         while(cnt<100) {
        	 i++;
        	 BigInteger addr = rad.nextTarget(false).getAddress();
        	 long wait = waitFn(addr,rad,i);
        	 rad.adjustWithTicket(now, addr,wait,now);
        	 System.out.println("radius "+rad.radius);
        	 System.out.println("maxradius "+TopicRadius.maxRadius);
        	 System.out.println("Topic:"+t+" wait:"+wait+" cnt:"+cnt+" bcnt:"+bcnt);	
     		  if(rad.radius.compareTo(TopicRadius.maxRadius)!=0) {
    			cnt++;
    			sum += rad.radius.doubleValue();
     		  } else {
    			bcnt++;
    			if(bcnt > 500){
    				assertTrue(bcnt<=500,() -> "Radius did not converge in 500 iterations");
    		    }
     		  }
         }
     	 double avgRel = sum / (double)cnt / targetRad.doubleValue();
     	 assertTrue(((avgRel <= 1.05)&&(avgRel >= 0.95)),() -> "Average/target ratio is too far from 1 "+ avgRel);
        	 
     }

     public long waitFn(BigInteger addr,TopicRadius rad,int i) {
    	 BigInteger prefix = Util.prefix(addr);
    	 
    	 BigDecimal dist = new BigDecimal(prefix.xor(rad.getTopicHashPrefix()));
    	 
    	 //System.out.println("Addr "+addr+" prefix "+prefix+" "+prefix.bitCount()+" topicprefix:"+rad.getTopicHashPrefix()+" dist:"+dist+" "+i);

    	 
    	 BigDecimal relDist = dist.divide(targetRad,MathContext.DECIMAL64);
    	 double relTime = (1.0 - (relDist.divide(BigDecimal.valueOf(2.0)).doubleValue())) * 2;

    	 if(relTime < 0) {
    		 relTime = 0;
    	 }
    	 //System.out.println("dist "+dist+" targetRad "+targetRad+" relDist "+relDist+" reltime "+relTime);

    	 //System.out.println("Wait time "+TopicRadius.targetWaitTime+" "+relTime+" "+TopicRadius.targetWaitTime*relTime);
    	 return (long)(TopicRadius.targetWaitTime * relTime);
     }

}