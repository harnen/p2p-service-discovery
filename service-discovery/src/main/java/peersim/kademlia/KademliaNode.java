package peersim.kademlia;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import peersim.core.Node;

import java.math.BigInteger;

public class KademliaNode implements Comparable<KademliaNode>{
    private BigInteger id;
    private String addr;
    private int port;
    
    private int protocolId;
    
    private List<BigInteger> lookupResultBuffer;
        
    private List<BigInteger> incomingConnections;
    private List<BigInteger> outgoingConnections;

    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.addr = addr;
        this.port = port;
        incomingConnections=new ArrayList<BigInteger>();
        outgoingConnections=new ArrayList<BigInteger>();
        //lookupResultBuffer=new ArrayList<BigInteger>();
    }
    
    public BigInteger getId(){
        return this.id;
    }

    public String getAddr(){
        return this.addr.toString();
    }

    public int getPort(){
        return this.port;
    }

    public int compareTo(KademliaNode n){
        if(this.id.compareTo(n.id) != 0){
            return this.id.compareTo(n.id);
        }
        if(this.addr != n.addr){
            return this.addr.compareTo(n.addr);
        }

        if(this.port == n.port){
            return 0;
        }

        if(this.port < n.port){
            return -1;
        }

        if(this.port > n.port){
            return 1;
        }

        return 0;
    }
    
    public void setLookupResult(List<BigInteger> result) {
    	System.out.println("Kademlianode result "+result.size());
    	lookupResultBuffer = result;
    	while(outgoingConnections.size()<(int)KademliaCommonConfig.MAXCONNECTIONS/3&&lookupResultBuffer.size()>0) {
    		boolean success=false;
    		while(!success&&lookupResultBuffer.size()>0){
    	    	System.out.println("Kademlianode start connection "+lookupResultBuffer.size());
    			success = startConnection(lookupResultBuffer.get(0));
    			if(success)addOutgoingConnection(lookupResultBuffer.get(0));
    			lookupResultBuffer.remove(0);
    		}
    	}
    			
    }
    
    public boolean addIncomingConnection(BigInteger addr) {
    	if(incomingConnections.size()<(int)KademliaCommonConfig.MAXCONNECTIONS*2/3) {
    		incomingConnections.add(addr);
    		return true;
    	} else
    		return false;
    }
    	
    public void addOutgoingConnection(BigInteger addr) {
    	outgoingConnections.add(addr);
    }
    
    public void deleteIncomingConnection(BigInteger addr) {
    	incomingConnections.remove(addr);
    }
    
    public void deleteOutgoingConnection(BigInteger addr) {
    	outgoingConnections.remove(addr);
    }
    
    public List<BigInteger> getIncomingConnections(){
    	return incomingConnections;
    }
    
    public List<BigInteger> getOutgoingConnections(){
    	return outgoingConnections;
    }
    
    public void setProtocolId(int id)
    {
    	this.protocolId = id;
    }
    private boolean startConnection(BigInteger addr) {
    	Node n = Util.nodeIdtoNode(addr, protocolId);
    	return(n.isUp()&&((KademliaProtocol)n.getProtocol(protocolId)).getNode().addIncomingConnection(id));
    		
    }
}