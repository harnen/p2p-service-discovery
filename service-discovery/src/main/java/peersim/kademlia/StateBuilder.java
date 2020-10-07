package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
public class StateBuilder implements peersim.core.Control {

	private static final String PAR_PROT = "protocol";
	private static final String EVIL_PAR_PROT = "evilProtocol";
	private static final String PAR_TRANSPORT = "transport";

	private String prefix;
	private int kademliaid;
    private int evilKademliaid;
	private int transportid;

	public StateBuilder(String prefix) {
		this.prefix = prefix;
		kademliaid = Configuration.getPid(this.prefix + "." + PAR_PROT);
		evilKademliaid = Configuration.getPid(this.prefix + "." + EVIL_PAR_PROT);
		transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
	}

	// ______________________________________________________________________________________________
	public final KademliaProtocol get(int i) {
		return ((KademliaProtocol) (Network.get(i)).getProtocol(kademliaid));
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
				KademliaProtocol p1 = (KademliaProtocol) (n1.getProtocol(kademliaid));
                if (p1 == null) 
				    p1 = (KademliaProtocol) (n1.getProtocol(evilKademliaid));

				KademliaProtocol p2 = (KademliaProtocol) (n2.getProtocol(kademliaid));
                if (p2 == null) 
				    p2 = (KademliaProtocol) (n2.getProtocol(evilKademliaid));
                
                BigInteger id = p1.node.getId();
				return Util.put0(p1.node.getId()).compareTo(Util.put0(p2.node.getId()));
			}

		});

		int sz = Network.size();

		// for every node take 50 random node and add to k-bucket of it
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
            KademliaProtocol iKad;
            if (iNode.getProtocol(kademliaid) != null)
    			iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));
            else
    			iKad = (KademliaProtocol) (iNode.getProtocol(evilKademliaid));

			for (int k = 0; k < 100; k++) {
                int index = CommonState.r.nextInt(sz);
				KademliaProtocol jKad = (KademliaProtocol) (Network.get(index).getProtocol(kademliaid));
                if (jKad == null)
                    jKad = (KademliaProtocol) (Network.get(index).getProtocol(evilKademliaid));

				iKad.routingTable.addNeighbour(jKad.node.getId());
			}
		}

		// add other 50 near nodes
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			KademliaProtocol iKad = (KademliaProtocol) (iNode.getProtocol(kademliaid));
            if (iKad == null)
                iKad = (KademliaProtocol) (iNode.getProtocol(evilKademliaid));

			int start = i;
			if (i > sz - 50) {
				start = sz - 25;
			}
			for (int k = 0; k < 50; k++) {
				start = start++;
				if (start > 0 && start < sz) {
					KademliaProtocol jKad = (KademliaProtocol) (Network.get(start++).getProtocol(kademliaid));
                    if (jKad == null)
                        jKad = (KademliaProtocol) (Network.get(start-1).getProtocol(evilKademliaid));
					iKad.routingTable.addNeighbour(jKad.node.getId());
				}
			}
		}
		
		if(!isNetworkConnected()) {
			System.err.println("Your network is not connected - try a different seed");
			System.exit(-1);
		}else {
			System.err.println("Your network is connected");
		}

		return false;

	} // end execute()
	
	
	private boolean isNetworkConnected() {
		
	
		ArrayList<Set<BigInteger>> groups = new ArrayList<Set<BigInteger>>(); 
	
		for(int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i); 
			KademliaProtocol prot = (KademliaProtocol) (node.getProtocol(kademliaid));
            if (prot == null)
                prot = (KademliaProtocol) (node.getProtocol(evilKademliaid));
			BigInteger id = prot.node.getId();
			Set<BigInteger> neighbours = prot.routingTable.getAllNeighbours();
			
			boolean added = false;
			for(Set<BigInteger> group: groups) {
				HashSet<BigInteger> intersection = new HashSet<BigInteger>(group);
				intersection.retainAll(neighbours);
				if(intersection.size() > 0) {
					group.addAll(neighbours);
					added = true;
					break;
				}
			}
			if(!added) {
				groups.add(neighbours);
			}
		}

		//try merging groups
		boolean merged = true;
		while((groups.size() > 1) && !merged) {
			merged = false;
			for (int i = 1; i < groups.size(); i++) {
				HashSet<BigInteger> intersection = new HashSet<BigInteger>(groups.get(0));
				intersection.retainAll(groups.get(i));
				if(intersection.size() > 0) {
					groups.get(0).addAll(groups.get(i));
					groups.remove(i);
					merged = true;
					break;
				}			
			}
		}
		if(groups.size() == 1) {
			return true;
		}else {
			return false;
		}
	}

}
