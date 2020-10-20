package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class LookupOperation extends Operation {
	final Topic topic;
	private HashSet<KademliaNode> discovered;

	
	public LookupOperation(BigInteger srcNode, Long timestamp, Topic t) {
		super(srcNode, t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
		this.topic = t;
		discovered = new HashSet<KademliaNode>();

	}
	
	public void addDiscovered(KademliaNode n) {
		discovered.add(n);
	}
	
	public HashSet<KademliaNode> getDiscovered(){
		return discovered;
	}
	
	public List<KademliaNode> getDiscoveredArray()
	{
		return new ArrayList<KademliaNode>(discovered);
	}
	
	public int discoveredCount() {
		return discovered.size();
	}

    public int maliciousDiscoveredCount() {
        int num_malicious = 0;
        for (KademliaNode n: this.discovered) {
            if (n.is_evil)
                num_malicious += 1;
        }
        return num_malicious;
    }
}
