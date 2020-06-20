package peersim.kademlia;

import java.math.BigInteger;

public class Topic implements Comparable<Topic>{
    //ID of the node storing the topic - this is used to enable sorted collections, priority queues etc.
    BigInteger hostID;
    BigInteger topicID;
    String topic;


    public int compareTo(Topic t){
        assert this.hostID == t.hostID : "Comparing topics with different hostID";
        return Util.distance(this.hostID, this.topicID).compareTo(Util.distance(t.hostID, t.topicID));
    }
}