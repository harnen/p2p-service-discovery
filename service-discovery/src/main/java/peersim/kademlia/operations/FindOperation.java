package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import peersim.kademlia.Message;
import peersim.kademlia.Util;

/**
 * This class represents a find operation and offer the methods needed to maintain and update the closest set.<br>
 * It also maintains the number of parallel requsts that can has a maximum of ALPHA.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class FindOperation extends Operation{


	
	private List<String> topics;
	/**
	 * defaul constructor
	 * 
	 * @param destNode
	 *            Id of the node to find
	 */
	public FindOperation(BigInteger srcNode, BigInteger destNode, long timestamp) {
		super(srcNode, destNode, Message.MSG_FIND, timestamp);
		topics = new ArrayList<String>();
	}
	
	public void addTopic(String topic) {
		topics.add(topic);
	}
	
	public void remTopic(String topic) {
		topics.remove(topic);
	}

	public List<String> getTopics() { 
		return topics;
	}
	/*public void setDiscovered(int disc) {
		this.discovered = disc;
	}*/
	
	public int getDiscovered() {
		
		int discovered = 0;
		for(BigInteger nodeId : this.getNeighboursList()) {
			boolean found=false;
			for(String topic : topics) {
				if (Util.nodeIdtoNode(nodeId).getKademliaProtocol().getNode().getTopicList().contains(topic)) {
				found=true;
				break;
				}
			}
			if(found)discovered++;
			
		}
		return discovered;
	}
	
	



}
