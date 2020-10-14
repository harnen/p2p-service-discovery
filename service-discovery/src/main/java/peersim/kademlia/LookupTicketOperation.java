package peersim.kademlia;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import peersim.core.CommonState;

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
		
		int distance = CommonState.r.nextInt(KademliaCommonConfig.NBUCKETS);
		//System.out.println("Distance "+distance);
		int tries=0;
		while((neighbours.length==0)&&(tries<KademliaCommonConfig.NBUCKETS)) {
			//System.out.println("Distance "+distance);
			distance = CommonState.r.nextInt(KademliaCommonConfig.NBUCKETS);
			tries++;
			//System.out.println("Distance "+distance);
			neighbours = sTable.getNeighbours(distance);
			//System.out.println("Distance "+distance+" "+neighbours.length);
		}

		if(neighbours.length!=0)res = neighbours[CommonState.r.nextInt(neighbours.length)];
		if(res!=null) {
			sTable.removeNeighbour(res);
		
		}
		return res;
	}
	


}
