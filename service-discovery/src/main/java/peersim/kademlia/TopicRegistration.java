package peersim.kademlia;

import java.math.BigInteger;
import java.util.Date;

import peersim.core.CommonState;

public class TopicRegistration implements Comparable<TopicRegistration>{

    private KademliaNode node;
    //have to check how to use time (number of cycles in peersim)
    private long timestamp;
    private Topic topic;

    public TopicRegistration(KademliaNode node) {
        this.node = node;
        this.topic = new Topic();
        this.timestamp = CommonState.getTime();
    }
    
public TopicRegistration(KademliaNode node, Topic topic) {
        this.node = node;
        this.topic = topic;
        this.timestamp = CommonState.getTime();
    }

    public TopicRegistration(TopicRegistration r) {
        this.node = r.node;
        this.timestamp = r.timestamp;
        this.topic = new Topic(r.topic);
    }

    // a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.

    public int compareTo(TopicRegistration r){
        return this.node.compareTo(r.node);
        /*if (this.timestamp < r.timestamp) return -1;
        if (this.timestamp == r.timestamp) return 0;
        return 1;*/
    }

    @Override
    public boolean equals(Object o) { 
  
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not 
          "null instanceof [type]" also returns false */
        if (!(o instanceof TopicRegistration)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        TopicRegistration r = (TopicRegistration) o; 
          
        if(this.node == r.node) return true;
        return false;
    } 

    public String toString(){
        return "[node:" + this.node + " date: " + this.timestamp +  "]";
    }

    public void setTimestamp(long t){
        this.timestamp = t;
    }
    
    public long getTimestamp() {
        return this.timestamp;
    }

    public Topic getTopic() {
        return this.topic;
    }
    
    public KademliaNode getNode() {
    	return this.node;
    }

}
