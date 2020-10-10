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
	private final static String EVIL_PAR_PROT = "evilProtocol";
	private final static String PAR_TOPICNUM = "topicnum";
	private final static String PAR_FREQ = "maxfreq";

	private static Integer topicCounter = 0;

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid,evil_pid,topicNum;
		
	
	private Map<String,Integer> topicList;

	private Iterator<Entry<String, Integer>> it;

	
	// ______________________________________________________________________________________________
	public Discv5ProposalTrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		evil_pid = Configuration.getPid(prefix + "." + EVIL_PAR_PROT, -1);
		topicNum = Configuration.getInt(prefix + "." + PAR_TOPICNUM,1);

		topicList = new HashMap<String,Integer>();
		
		for(int i=1; i <= topicNum; i++) {
			//topicList.put(new String("t"+i),new Integer(times));
			topicList.put(new String("t"+i),new Integer(i));
		}
		//topicList.put(new String("t"+1),new Integer(10));
		
		it = topicList.entrySet().iterator();

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
            BigInteger nId;
            if (node.getProtocol(pid) != null)
    			nId = ((KademliaProtocol) (node.getProtocol(pid))).node.getId();
            else
    			nId = ((KademliaProtocol) (node.getProtocol(evil_pid))).node.getId();
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
		/*if(!first){
			return false;
		}
		first = false;*/
		

		if(it.hasNext()) {
			Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>) it.next();
			int regNum = pair.getValue() * 1;
			int queryNum = pair.getValue() * 1;
			System.out.println("Topic " + pair.getKey() + " will be registered " + regNum + " times and queried " + queryNum + " times.");
			
			Topic t = new Topic(pair.getKey());
			System.out.println("Topic hash: " + t.getTopicID());
			System.out.println("Closest node is " + getClosestNode(t.getTopicID()));
			for(int i=0; i < regNum; i++) {
				Message m = generateRegisterMessage(pair.getKey());
				Node start = getRandomNode();
                BigInteger nId;
                int prot;
                if (start.getProtocol(pid) != null) {
	    		    nId = ((KademliaProtocol) (start.getProtocol(pid))).node.getId();
                    prot = pid;
                }
                else {
	    		    nId = ((KademliaProtocol) (start.getProtocol(evil_pid))).node.getId();
                    prot = evil_pid;
                }

				if(m != null)
					EDSimulator.add(0, m, start, prot);
			}
			
			for(int i=0;i < queryNum; i++) {
				Message m = generateTopicLookupMessage(new String(pair.getKey()));
				Node start = getRandomNode();
                int prot;
                if (start.getProtocol(pid) != null) 
                    prot = pid;
                else
                    prot = evil_pid;

				int time = 200000 + i * 10;
				if(m != null)
					EDSimulator.add(time, m, start, prot);
			}
		}
		
		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
