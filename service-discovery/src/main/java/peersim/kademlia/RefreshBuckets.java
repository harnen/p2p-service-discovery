package peersim.kademlia;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;
import peersim.edsim.EDSimulator;


public class RefreshBuckets implements Control {

	private String prefix;


	// ______________________________________________________________________________________________
	public RefreshBuckets(String prefix) {
		this.prefix = prefix;
	}


	// ______________________________________________________________________________________________
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node iNode = Network.get(i);
			if(iNode.getFailState()==Node.OK) {
                KademliaProtocol iKad = iNode.getKademliaProtocol();
				iKad.refreshBuckets();
			}
		}

		return false;
	}
	

} // End of class

