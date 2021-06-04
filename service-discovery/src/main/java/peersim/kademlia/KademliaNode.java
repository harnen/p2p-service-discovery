package peersim.kademlia;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.math.BigInteger;

public class KademliaNode implements Comparable<KademliaNode>{
    private BigInteger id;
    // attackerId is the ID used by Sybil nodes (when multiple nodes
    private BigInteger attackerID; 
    private String addr;
    private int port;
    
    private Node n;
    
    //private Discv5ZipfTrafficGenerator client;
    
    boolean requested=false;

    public boolean is_evil=false;
    
    public HashMap<String,NodeConnections> connections;

    
    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.attackerID = null;
        this.addr = addr;
        this.port = port;
        this.connections = new HashMap<>();

    }
    
    public KademliaNode(BigInteger id, BigInteger attackerId,  String addr, int port) {
        this.id = id;
        this.attackerID = attackerId;
        this.addr = addr;
        this.port = port;
        this.connections = new HashMap<>();
    }

    public KademliaNode(BigInteger id){
        this.id = id;
        this.addr = "127.0.0.1";
        this.port = 666;
        this.attackerID = null;
        this.connections = new HashMap<>();

    }

    public KademliaNode(KademliaNode n){
        this.id = n.id;
        this.addr = n.addr;
        this.port = n.port;
        this.is_evil = n.is_evil;
        this.attackerID = n.attackerID;
        this.connections = new HashMap<>();

    }

    public BigInteger getId(){
        return this.id;
    }
    
    public BigInteger getAttackerId() {
        return this.attackerID;
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
        if(!this.addr.equals(n.addr)){
            //return this.addr.compareTo(n.addr);
        	return -1;
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
    
    public void setLookupResult(List<KademliaNode> result, String topic) {
    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" setLookupResult "+result.size());
    	//lookupResultBuffer = result;
    	//for(KademliaNode rest : result)
    	//	if(lookupResultBuffer.size()<10)lookupResultBuffer.add(rest);
    	connections.get(topic).addLookupResult(result);
    	connections.get(topic).setRequested(false);
    	//System.out.println(CommonState.getTime()+" Kademlianode:"+id+" setLookupResult "+lookupResultBuffer.size()+" "+outgoingConnections.size());
    	if(getLookupResult(topic).size()>0)
    		tryNewConnections(topic);
    	else {
    		//System.out.println(CommonState.getTime()+" emptybuffer:"+lookupResultBuffer.size()+" Sending lookup");
   
    		if(n!=null&&!connections.get(topic).isRequested()) {
    			EDSimulator.add(0,generateTopicLookupMessage(topic),n, n.getKademliaProtocol().getProtocolID());
    			connections.get(topic).setRequested(true);
    		}


    	}
    }
    		
    public List<KademliaNode> getLookupResult(String topic) {
    	return connections.get(topic).getLookupBuffer(); 
    	
    }
    
    public boolean addIncomingConnection(KademliaNode node,String topic) {
    	return connections.get(topic).addIncomingConnection(node);

    }	

    public void deleteIncomingConnection(KademliaNode node) {
    	
    	for(NodeConnections nc : connections.values())
    		nc.removeIncomingNode(node);
    	//incomingConnections.remove(node);

    }
    
    public void deleteOutgoingConnection(KademliaNode node) {
    	/*System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection:"+outgoingConnections.size()+" "+addr);
    	for(BigInteger ad : outgoingConnections)
    	{
        	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection:"+ad);
    	}*/

    	for(String t : connections.keySet())
    		if(connections.get(t).removeOutgoingNode(node))tryNewConnections(t);

    	/*outgoingConnections.remove(node);
    	System.out.println(CommonState.getTime()+" Kademlianode:"+id+" deleteOutgoingConnection2:"+outgoingConnections.size()+" "+lookupResultBuffer.size());
    	tryNewConnections();*/

    }
    
    public List<KademliaNode> getIncomingConnections(String topic){
    	if(connections.get(topic)!=null)return connections.get(topic).getIncomingConnections();
    	else return null;
    }
    
    public List<KademliaNode> getOutgoingConnections(String topic){
    	if(connections.get(topic)!=null)return connections.get(topic).getOutgoingConnections();
    	else return null;
    }
    
    public List<KademliaNode> getTotalIncomingConnections(){
    	List<KademliaNode> connectionsList = new ArrayList<>();
    	for(NodeConnections n : connections.values()) {
    		connectionsList.addAll(n.getIncomingConnections());
    	}
    	return connectionsList;
    }
    
    public List<KademliaNode> getTotalOutgoingConnections(){
    	List<KademliaNode> connectionsList = new ArrayList<>();
    	for(NodeConnections n : connections.values()) {
    		connectionsList.addAll(n.getOutgoingConnections());
    	}
    	return connectionsList;
    }
   /* public void setCallBack(Discv5ZipfTrafficGenerator client, Node n,Topic t) {
    	this.client = client;
    	this.n = n;
    	this.t = t;
    }*/
    
    public void setTopic(String t, Node n) {
    	this.n = n;
    	if(connections.get(t)==null) {
    		connections.put(t, new NodeConnections(t,this));
    	}
    }
    
    public void setTopicList(List<String> t, Node n) {
    	for(String topic : t)
    		setTopic(topic,n);
    }
    
    public boolean isEclipsed(String topic) {
    	
        if (this.is_evil)
            //Don't include malicious nodes in the count
            return false;
        
        if(connections.get(topic)!=null)
        	return connections.get(topic).isEclipsed();
        
        return false;
    }
    
    public boolean isEclipsed() {
    	
    	for(String topic : connections.keySet())
    		if(isEclipsed(topic)) return true;
        
    	
        return false;

    }
    
    private void tryNewConnections(String topic) {
    	//System.out.println(CommonState.getTime()+" "+id+" trying connections "+maxOutgoingConnections);
    	
    	connections.get(topic).tryNewConnections();
       	if(connections.get(topic).getLookupBuffer().size()==0){
       		System.out.println(CommonState.getTime()+" "+id+" emptybuffer:"+connections.get(topic).getLookupBuffer().size()+" "+connections.get(topic).getOutgoingConnections().size());
       		if(!connections.get(topic).isRequested()) {
       			//client.emptyBufferCallback(n,t);
       			if(n!=null)EDSimulator.add(10000,generateTopicLookupMessage(topic),n, n.getKademliaProtocol().getProtocolID());
       			connections.get(topic).setRequested(true);
       		}
       	}
    }

	// ______________________________________________________________________________________________
	/**
	 * generates a topic lookup message, by selecting randomly the destination and one of previousely registered topic.
	 * 
	 * @return Message
	 */
	protected Message generateTopicLookupMessage(String topic) {
		System.out.println("New lookup message "+topic);

		Topic t = new Topic(topic);
		Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
		m.timestamp = CommonState.getTime();

		return m;
	}
	
}
