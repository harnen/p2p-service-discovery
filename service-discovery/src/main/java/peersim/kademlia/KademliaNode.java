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
    
       
    
    //private List<String> topicList;

    
    public KademliaNode(BigInteger id, String addr, int port){
        this.id = id;
        this.attackerID = null;
        this.addr = addr;
        this.port = port;
    }
    
    public KademliaNode(BigInteger id, BigInteger attackerId,  String addr, int port) {
        this.id = id;
        this.attackerID = attackerId;
        this.addr = addr;
        this.port = port;
    }

    public KademliaNode(BigInteger id){
        this.id = id;
        this.addr = "127.0.0.1";
        this.port = 666;
        this.attackerID = null;
    }

    public KademliaNode(KademliaNode n){
        this.id = n.id;
        this.addr = n.addr;
        this.port = n.port;
        this.is_evil = n.is_evil;
        this.attackerID = n.attackerID;
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

    
    public void setTopic(String t, Node n) {
    	//System.out.println("Set topic "+t+" "+id);
    	this.n = n;
    	//this.topicList.
    	//this.topicList.add(t);
    	
    	//System.out.println(this +" Has topic "+this.id+" "+t+" "+connections.keySet().contains(t)+" "+this.connections.size());
    }
    
    /*public void setTopicDiscv4(String t, Node n) {
    	this.n = n;
    	//this.topicList.add(t);

    	if(connections.get(t)==null) {
    		//System.out.println("Add topic "+t);
    		NodeConnections con = new NodeConnectionsv4(t,this);
    		con.setRequested(true);
    		connections.put(t, con);
    	} else {
    		connections.get(t).setRequested(true);
    	}
    	
    }*/
    
    public void setTopicList(List<String> t, Node n) {
    	for(String topic : t)
    		setTopic(topic,n);
    }
    


	
}
