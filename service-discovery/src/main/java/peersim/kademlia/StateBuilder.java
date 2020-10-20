package peersim.kademlia;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.Viewer;

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

	private static final String PAR_TRANSPORT = "transport";

	private String prefix;
	private int transportid;

	public StateBuilder(String prefix) {
		this.prefix = prefix;
		transportid = Configuration.getPid(this.prefix + "." + PAR_TRANSPORT);
	}

	// ______________________________________________________________________________________________
	public final KademliaProtocol get(int i) {
		return (Network.get(i)).getKademliaProtocol();
	}

	// ______________________________________________________________________________________________
	public final Transport getTr(int i) {
		return ((Transport) (Network.get(i)).getProtocol(transportid));
	}

	// ______________________________________________________________________________________________
	public static void o(Object o) {
		System.out.println(o);
	}


	
	/*public void visualize() {
		Graph graph = new SingleGraph("Tutorial 1");
		UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
		BigInteger max = urg.getMaxID();
		System.out.println("MAX: " + max);
		
		for(int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i); 
			KademliaProtocol prot = (KademliaProtocol) (node.getKademliaProtocol());
			BigInteger id = prot.node.getId();
			double ratio = id.doubleValue()/ max.doubleValue() * 360;
			System.out.println("ID: " + id + " div: " + ratio);
			double alpha = Math.toRadians(ratio);
			double x = Math.cos(alpha);
			double y = Math.sin(alpha);

			org.graphstream.graph.Node gnode = graph.addNode(id.toString());
		    gnode.setAttribute("x", x);
		    gnode.setAttribute("y", y);
		    gnode.setAttribute("ui.style", "fill-color: rgba(0,100,255, 50); size: 10px, 10px;");
		    if((i %10) == 0) {
		    	gnode.setAttribute("ui.style", "fill-color: rgb(255,0,0); size: 20px, 20px;");
		    }
		}
		System.setProperty("org.graphstream.ui", "swing"); 
		Viewer viewer = graph.display();
		viewer.disableAutoLayout();
		//System.exit(0);		
		//
	}*/

	
	// ______________________________________________________________________________________________
	public boolean execute() {
		// Sort the network by nodeId (Ascending)
		Network.sort(new Comparator<Node>() {

			public int compare(Node o1, Node o2) {
				Node n1 = (Node) o1;
				Node n2 = (Node) o2;
				KademliaProtocol p1 = n1.getKademliaProtocol();

				KademliaProtocol p2 = n2.getKademliaProtocol();
                
				return Util.put0(p1.node.getId()).compareTo(Util.put0(p2.node.getId()));
			}

		});

		int sz = Network.size();

		// for every node take 50 random node and add to k-bucket of it
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
            KademliaProtocol iKad = iNode.getKademliaProtocol();

			for (int k = 0; k < 100; k++) {
                int index = CommonState.r.nextInt(sz);
				KademliaProtocol jKad = Network.get(index).getKademliaProtocol();

				iKad.routingTable.addNeighbour(jKad.node.getId());
			}
		}

		// add other 50 near nodes
		for (int i = 0; i < sz; i++) {
			Node iNode = Network.get(i);
			KademliaProtocol iKad = (KademliaProtocol) (iNode.getKademliaProtocol());

			int start = i;
			if (i > sz - 50) {
				start = sz - 25;
			}
			for (int k = 0; k < 50; k++) {
				start = start++;
				if (start > 0 && start < sz) {
					KademliaProtocol jKad = (KademliaProtocol) (Network.get(start++).getKademliaProtocol());
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
			KademliaProtocol prot = (KademliaProtocol) (node.getKademliaProtocol());
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
