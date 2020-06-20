package peersim.kademlia;

import java.math.BigInteger;
import java.util.Date;

public class Registration implements Comparable<Registration>{

    public BigInteger node;
    //have to check how to use time (number of cycles in peersim)
    public Integer timestamp;

    public Registration(BigInteger node, Integer timestamp) {
        this.node = node;
        this.timestamp = timestamp;
    }

    // a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.

    public int compareTo(Registration r){
        return this.timestamp.compareTo(r.timestamp);
    }

}