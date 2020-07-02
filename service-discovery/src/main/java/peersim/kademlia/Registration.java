package peersim.kademlia;

import java.math.BigInteger;
import java.util.Date;

public class Registration implements Comparable<Registration>{

    private BigInteger node;
    //have to check how to use time (number of cycles in peersim)
    private long timestamp;

    public Registration(BigInteger node) {
        this.node = node;
        this.timestamp = 0;
    }

    public Registration(Registration r) {
        this.node = r.node;
        this.timestamp = r.timestamp;
    }

    // a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.

    public int compareTo(Registration r){
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
        if (!(o instanceof Registration)) { 
            return false; 
        } 
          
        // typecast o to Complex so that we can compare data members  
        Registration r = (Registration) o; 
          
        if(this.node == r.node) return true;
        return false;
    } 

    public String toString(){
        return "[node:" + this.node + " date: " + this.timestamp +  "]";
    }

    public void setTimestamp(long t){
        this.timestamp = t;
    }

}