package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.KademliaNode;
import peersim.kademlia.Message;
import peersim.kademlia.Topic;

public class LookupOperation extends Operation {
    public final Topic topic;
    //private HashMap<KademliaNode,BigInteger> discovered;
    private ArrayList<KademliaNode> discovered;

    private int malQueried;
    
    private HashSet<BigInteger> tried;
    
    public LookupOperation(BigInteger srcNode, Long timestamp, Topic t) {
        super(srcNode, t.getTopicID(), Message.MSG_TOPIC_QUERY, timestamp);
        this.topic = t;
        discovered = new ArrayList<KademliaNode>();
        malQueried=0;
        tried = new HashSet<BigInteger>();

    }
    
    public Topic getTopic() {
    	return topic;
    }
    
    public void setTried(BigInteger id) {
    	tried.add(id);
    }
    
    public boolean tried(BigInteger id) {
    	return tried.contains(id);
    }
    
    public void addDiscovered(KademliaNode n) {
    	//make sure we don't add the same node twice
    	if(!discovered.contains(n)) {
    		discovered.add(n);
    	}  
    }
    
    public boolean isEclipsed() {
    	//we're not eclipsed if we didn't discover anyone yet
    	if(discovered.size() == 0) {
    		return false;
    	}
    	//considered only first TOPIC_PEER_LIMIT discovered peers
    	int hi = Math.min(discovered.size(), KademliaCommonConfig.TOPIC_PEER_LIMIT);
    	for(KademliaNode n: discovered.subList(0, hi)) {
    		if(!n.is_evil) {
    			return false;
    		}
    	}
    	return true;
    }
    
   
    public ArrayList<KademliaNode> getDiscovered(){
        return discovered;
    }
    
    public String discoveredToString() {
        if(discovered.size() == 0) return "";
        
        String result = "\"";
        boolean first = true;
        for(KademliaNode n: discovered) {
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
        for(KademliaNode n: discovered) {
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
    
    public int goodDiscoveredCount() {
        int numGood = 0;
        for (KademliaNode n: this.discovered) {
            if (!n.is_evil)
                numGood++;
        }
        return numGood;
    }

    public int maliciousDiscoveredCount() {
        int numMalicious = 0;
        for (KademliaNode n: this.discovered) {
            if (n.is_evil)
                numMalicious++;
        }
        return numMalicious;
    }
    
    public int maliciousNodesQueries() {
    	return malQueried;
    }
}
