package peersim.kademlia;

import java.util.Comparator;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

/**
 * Initialization class that performs the bootsrap filling the k-buckets of all initial nodes.<br>
 * In particular every node is added to the routing table of every other node in the network. In the end however the various nodes
 * doesn't have the same k-buckets because when a k-bucket is full a random node in it is deleted.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class Discv5StateBuilder implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String PAR_TRANSPORT = "transport";

	private String prefix;
	private int discv5id;
	private int transportid;

	public Discv5StateBuilder(String prefix) {
		this.prefix = prefix;
		discv5id = Configuration.getPid(this.prefix + "." + PAR_PROT);
		transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
	}

	// ______________________________________________________________________________________________
	public final Discv5Protocol get(int i) {
		return ((Discv5Protocol) (Network.get(i)).getProtocol(discv5id));
	}

	// ______________________________________________________________________________________________
	public final Transport getTr(int i) {
		return ((Transport) (Network.get(i)).getProtocol(transportid));
	}

	// ______________________________________________________________________________________________
	public static void o(Object o) {
		System.out.println(o);
	}

	// ______________________________________________________________________________________________
	public boolean execute() {

		// Sort the network by nodeId (Ascending)
		Network.sort(new Comparator<Node>() {

			public int compare(Node o1, Node o2) {
				Node n1 = (Node) o1;
				Node n2 = (Node) o2;
				Discv5Protocol p1 = (Discv5Protocol) (n1.getProtocol(discv5id));
				Discv5Protocol p2 = (Discv5Protocol) (n2.getProtocol(discv5id));
				return Util.put0(p1.kademliaNode.getId()).compareTo(Util.put0(p2.kademliaNode.getId()));
			}

		});

        return false;
	} // end execute()

}
