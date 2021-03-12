package peersim.kademlia;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.math.BigInteger;

/**
 * This control generates random search traffic from nodes to random destination node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

// ______________________________________________________________________________________________
public class Discv5TrafficGenerator implements Control {

	// ______________________________________________________________________________________________
	/**
	 * MSPastry Protocol to act
	 */
	private final static String PAR_PROT = "protocol";

	private boolean first = true;

	private int topicCounter = 0;
	
	private int counter = 0;
	private int limit = 1;

	/**
	 * MSPastry Protocol ID to act
	 */
	private final int pid;

	/**
	 * set to keep track of nodes that already initiated a register
	 */
    //private HashMap<Integer, Boolean>();
    

	// ______________________________________________________________________________________________
	public Discv5TrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);

	}
	
    // ______________________________________________________________________________________________
	/**
	 * generates a register message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
    private Message generateTopicLookupMessage() {
		Topic t = new Topic("t" + Integer.toString(CommonState.r.nextInt(this.topicCounter)));
		Message m = new Message(Message.MSG_INIT_TOPIC_LOOKUP, t);
		m.timestamp = CommonState.getTime();
		
		return m;
    }

	// ______________________________________________________________________________________________
	/**
	 * generates a register message, by selecting randomly the destination.
	 * 
	 * @return Message
	 */
	private Message generateRegisterMessage() {
		Topic topic = new Topic("t" + Integer.toString(0));
		Message m = Message.makeRegister(topic);
		m.timestamp = CommonState.getTime();
		//System.out.println("Topic id "+topic.topicID);
		
		return m;
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a topic register message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		if(counter >= limit){
			return false;
		}
		counter++;
		//first = false;
		System.out.println("Discv5 Traffic generator called");
		Node start;
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));

		// send register message
		EDSimulator.add(0, generateRegisterMessage(), start, pid);
		
		// send topic lookup message
        /*do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));
        EDSimulator.add(1, generateTopicLookupMessage(), start, pid);*/
		

		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
