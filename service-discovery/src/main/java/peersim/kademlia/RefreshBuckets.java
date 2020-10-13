package peersim.kademlia;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;
import peersim.edsim.EDSimulator;

/**
 * Turbulcen class is only for test/statistical purpose. This Control execute a node add or remove (failure) with a given
 * probability.<br>
 * The probabilities are configurabily from the parameters p_idle, p_add, p_rem.<BR>
 * - p_idle (default = 0): probability that the current execution does nothing (i.e. no adding and no failures).<br>
 * - p_add (default = 0.5): probability that a new node is added in this execution.<br>
 * - p_rem (deafult = 0.5): probability that this execution will result in a failure of an existing node.<br>
 * If the user desire to change one probability, all the probability value MUST be indicated in the configuration file. <br>
 * Other parameters:<br>
 * - maxsize (default: infinite): max size of network. If this value is reached no more add operation are performed.<br>
 * - minsize (default: 1): min size of network. If this value is reached no more remove operation are performed.<br>
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */

public class RefreshBuckets implements Control {

	private int kademliaid;
	private int evilKademliaid;
	private static final String PAR_PROT = "protocol";
	private static final String EVIL_PAR_PROT = "evilProtocol";
	private String prefix;


	// ______________________________________________________________________________________________
	public RefreshBuckets(String prefix) {
		this.prefix = prefix;
		kademliaid = Configuration.getPid(this.prefix + "." + PAR_PROT);
        evilKademliaid = Configuration.getPid(this.prefix + "." + EVIL_PAR_PROT, -1);
		//System.out.println("Refresh "+kademliaid);
		//System.err.println(String.format("Turbolence: [p_idle=%f] [p_add=%f] [(min,max)=(%d,%d)]", p_idle, p_add, maxsize, minsize));
	}


	// ______________________________________________________________________________________________
	public boolean execute() {
		// throw the dice
		// for every node take 50 random node and add to k-bucket of it
		//System.out.println("RefreshBuckets execute "+CommonState.getTime());
		for (int i = 0; i < Network.size(); i++) {
			Node iNode = Network.get(i);
			if(iNode.getFailState()==Node.OK) {
                KademliaProtocol iKad;
                if (iNode.getProtocol(kademliaid) != null)
				    iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));
                else
				    iKad = (KademliaProtocol) (iNode.getProtocol(evilKademliaid));

				iKad.refreshBuckets(kademliaid, evilKademliaid);
			}
		}

		return false;
	}
	

} // End of class

