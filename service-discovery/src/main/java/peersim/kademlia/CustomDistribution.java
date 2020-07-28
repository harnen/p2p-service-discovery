package peersim.kademlia;

import java.math.BigInteger;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;

/**
 * This control initializes the whole network (that was already created by peersim) assigning a unique NodeId, randomly generated,
 * to every node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class CustomDistribution implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String DISCV5_PAR_PROT = "discv5_protocol";

	private int protocolID;
	private int discv5_protocolID;
	private UniformRandomGenerator urg;

	public CustomDistribution(String prefix) {
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
		discv5_protocolID = Configuration.getPid(prefix + "." + DISCV5_PAR_PROT);

		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	/**
	 * Scan over the nodes in the network and assign a randomly generated NodeId in the space 0..2^BITS, where BITS is a parameter
	 * from the kademlia protocol (usually 160)
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		
		for (int i = 0; i < Network.size(); ++i) {
			BigInteger id;
			id = urg.generate();
			KademliaNode node = new KademliaNode(id, "127.0.0.1", 0);
			
			((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
			((Discv5Protocol) (Network.get(i).getProtocol(discv5_protocolID))).setNode(node, Network.get(i));
		}

		return false;
	}

}
