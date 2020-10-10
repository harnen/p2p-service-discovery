package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LookupOperation extends Operation {
	final Topic topic;
	private HashSet<BigInteger> discovered;
	private HashSet<Integer> used;
	
	public LookupOperation(BigInteger srcNode, Long timestamp, Topic t) {
		super(srcNode, t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
		this.topic = t;
		discovered = new HashSet<BigInteger>();
		used = new HashSet<Integer>();

	}
	
	public void addDiscovered(BigInteger id) {
		discovered.add(id);
	}
	
	public HashSet<BigInteger> getDiscovered(){
		return discovered;
	}
	
	public List<BigInteger> getDiscoveredArray()
	{
		return new ArrayList<BigInteger>(discovered);
	}
	
	public int discoveredCount() {
		return discovered.size();
	}
	
	public void addUsed(int distance) {
		used.add(distance);
	}
	
	public boolean isUsed(int distance) {
		return used.contains(distance);
	}
	
	public HashSet<Integer> getUsed(){
		return used;
	} 
}
