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
	private Message generateRegisterMessage() {
		String topic = new String("t" + Integer.toString(this.topicCounter++));
		Message m = Message.makeRegister(topic);
		m.timestamp = CommonState.getTime();
		

		// existing active destination node
		Node n = Network.get(CommonState.r.nextInt(Network.size()));
		while (!n.isUp()) {
			n = Network.get(CommonState.r.nextInt(Network.size()));
		}
		//m.dest = ((Discv5Protocol) (n.getProtocol(pid))).node.getId();

		return m;
	}

	// ______________________________________________________________________________________________
	/**
	 * every call of this control generates and send a topic register message
	 * 
	 * @return boolean
	 */
	public boolean execute() {
		//first = false;
		if(!first){
			return false;
		}
		first = false;
		System.out.println("Discv5 Traffic generator called");
		Node start;
		do {
			start = Network.get(CommonState.r.nextInt(Network.size()));
		} while ((start == null) || (!start.isUp()));

		// send message
		EDSimulator.add(0, generateRegisterMessage(), start, pid);

		return false;
	}

	// ______________________________________________________________________________________________

} // End of class
// ______________________________________________________________________________________________
