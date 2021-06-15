package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import peersim.kademlia.KademliaNode;
import peersim.kademlia.Message;
import peersim.kademlia.Topic;

public class LookupOperation extends Operation {
    public final Topic topic;
    private HashMap<KademliaNode,BigInteger> discovered;

    private int malQueried;
    
    public LookupOperation(BigInteger srcNode, Long timestamp, Topic t) {
        super(srcNode, t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
        this.topic = t;
        discovered = new HashMap<KademliaNode,BigInteger>();
        malQueried=0;

    }
    
    public void addDiscovered(KademliaNode n,BigInteger sourceId) {
        discovered.put(n,sourceId);
    }
    
    public HashMap<KademliaNode,BigInteger> getDiscovered(){
        return discovered;
    }
    
    public String discoveredToString() {
        if(discovered.size() == 0) return "";
        
        String result = "\"";
        boolean first = true;
        for(KademliaNode n: discovered.keySet()) {
        	if(!n.is_evil) {
	            if(first) {
	                result += n.getId();
	                first = false;
	            }else {
	                result += " " + n.getId();
	            }
        	}
        }
        result += "\"";
        return result;
    }
    
    public String discoveredMaliciousToString() {
        if(discovered.size() == 0) return "";
        
        String result = "\"";
        boolean first = true;
        for(KademliaNode n: discovered.keySet()) {
        	if(n.is_evil) {
	            if(first) {
	                result += n.getId();
	                first = false;
	            }else {
	                result += " " + n.getId();
	            }
        	}
        }
        result += "\"";
        return result;
    }
    
    public void increaseMaliciousQueries() {
    	malQueried++;
    }
    
    
    /*public List<KademliaNode> getDiscoveredArray()
    {
        return new ArrayList<KademliaNode>(discovered.keySet());
    }
    */
    public int discoveredCount() {
        return discovered.size();
    }

    public int maliciousDiscoveredCount() {
        int num_malicious = 0;
        for (KademliaNode n: this.discovered.keySet()) {
            if (n.is_evil)
                num_malicious += 1;
        }
        return num_malicious;
    }
    
    public int maliciousNodesQueries() {
    	return malQueried;
    }
}
