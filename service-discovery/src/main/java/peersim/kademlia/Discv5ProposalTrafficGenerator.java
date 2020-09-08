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
public class Discv5ProposalTrafficGenerator implements Control {

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
	public Discv5ProposalTrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		exp = Configuration.getDouble(prefix + "." + PAR_ZIPF);
		topicNum = Configuration.getInt(prefix + "." + PAR_TOPICNUM,1);
		zipf = new ZipfDistribution(topicNum,exp);
		pendingRegistrations = topicNum;
		pendingLookups = topicNum;
		topicList = new HashMap();
		
		for(int i=1; i <= topicNum; i++) {
			int times=zipf.sample();
			times = 4;
			topicList.put(new String("t"+i),new Integer(times));
		}
		
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
		

		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}

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
		System.out.println("New lookup message "+topic);

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

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a random find node message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		/*if(!first){
			return false;
		}
		first = false;*/
		
		
		
		//System.out.println("Pending registration: " + pendingRegistrations + " lookups: " + pendingLookups);
		if(pendingRegistrations>0) {
			Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
			System.out.println("Topic " + pair.getKey() + " will be registered " + pair.getValue() + " times");
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
		/*Node start;
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));

		// send message
		//Message m = generateFindNodeMessage();
		Message m = generateRegisterMessage();
		//System.out.println(">>>[" + CommonState.getTime() + "] Scheduling new MSG_FIND for " + (BigInteger) m.body);
		System.out.println(">>>[" + CommonState.getTime() + "] Scheduling new MSG_REGISTER for " + ((Topic) m.body).getTopic());

		EDSimulator.add(0, m, start, pid);

		return false;
	}	 */

	
	/*public boolean execute() {	
		Node start;
		Message m = null;
		//System.out.println(zipf.sample());
		
		
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));
		
		if(first){
			m = generateRegisterMessage();
			System.out.println(">>>[" + CommonState.getTime() + "] Scheduling new Register for " + (Topic) m.body);
			first = false;
			second = true;
		}else if (second) {
			m = generateTopicLookupMessage();
			System.out.println(">>>[" + CommonState.getTime() + "] Scheduling new lookup for " + (Topic) m.body);
			first = false;
			second = false;
		}
		
		
		
		if(m != null)
			EDSimulator.add(0, m, start, pid);

		

		return false;
	}*/
	
	/*
	* Used for debugging - submit a specific query
	public boolean execute() {
		if(!first){
			return false;
		}
		first = false;
		System.out.println("[" + CommonState.getTime() + "] Scheduling new MSG_FIND");
		Node n;
		BigInteger id = new BigInteger("33748437612422773219378968224765130741693226654183676333640231977266549442093");
		BigInteger dst = new BigInteger("10569885842503213978518658636404994036362621778380110644031029233192174040958");
		for(int i = 0; i < Network.size(); i++){
			n = Network.get(i);
			KademliaProtocol kad = (KademliaProtocol) (n.getProtocol(pid));
			BigInteger nodeId = kad.node.getId();
			if(!nodeId.equals(new BigInteger("32723433973953629672970764487732486867255891608285193160907571170441265141575"))) continue;
			int result = kad.routingTable.containsNode(dst);
			if(result >= 0){
				System.out.println(nodeId +  " contains node " + dst + " in its " + result + " bucket");
				System.out.println(kad.routingTable.bucket(dst));
			}
		}

		for(int i = 0; i < Network.size(); i++){
			System.out.println("Checking node " + i);
			n = Network.get(i);
			BigInteger nodeId = ((KademliaProtocol) (n.getProtocol(pid))).node.getId();
			//System.out.println("ID: " + id + " nodeId: " + nodeId);
			if( nodeId.equals(id)){
				//System.out.println("Found node " + id);
				Message m = Message.makeFindNode(dst);
				m.timestamp = CommonState.getTime();
				EDSimulator.add(0, m, n, pid);
				return false;
			}
		}

		return false;
	}*/

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
