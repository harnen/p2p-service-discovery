package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import peersim.kademlia.KademliaNode;
import peersim.kademlia.Message;
import peersim.kademlia.Topic;

public class LookupOperation extends Operation {
    public final Topic topic;
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
    
    public String discoveredToString() {
        if(discovered.size() == 0) return "";
        
        String result = "\"";
        boolean first = true;
        for(KademliaNode n: discovered) {
            if(first) {
                result += n.getId();
                first = false;
            }else {
                result += " " + n.getId();
            }
        }
        result += "\"";
        return result;
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
