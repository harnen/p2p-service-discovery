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
public class Discv5ZipfTrafficGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_ZIPF = "zipf";
	private final static String PAR_TOPICNUM = "topicnum";
    private final static String PAR_ATTACKTOPIC = "attackTopic";
	//private final static String PAR_FREQ = "maxfreq";

	protected boolean first = true;
	private boolean second = false;

    protected final int attackTopicIndex; //index of the topic attacked by all the malicious nodes
	private static Integer topicCounter = 0;

	protected ZipfDistribution zipf;
	/**
	 * MSPastry Protocol ID to act
	 */
	private final int topicNum;
	
	protected final double exp;
	
	private int pendingRegistrations,pendingLookups,topicCount;
	
	//private Map<String,Integer> topicList;

	//private Iterator<Entry<String, Integer>> it;
	
	private int counter = 0;
	private int lastRegistered = 0;
	
	// ______________________________________________________________________________________________
	public Discv5ZipfTrafficGenerator(String prefix) {
		exp = Configuration.getDouble(prefix + "." + PAR_ZIPF);
		topicNum = Configuration.getInt(prefix + "." + PAR_TOPICNUM,1);
		zipf = new ZipfDistribution(topicNum,exp);
        attackTopicIndex = Configuration.getInt(prefix + "." + PAR_ATTACKTOPIC, -1);
		pendingRegistrations = topicNum;
		pendingLookups = topicNum;
		/*topicList = new HashMap<String,Integer>();
		
		for(int i=1; i <= topicNum; i++) {
			int times=zipf.sample();
			//topicList.put(new String("t"+i),new Integer(times));
			topicList.put(new String("t"+i),new Integer(i));
		}
		//topicList.put(new String("t"+1),new Integer(20));
		
		it = topicList.entrySet().iterator();*/

	}

	// ______________________________________________________________________________________________
	/**
	 * generates a random find node message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	protected Message generateFindNodeMessage() {
		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}
        BigInteger dst = n.getKademliaProtocol().getNode().getId();

		Message m = Message.makeInitFindNode(dst);
		m.timestamp = CommonState.getTime();

		return m;
	}


	
	// ______________________________________________________________________________________________
	/**
	 * generates a register message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	protected Message generateRegisterMessage(String topic) {
		Topic t = new Topic(topic);
		Message m = Message.makeRegister(t);
		m.timestamp = CommonState.getTime();

		return m;
	}
	
	
	public void emptyBufferCallback(Node n, Topic t) {
        System.out.println("Emptybuffer:" + n.getKademliaProtocol().getNode().getId());
		//EDSimulator.add(0,generateTopicLookupMessage(t.getTopic()),n, n.getKademliaProtocol().getProtocolID());

	}
	
	
	// ______________________________________________________________________________________________
	/**
	 * generates a topic lookup message, by selecting randomly the destination and one of previousely registered topic.
	 * 
	 * @return Message
	 */
	protected Message generateTopicLookupMessage(String topic) {
		//System.out.println("New lookup message "+topic);

		Topic t = new Topic(topic);
		Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
		m.timestamp = CommonState.getTime();

		return m;
	}
	

	public BigInteger getClosestNode(BigInteger id) {
		BigInteger closestId = null;
		for(int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
            BigInteger nId = node.getKademliaProtocol().getNode().getId();
			if(closestId == null || (Util.distance(id, closestId).compareTo(Util.distance(id, nId)) == 1)) {
				closestId = nId;
			}
		}
		return closestId;
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 * 
	 * @return boolean
	 */
	public boolean execute() {

		HashMap<String,Integer> n = new HashMap<String,Integer>();
		if(first) {
			for(int i = 0;i<Network.size();i++) 
			{
				Node start = Network.get(i);
				KademliaProtocol prot = (KademliaProtocol)start.getKademliaProtocol();
                Topic t = null;
                String topic="";

                if (prot.getNode().is_evil) {
                    t = prot.getTargetTopic();
                }
                if (t == null) {
				    topic = new String("t"+zipf.sample());
    				t = new Topic(topic);
                }
                else 
                    topic = t.getTopic();
				Integer value = n.get(topic);
				if(value==null)
					n.put(topic, 1);
				else {
					int val = value.intValue()+1;
					n.put(topic,val);
				}
				System.out.println("Topic " + topic + " will be registered ");
				System.out.println("Topic hash: " + t.getTopicID());
				System.out.println("Closest node is " + getClosestNode(t.getTopicID()));
				Message registerMessage = generateRegisterMessage(t.getTopic());
				
				//kad.setClient(this);
				prot.getNode().setTopic(topic,start);
				//prot.getNode().setCallBack(this,start,t);
				
				Message lookupMessage = generateTopicLookupMessage(t.getTopic());

				if(registerMessage != null) {
					//int time = CommonState.r.nextInt(900000);
					int time = 0;
					System.out.println("Topic " + topic + " will be registered by "+prot.getNode().getId()+" at "+time);
					EDSimulator.add(time, registerMessage, start, start.getKademliaProtocol().getProtocolID());
					EDSimulator.add(time+20000, lookupMessage, start, start.getKademliaProtocol().getProtocolID());

				}
				
			}
			
			for (Map.Entry<String, Integer> i :n.entrySet()) 
				System.out.println("Topic "+i.getKey()+" "+i.getValue()+" times");
			first=false;
		}
		
		
		return false;
		
	}
		

	


	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
