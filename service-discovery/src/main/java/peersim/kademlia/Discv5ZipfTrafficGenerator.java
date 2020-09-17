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
import java.util.HashMap;
import java.util.Iterator;
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
	private final static String PAR_PROT = "protocol";
	private final static String PAR_ZIPF = "zipf";
	private final static String PAR_TOPICNUM = "topicnum";
	private final static String PAR_FREQ = "maxfreq";

	private boolean first = true;
	private boolean second = false;

	private static Integer topicCounter = 0;

	private ZipfDistribution zipf;
	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid,topicNum;
	
	private final double exp;
	
	private int pendingRegistrations,pendingLookups,topicCount;
	
	private Map<String,Integer> topicList;

	private Iterator<Entry<String, Integer>> it;
	
	private int counter = 0;
	private int lastRegistered = 0;
	
	// ______________________________________________________________________________________________
	public Discv5ZipfTrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		exp = Configuration.getDouble(prefix + "." + PAR_ZIPF);
		topicNum = Configuration.getInt(prefix + "." + PAR_TOPICNUM,1);
		zipf = new ZipfDistribution(topicNum,exp);
		pendingRegistrations = topicNum;
		pendingLookups = topicNum;
		topicList = new HashMap<String,Integer>();
		
		for(int i=1; i <= topicNum; i++) {
			int times=zipf.sample();
			//topicList.put(new String("t"+i),new Integer(times));
			topicList.put(new String("t"+i),new Integer(i));
		}
		//topicList.put(new String("t"+1),new Integer(20));
		
		it = topicList.entrySet().iterator();

	}

	// ______________________________________________________________________________________________
	/**
	 * generates a random find node message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	private Message generateFindNodeMessage() {
		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}
		BigInteger dst = ((KademliaProtocol) (n.getProtocol(pid))).node.getId();

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
	private Message generateRegisterMessage() {
		Topic t = new Topic("t" + Integer.toString(this.topicCounter++));
		Message m = Message.makeRegister(t);
		m.timestamp = CommonState.getTime();

		return m;
	}
	
	// ______________________________________________________________________________________________
	/**
	 * generates a register message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	private Message generateRegisterMessage(String topic) {
		Topic t = new Topic(topic);
		Message m = Message.makeRegister(t);
		m.timestamp = CommonState.getTime();

		return m;
	}
	
	
	// ______________________________________________________________________________________________
	/**
	 * generates a topic lookup message, by selecting randomly the destination and one of previousely registered topic.
	 * 
	 * @return Message
	 */
	private Message generateTopicLookupMessage() {
		Topic t = new Topic("t" + Integer.toString(CommonState.r.nextInt(this.topicCounter)));
		Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
		m.timestamp = CommonState.getTime();

		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}
		return m;
	}
	
	// ______________________________________________________________________________________________
	/**
	 * generates a topic lookup message, by selecting randomly the destination and one of previousely registered topic.
	 * 
	 * @return Message
	 */
	private Message generateTopicLookupMessage(String topic) {
		//System.out.println("New lookup message "+topic);

		Topic t = new Topic(topic);
		Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
		m.timestamp = CommonState.getTime();

		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}
		return m;
	}
	
	public Node getRandomNode() {
		Node node = null;
		do {
			node = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((node == null) || (!node.isUp()));
		
		return node;
	}
	
	public BigInteger getClosestNode(BigInteger id) {
		BigInteger closestId = null;
		for(int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			BigInteger nId = ((KademliaProtocol) (node.getProtocol(pid))).node.getId();
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


		
		if(pendingRegistrations>0) {
			Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
			System.out.println("Topic " + pair.getKey() + " will be registered " + pair.getValue() + " times");
			Topic t = new Topic(pair.getKey());
			System.out.println("Topic hash: " + t.getTopicID());
			System.out.println("Closest node is " + getClosestNode(t.getTopicID()));
			for(int i=0;i<pair.getValue();i++) {
				Message m = generateRegisterMessage(pair.getKey());
				Node start = getRandomNode();
				if(m != null)
					EDSimulator.add(0, m, start, pid);
			}
			pendingRegistrations--;
			this.lastRegistered = counter;
		} else if((pendingLookups > 0) && (counter > (this.lastRegistered + 50)) ) {
			Message m = generateTopicLookupMessage(new String("t"+pendingLookups));
			Node start = getRandomNode();
			if(m != null)
				EDSimulator.add(0, m, start, pid);
			pendingLookups--;
		}
		
		this.counter++;
		
		return false;
		
	}
		

	


	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________