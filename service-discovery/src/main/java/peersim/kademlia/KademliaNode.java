package peersim.kademlia;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import peersim.core.CommonState;
import peersim.core.Node;

import java.math.BigInteger;

public class KademliaNode implements Comparable<KademliaNode>{
    private BigInteger id;
    private String addr;
    private int port;
    
    private int protocolId;
    private int maxIncomingConnections;
    private int maxOutgoingConnections;

    private List<BigInteger> lookupResultBuffer;
        
    private List<BigInteger> incomingConnections;
    private List<BigInteger> outgoingConnections;
    
    private Node n;
    
    private EthClient client;

    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.addr = addr;
        this.port = port;
        incomingConnections=new ArrayList<BigInteger>();
        outgoingConnections=new ArrayList<BigInteger>();
        maxIncomingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS*2/3;
        maxOutgoingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS/3;
        lookupResultBuffer=new ArrayList<BigInteger>();
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
    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" setLookupResult "+result.size());
    	lookupResultBuffer = result;
    	tryNewConnections();
    			
    }
    		
    public List<BigInteger> getLookupResult() {
    	return lookupResultBuffer;
    	
    }
    
    public boolean addIncomingConnection(BigInteger addr) {
    	if(incomingConnections.size()<maxIncomingConnections) {
    		incomingConnections.add(addr);
    		return true;
    	} else
    		return false;
    }		
    	
    public boolean addOutgoingConnection(BigInteger addr) {
    	if(outgoingConnections.size()<maxOutgoingConnections) {
    		outgoingConnections.add(addr);
    		return true;
    	} else
    		return false;
    }
    
    public void deleteIncomingConnection(BigInteger addr) {
    	incomingConnections.remove(addr);

    }
    
    public void deleteOutgoingConnection(BigInteger addr) {
    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection:"+outgoingConnections.size());
    	outgoingConnections.remove(addr);
    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection2:"+outgoingConnections.size());
    	tryNewConnections();

    }
    
    public List<BigInteger> getIncomingConnections(){
    	return incomingConnections;
    }
    
    public List<BigInteger> getOutgoingConnections(){
    	return outgoingConnections;
    }
    
    public void setProtocolId(int id) {
    	this.protocolId = id;
    }
    
    public void setCallBack(EthClient client, Node n) {
    	this.client = client;
    	this.n = n;
    }
    
    private void tryNewConnections() {
       	while(outgoingConnections.size()<maxOutgoingConnections&&lookupResultBuffer.size()>0) {
    		boolean success=false;
    		while(!success&&lookupResultBuffer.size()>0){
    	    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" start connection "+lookupResultBuffer.size());
    			success = startConnection(lookupResultBuffer.get(0));
    			
    			if(success)addOutgoingConnection(lookupResultBuffer.get(0));
    			lookupResultBuffer.remove(0);
    	    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" start2 connection "+lookupResultBuffer.size());

    		}
    	}
       	if(lookupResultBuffer.size()==0&&client!=null&&n!=null){
       		System.out.println(CommonState.getTime()+" emptybuffer:"+lookupResultBuffer.size());
       		client.emptyBufferCallback(n);
       	}
    }
    
    private boolean startConnection(BigInteger addr) {
    	Node n = Util.nodeIdtoNode(addr, protocolId);
    	return(n.isUp()&&((KademliaProtocol)n.getProtocol(protocolId)).getNode().addIncomingConnection(id));
    		
    }
}