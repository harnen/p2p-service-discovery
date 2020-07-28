package peersim.kademlia;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class Topic implements Comparable<Topic> {
    // ID of the node storing the topic - this is used to enable sorted collections,
    // priority queues etc.
    protected BigInteger hostID;
    protected BigInteger topicID;
    protected String topic;
    /*
     * Create a new topic. The topicID is created using SHA-256 hash of the string.
     * All the node IDs are positive, so to make it uniform, we create only positive
     * topic IDs as well. 
    */

    public Topic(String topic){
        this.hostID = new BigInteger("0");
        this.topic = topic;

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(topic.getBytes(StandardCharsets.UTF_8));
            this.topicID = new BigInteger(hash, 0, KademliaCommonConfig.BITS/8).abs();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }
    public Topic(BigInteger hostID, String topic) {
        this.hostID = hostID;
        this.topic = topic;

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(topic.getBytes(StandardCharsets.UTF_8));
            this.topicID = new BigInteger(hash, 0, KademliaCommonConfig.BITS/8).abs();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
    }

    public Topic(Topic t){
        this.hostID = t.hostID;
        this.topicID = t.topicID;
        this.topic = t.topic;
    }


    public int compareTo(Topic t){
        assert this.hostID == t.hostID : "Comparing topics with different hostID";
        return Util.distance(this.hostID, this.topicID).compareTo(Util.distance(t.hostID, t.topicID));
    }

    public String toString(){
        return "[hostID=" + this.hostID + "][topicID=" + this.topicID + "][topic=" + this.topic + "]";
    }

    public void setHostID(BigInteger hostID){
        this.hostID = hostID; 
    }

    public BigInteger getHostID(){
        return this.hostID;
    }

    public String getTopic(){
        return this.topic;
    }

    public BigInteger getTopicID(){
        return this.topicID;
    }

    @Override
    public int hashCode()
    {
        return this.topic.hashCode();
    }

}
