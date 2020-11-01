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
    
    private int maxIncomingConnections;
    private int maxOutgoingConnections;

    private List<KademliaNode> lookupResultBuffer;
        
    private List<KademliaNode> incomingConnections;
    private List<KademliaNode> outgoingConnections;
    
    private Node n;
    
    private Discv5ZipfTrafficGenerator client;
    
    boolean requested=false;

    public boolean is_evil=false;

    Topic t;
    
    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.addr = addr;
        this.port = port;
        incomingConnections = new ArrayList<KademliaNode>();
        outgoingConnections = new ArrayList<KademliaNode>();
        maxIncomingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS*2/3;
        maxOutgoingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS/3;
        lookupResultBuffer = new ArrayList<KademliaNode>();
    }

    public KademliaNode(BigInteger id){
        this.id = id;
        this.addr = "127.0.0.1";
        this.port = 666;
        incomingConnections = new ArrayList<KademliaNode>();
        outgoingConnections = new ArrayList<KademliaNode>();
        maxIncomingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS*2/3;
        maxOutgoingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS/3;
        lookupResultBuffer = new ArrayList<KademliaNode>();
    }

    public KademliaNode(KademliaNode n){
        this.id = n.id;
        this.addr = n.addr;
        this.port = n.port;
        this.is_evil = n.is_evil;
        incomingConnections = new ArrayList<KademliaNode>();
        outgoingConnections = new ArrayList<KademliaNode>();
        maxIncomingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS*2/3;
        maxOutgoingConnections = (int)KademliaCommonConfig.MAXCONNECTIONS/3;
        lookupResultBuffer = new ArrayList<KademliaNode>();
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
    
    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }
    
    @Override
    public boolean equals(Object o) { 
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 

        /* Check if o is an instance of KademliaNode or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof KademliaNode)) { 
            return false; 
        } 
        // typecast o to KademliaNode so that we can compare data members  
        KademliaNode r = (KademliaNode) o; 

        return this.id.equals(r.id);
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
    
    public void setLookupResult(List<KademliaNode> result) {
    	//System.out.println(CommonState.getTime()+" Kademlianode:"+id+" setLookupResult "+result.size());
    	//lookupResultBuffer = result;
    	lookupResultBuffer.addAll(result);
    	requested=false;
    	tryNewConnections();
    			
    }
    		
    public List<KademliaNode> getLookupResult() {
    	return lookupResultBuffer;
    	
    }
    
    public boolean addIncomingConnection(KademliaNode node) {
    	if(incomingConnections.size()<maxIncomingConnections) {
    		incomingConnections.add(node);
    		return true;
    	} else
    		return false;
    }		
    	
    public boolean addOutgoingConnection(KademliaNode node) {
    	if(outgoingConnections.size()<maxOutgoingConnections) {
    		outgoingConnections.add(node);
    		return true;
    	} else
    		return false;
    }
    
    public void deleteIncomingConnection(KademliaNode node) {
    	incomingConnections.remove(node);

    }
    
    public void deleteOutgoingConnection(KademliaNode node) {
    	/*System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection:"+outgoingConnections.size()+" "+addr);
    	for(BigInteger ad : outgoingConnections)
    	{
        	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection:"+ad);
    	}*/

    	outgoingConnections.remove(node);
    	//System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection2:"+outgoingConnections.size()+" "+lookupResultBuffer.size());
    	tryNewConnections();

    }
    
    public List<KademliaNode> getIncomingConnections(){
    	return incomingConnections;
    }
    
    public List<KademliaNode> getOutgoingConnections(){
    	return outgoingConnections;
    }
    
    public void setCallBack(Discv5ZipfTrafficGenerator client, Node n,Topic t) {
    	this.client = client;
    	this.n = n;
    	this.t = t;
    }
    
    private void tryNewConnections() {
       	while(outgoingConnections.size()<maxOutgoingConnections&&lookupResultBuffer.size()>0) {
    		boolean success=false;
    		while(!success&&lookupResultBuffer.size()>0){
    	    	//System.out.println(CommonState.getTime()+" Kademlianode:"+id+" start connection "+lookupResultBuffer.size());
    			int n = CommonState.r.nextInt(lookupResultBuffer.size());
    			success = startConnection(lookupResultBuffer.get(n));
    			
    			if(success)addOutgoingConnection(lookupResultBuffer.get(n));
    			lookupResultBuffer.remove(n);
    	    	//System.out.println(CommonState.getTime()+" Kademlianode:"+id+" start2 connection "+lookupResultBuffer.size());

    		}
    	}
       	if(lookupResultBuffer.size()==0&&client!=null&&n!=null){
       		//System.out.println(CommonState.getTime()+" emptybuffer:"+lookupResultBuffer.size());
       		if(!requested) {
       			if(client!=null&&n!=null&&t!=null)client.emptyBufferCallback(n,t);
       			requested=true;
       		}
       	}
    }
    
    private boolean startConnection(KademliaNode node) {
    	Node n = Util.nodeIdtoNode(node.getId());
    	return(n.isUp()&&(n.getKademliaProtocol().getNode().addIncomingConnection(node)));
    }
}
