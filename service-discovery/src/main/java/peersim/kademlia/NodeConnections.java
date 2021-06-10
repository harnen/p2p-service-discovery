package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import peersim.core.CommonState;
import peersim.core.Node;

public class NodeConnections {
	
    
    private int maxIncomingConnections;
    private int maxOutgoingConnections;
    
    private String topic;

    private List<KademliaNode> lookupResultBuffer;
        
    private List<KademliaNode> incomingConnections;
    private List<KademliaNode> outgoingConnections;
    
    private HashSet<String> addresses;
    
    private KademliaNode n;
    boolean requested;
	public NodeConnections(String topic,KademliaNode n) {
		
		this.incomingConnections = new ArrayList<KademliaNode>();
        this.outgoingConnections = new ArrayList<KademliaNode>();
        //if(!n.is_evil) {
	        this.maxIncomingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS*2/3;
	        this.maxOutgoingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS/3;
        /*} else {
        	this.maxIncomingConnections = KademliaCommonConfig.MAXCONNECTIONS*1000;
        	this.maxOutgoingConnections = KademliaCommonConfig.MAXCONNECTIONS*1000;
        }*/
        this.lookupResultBuffer = new ArrayList<KademliaNode>();
		this.topic = topic;
		this.requested = false;
		this.n = n;
		this.addresses = new HashSet<String>();
	}
	
	
	public boolean isRequested() {
		return requested;
	}
	
	public void setRequested(boolean req) {
		this.requested = req;
	}
	
	public void addLookupResult(List<KademliaNode> lookupResultBuffer) {
		this.lookupResultBuffer.addAll(lookupResultBuffer);
	}
	public void setLookupBuffer(List<KademliaNode> lookupResultBuffer) {
		this.lookupResultBuffer = lookupResultBuffer;
	}
	
	public List<KademliaNode> getLookupBuffer(){
		return this.lookupResultBuffer;
	}
	
	public List<KademliaNode> getIncomingConnections() {
		return this.incomingConnections;
	}
	
	public List<KademliaNode> getOutgoingConnections() {
		return this.outgoingConnections;
	}
	
	public boolean removeIncomingNode(KademliaNode node) {
		return incomingConnections.remove(node);
	}
	
	public boolean removeOutgoingNode(KademliaNode node) {
		if(outgoingConnections.contains(node)) {
	   		//System.out.println(CommonState.getTime()+" outgoing connections removed "+(outgoingConnections.size()-1))
			addresses.remove(node.getAddr());
			return outgoingConnections.remove(node);
		} else {
			return false;
		}
	}
	
	public void tryNewConnections() {
		
		int count=0;
    	while(outgoingConnections.size()<maxOutgoingConnections&&lookupResultBuffer.size()>0) {
       		int n = CommonState.r.nextInt(lookupResultBuffer.size());
       		boolean success = startConnection(lookupResultBuffer.get(n));

			if(success) {
				addOutgoingConnection(lookupResultBuffer.get(n));
				addresses.add(lookupResultBuffer.get(n).getAddr());
				count++;
			}
			lookupResultBuffer.remove(n);
    	}
   		//System.out.println(CommonState.getTime()+" new connections added "+count+" "+outgoingConnections.size()+" "+lookupResultBuffer.size());
        
	}

    
    private boolean startConnection(KademliaNode node) {
    	Node nd = Util.nodeIdtoNode(node.getId());
    	if(!nd.isUp()||addresses.contains(node.getAddr())) {
    		//System.out.println(CommonState.getTime()+" node is down");
    		return false;
    	} else {
    		return nd.getKademliaProtocol().getNode().addIncomingConnection(n,topic);
    	}
    		
    }
    
    
    public boolean isEclipsed() {
      
        if (getOutgoingConnections().size() == 0)
            return false;

        for (KademliaNode outConn : getOutgoingConnections())
            if (!outConn.is_evil)
                return false;

        return true;
    }
    
    public boolean addIncomingConnection(KademliaNode node) {
    	/*System.out.println(CommonState.getTime()+" "+incomingConnections.size()+" "+maxIncomingConnections);
    	for(KademliaNode n: incomingConnections)
    		System.out.println(CommonState.getTime()+" "+n.getId());*/
    	//if(incomingConnections.size()==maxIncomingConnections)System.out.println(CommonState.getTime()+" node full");
    	
    	if(incomingConnections.size()>=maxIncomingConnections) {
    		//System.out.println(CommonState.getTime()+" incoming connections full");
    		return false;
    	} else if (incomingConnections.contains(node)) {
    		//System.out.println(CommonState.getTime()+" incoming connection already existing");
    		return false;
    	}
    	if(incomingConnections.size()<maxIncomingConnections&&!incomingConnections.contains(node)) {
    		incomingConnections.add(node);
    		return true;
    	} else
    		return false;
    }		
    	
    public boolean addOutgoingConnection(KademliaNode node) {
    	if(outgoingConnections.size()<maxOutgoingConnections&&!outgoingConnections.contains(node)) {
    		outgoingConnections.add(node);
    		return true;
    	} else
    		return false;
    }
    
	
}
