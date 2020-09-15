package peersim.kademlia;

import java.math.BigInteger;
import java.util.HashMap;
import peersim.kademlia.Operation;

/**
 * This class represents a find operation and offer the methods needed to maintain and update the closest set.<br>
 * It also maintains the number of parallel requsts that can has a maximum of ALPHA.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class RegisterOperation extends Operation{

	private Topic topic;
	private TopicRegistration registration;


	/**
	 * defaul constructor
	 * 
	 * @param destNode
	 *            Id of the node to find
	 */
	public RegisterOperation(long timestamp, Topic t, TopicRegistration r) {
		
		super(t.getTopicID(), Message.MSG_REGISTER, timestamp);

		this.registration = r;
		this.topic = t;
	}
    
    public RegisterOperation(long timestamp, Topic t, BigInteger targetAddr) {
        super(targetAddr, Message.MSG_REGISTER, timestamp);
        this.topic = t;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
