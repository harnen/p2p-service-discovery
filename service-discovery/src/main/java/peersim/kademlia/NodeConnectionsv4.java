package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class NodeConnectionsv4  extends NodeConnections{
	
    
	private List<BigInteger> result;
    
	public NodeConnectionsv4(String topic,KademliaNode n) {
		super(topic,n);
		result = new ArrayList<BigInteger>();
	}
	

	public void addLookupResult(List<BigInteger> result) {
		
		this.result.addAll(result);
		this.setRequested(false);

		
	}
	
	public boolean isEmpty() {
		return result.size()==0;
	}
	
	
	public void sendLookup(Node n) {
		Message lookup = generateFindNodeMessage();
		EDSimulator.add(0, lookup, n, n.getKademliaProtocol().getProtocolID());

	}
	
	public boolean removeOutgoingNode(KademliaNode node) {
		if(outgoingConnections.contains(node)) {
	   		//System.out.println(CommonState.getTime()+" outgoing connections removed "+(outgoingConnections.size()-1))
	
			return outgoingConnections.remove(node);
		} else {
			return false;
		}
	}

	public void tryNewConnections() {
		
		//System.out.println("Trying connections v4 "+outgoingConnections.size());

    	while(outgoingConnections.size()<maxOutgoingConnections&&result.size()>0) {
    		//List<KademliaNode> valuesList = new ArrayList<KademliaNode>(lookupResultBuffer.keySet());

			int n = CommonState.r.nextInt(result.size());
	   		BigInteger randomNode = result.get(n);
			boolean success = startConnection(randomNode);
	
			if(success) {
				addOutgoingConnection(Util.nodeIdtoNode(randomNode).getKademliaProtocol().getNode());
	
			}
			result.remove(randomNode);
    	}

		//System.out.println("Outgoing connections "+outgoingConnections.size());

		
			
   		//}
	
		//System.out.println(CommonState.getTime()+" new connections added "+count+" "+outgoingConnections.size()+" "+lookupResultBuffer.size());
    
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

        BigInteger dst = n.getKademliaProtocol().getNode().getId();

		Message m = Message.makeInitFindNode(dst);
		m.timestamp = CommonState.getTime();

		return m;
	}
	
	
    private boolean startConnection(BigInteger node) {
    	///System.out.println("Starting connection");
    	Node nd = Util.nodeIdtoNode(node);
    	if(!nd.isUp() || !nd.getKademliaProtocol().getNode().hasTopic(topic)) {
    		//System.out.println(CommonState.getTime()+" node does not have topic");
    		return false;
    	} else {
    		return nd.getKademliaProtocol().getNode().addIncomingConnection(n,topic);
    	}
    		
    }
    

	
}
