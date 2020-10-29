package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.concurrent.ThreadLocalRandom;

import peersim.core.CommonState;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.SearchTable;
import peersim.kademlia.Topic;

public class LookupTicketOperation extends LookupOperation {

	SearchTable sTable;

	public LookupTicketOperation(BigInteger srcNode, SearchTable sTable, Long timestamp, Topic t) {
		super(srcNode, timestamp, t);
		this.sTable = sTable;
		// TODO Auto-generated constructor stub
	}


	public BigInteger getNeighbour() {
		// find closest neighbour ( the first not already queried)
		BigInteger res = null;

		BigInteger[] neighbours = new BigInteger[0];
		
		int distance = ThreadLocalRandom.current().nextInt(KademliaCommonConfig.BITS-sTable.getnBuckets(),KademliaCommonConfig.BITS);
		//System.out.println("Distance "+distance);
		int tries=0;
		
		while((neighbours.length==0)&&(tries<sTable.getnBuckets())) {
			//System.out.println("Distance "+distance);
			distance = ThreadLocalRandom.current().nextInt(KademliaCommonConfig.BITS-sTable.getnBuckets(),KademliaCommonConfig.BITS);
			tries++;
			//System.out.println("Distance "+distance);
			neighbours = sTable.getNeighbours(distance);
			System.out.println("Distance "+distance+" "+neighbours.length);
		}

		if(neighbours.length!=0)res = neighbours[ThreadLocalRandom.current().nextInt(neighbours.length)];
		if(res!=null) {
			sTable.removeNeighbour(res);
			//returned.add(res);
			increaseUsed(res);
			available_requests--;
		}
		
		return res;
	}
	


}
