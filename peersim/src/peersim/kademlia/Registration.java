package peersim.kademlia;

import java.math.BigInteger;
import java.util.Date;

public class Registration implements Comparable<Registration>{

    public String topic;
    public Integer id;
    //have to check how to use time (number of cycles in peersim)
    public Integer timestamp;

    public Registration(String topic, Integer id, Integer timestamp) {
        this.topic = topic;
        this.id = id;
        this.timestamp = timestamp;
    }

    // a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.

    public int compareTo(Registration r){
        
        if (this.topic != r.topic){
            System.out.println("Not the same topic");
            return this.topic.compareTo(r.topic);
        }

            if (this.id != r.id){
                System.out.println("Not the same id");
                return this.id.compareTo(r.id);
            }

            if (this.timestamp != r.timestamp){
                System.out.println("Not the same timestamp");
                return this.timestamp.compareTo(r.timestamp);
            }
        

        
        return 0;
    }

}