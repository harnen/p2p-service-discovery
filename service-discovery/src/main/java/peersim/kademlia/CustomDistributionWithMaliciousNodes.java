package peersim.kademlia;

import java.math.BigInteger;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

/**
 * This control initializes the whole network (that was already created by peersim) assigning a unique NodeId, randomly generated,
 * to every node.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class CustomDistributionWithMaliciousNodes implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String PAR_EVIL_PROT = "evilProtocol";
    // percentage of nodes that are evil
	private static final String PAR_PERCENT_EVIL = "percentEvil";
	//private static final String DISCV5_PAR_PROT = "discv5_protocol";

	private int protocolID;
	private int evilProtocolID;
    private double percentEvil;
	//private int discv5_protocolID=-1;
	private UniformRandomGenerator urg;

	public CustomDistributionWithMaliciousNodes(String prefix) {
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
		evilProtocolID = Configuration.getPid(prefix + "." + PAR_EVIL_PROT);
        percentEvil = Configuration.getDouble(prefix + "." + PAR_PERCENT_EVIL);
        /*if (Configuration.isValidProtocolName(prefix + "." + DISCV5_PAR_PROT)) {
		    discv5_protocolID = Configuration.getPid(prefix + "." + DISCV5_PAR_PROT);
        }*/

		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	/**
	 * Scan over the nodes in the network and assign a randomly generated NodeId in the space 0..2^BITS, where BITS is a parameter
	 * from the kademlia protocol (usually 160)
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {

        int num_evil_nodes = (int) (Network.size()*percentEvil);
        System.out.println("Number of evil nodes: " + num_evil_nodes);
		
		for (int i = 0; i < Network.size(); ++i) {
            Node generalNode = Network.get(i);
			BigInteger id;
			id = urg.generate();
			KademliaNode node = new KademliaNode(id, "127.0.0.1", 0);
			
            if (i <= num_evil_nodes) {
                generalNode.setProtocol(protocolID, null);
                node.setProtocolId(evilProtocolID);
                node.setOtherProtocolId(protocolID);
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setNode(node);
            }
            else {
                generalNode.setProtocol(evilProtocolID, null);
                node.setProtocolId(protocolID);
                node.setOtherProtocolId(evilProtocolID);
                ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
            }
		}

		return false;
	}

}
