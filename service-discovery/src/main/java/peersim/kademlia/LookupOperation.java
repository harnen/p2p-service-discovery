package peersim.kademlia;

import java.math.BigInteger;
import java.util.HashSet;

public class LookupOperation extends Operation {
	final Topic topic;
	private HashSet<BigInteger> discovered;
	
	public LookupOperation(Long timestamp, Topic t) {
		super(t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
		this.topic = t;
		discovered = new HashSet<BigInteger>();
	}
	
	public void addDiscovered(BigInteger id) {
		discovered.add(id);
	}
	
	public int discoveredCount() {
		return discovered.size();
	}

}
