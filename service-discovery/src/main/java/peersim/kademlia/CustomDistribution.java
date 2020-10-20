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
public class CustomDistribution implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String PAR_EVIL_PROT = "evilProtocol";
	private static final String PAR_PERCENT_EVIL = "percentEvil";
	
    private int protocolID;
	private int evilProtocolID;
    private double percentEvil;
	private UniformRandomGenerator urg;

	public CustomDistribution(String prefix) {
		protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
        // Optional configurations when including secondary (malicious) protocol:
		evilProtocolID = Configuration.getPid(prefix + "." + PAR_EVIL_PROT, -1);
        percentEvil = Configuration.getDouble(prefix + "." + PAR_PERCENT_EVIL, 0.0);

		urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
	}

	/**
	 * Scan over the nodes in the network and assign a randomly generated NodeId in the space 0..2^BITS, where BITS is a parameter
	 * from the kademlia protocol (usually 160)
     *
     * Assign a percentage of nodes (if percentEvil is greater than 0.0) to run 
	 * a secondary protocol - those nodes can be the  malicious ones. 
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
            if (i < num_evil_nodes) {
                generalNode.setProtocol(protocolID, null);
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setNode(node);
                generalNode.setKademliaProtocol((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID)));
                ((KademliaProtocol) (Network.get(i).getProtocol(evilProtocolID))).setProtocolID(evilProtocolID);
            }
            else {
                if (evilProtocolID != -1) {
                    generalNode.setProtocol(evilProtocolID, null);
                }
                generalNode.setKademliaProtocol((KademliaProtocol) (Network.get(i).getProtocol(protocolID)));
                ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setNode(node);
                ((KademliaProtocol) (Network.get(i).getProtocol(protocolID))).setProtocolID(protocolID);
            }
			
		}

		return false;
	}

}
