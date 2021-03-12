package peersim.kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.distribution.ZipfDistribution; 
/**
 * This control generates random search traffic from nodes to random destination node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

// ______________________________________________________________________________________________
public class Discv5MultiTopicTrafficGenerator extends Discv5ZipfTrafficGenerator {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_MINTOPIC = "mintopicnum";
	private final static String PAR_MAXTOPIC = "maxtopicnum";
    private final static String PAR_LOOKUPS = "randomlookups";

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int mintopicNum;
	private final int maxtopicNum;
	private final int randomLookups;

	// ______________________________________________________________________________________________
	public Discv5MultiTopicTrafficGenerator(String prefix) {
        super(prefix);
		mintopicNum = Configuration.getInt(prefix + "." + PAR_MINTOPIC,1);
		maxtopicNum = Configuration.getInt(prefix + "." + PAR_MAXTOPIC);
        randomLookups = Configuration.getInt(prefix + "." + PAR_LOOKUPS, 0);

		zipf = new ZipfDistribution(maxtopicNum,exp);
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 
	 * @return boolean
	 */
	public boolean execute() {
		System.out.println("MultiTopic Traffic generator called");
        int num_topics;
		HashMap<String,Integer> n = new HashMap<String,Integer>();
        Topic [] topicList = new Topic[maxtopicNum]; //references to topics of a node
		if(first) {
			for(int i = 0; i<Network.size(); i++) 
			{
				Node start = Network.get(i);
				KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
                Topic t = null;
                String topic="";
                int numTopics;

                // if the node is malicious, it targets only one topic
                if (prot.getNode().is_evil) {
                    if (attackTopicIndex == -1) {
                        t = prot.getTargetTopic();
                        topicList[0] = t;
                    }
                    else {
                        topic = new String("t" + attackTopicIndex);
                        topicList[0] = new Topic(topic);
                        prot.setTargetTopic(topicList[0]);
                    }
                    numTopics = 1;
                } else {
                    numTopics = zipf.sample();
                    //System.out.println("Assigning node to " + numTopics + " topics");
                    
                    if (numTopics < mintopicNum)
                        numTopics = mintopicNum;
                    for (int topicIndex = 1; topicIndex < numTopics+1; topicIndex++) {
                        topic = new String("t" + topicIndex);
                        topicList[topicIndex-1] = new Topic(topic);
                    }
                    //System.out.println(i + "," + numTopics);
                }
				
                Integer value = n.get(topic);
				if(value==null)
					n.put(topic, 1);
				else {
					int val = value.intValue()+1;
					n.put(topic,val);
				}
				//System.out.println("Topic " + topic + " will be registered ");
				//System.out.println("Topic hash: " + t.getTopicID());
				//System.out.println("Closest node is " + getClosestNode(t.getTopicID()));
				
                for (int topicIndex = 1; topicIndex < numTopics+1; topicIndex++) {
				    //kad.setClient(this);
			    	//prot.getNode().setCallBack(this,start,topicList[topicIndex-1]);
				    Message registerMessage = generateRegisterMessage(topicList[topicIndex-1].getTopic());
				    Message lookupMessage = generateTopicLookupMessage(topicList[topicIndex-1].getTopic());
                    //System.out.println();
				    if(registerMessage != null) {
					    //int time = CommonState.r.nextInt(900000);
						int time = CommonState.r.nextInt(KademliaCommonConfig.AD_LIFE_TIME);
					    //int time = 0;
					    System.out.println("Topic " + topicList[topicIndex-1].getTopic() + " will be registered by "+prot.getNode().getId()+" at "+time);
					    EDSimulator.add(time, registerMessage, start, start.getKademliaProtocol().getProtocolID());
					    EDSimulator.add((KademliaCommonConfig.AD_LIFE_TIME)+time, lookupMessage, start, start.getKademliaProtocol().getProtocolID());

				    }
			    }
            }
			for (Map.Entry<String, Integer> i :n.entrySet()) 
				System.out.println("Topic "+i.getKey()+" "+i.getValue()+" times");
			
			first=false;
		}  else if(randomLookups==1) {
		
			for(int i = 0;i<Network.size();i++) 
			{
				for(int j = 0;j<3;j++) {
					Node start = Network.get(i);
					Message lookup = generateFindNodeMessage();
					EDSimulator.add(0, lookup, start, start.getKademliaProtocol().getProtocolID());
				}
	
			}
		
		}
		
		
		return false;
		
	}
		

	


	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
