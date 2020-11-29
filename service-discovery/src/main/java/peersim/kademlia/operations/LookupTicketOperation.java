package peersim.kademlia.operations;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import peersim.core.CommonState;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.SearchTable;
import peersim.kademlia.Topic;
import peersim.kademlia.Util;

public class LookupTicketOperation extends LookupOperation {

	SearchTable sTable;

	public LookupTicketOperation(BigInteger srcNode, SearchTable sTable, Long timestamp, Topic t) {
		super(srcNode, timestamp, t);
		this.sTable = sTable;
		// TODO Auto-generated constructor stub
	}


	public BigInteger getNeighbour() {
		BigInteger res = null;
		ArrayList<BigInteger> neighbours = new ArrayList<BigInteger>();
		int tries=0;
		
		while((neighbours.size() == 0)&&(tries<sTable.getnBuckets())) {
			//int distance = ThreadLocalRandom.current().nextInt(KademliaCommonConfig.BITS-sTable.getnBuckets(),KademliaCommonConfig.BITS);
			int distance = KademliaCommonConfig.BITS - CommonState.r.nextInt(sTable.getnBuckets());
			tries++;
			Collections.addAll(neighbours, sTable.getNeighbours(distance));
			//System.out.println("Distance "+distance+" "+neighbours.size());
		}
		
		/*for(int dist = sTable.getbucketMinDistance(); dist <= KademliaCommonConfig.BITS; dist++) {
			Collections.addAll(neighbours, sTable.getNeighbours(dist));
			if(neighbours.size() != 0)
				break;
		}*/
		
		/*for(BigInteger n: neighbours) {
			System.out.println("Logdist to the topic: " + Util.logDistance(topic.getTopicID(), n));
		}
		
		System.out.println("Used:");
		for(BigInteger n: this.used) {
			System.out.println(n);
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~");*/

		while(neighbours.size() != 0) {
			//res = neighbours.get(ThreadLocalRandom.current().nextInt(neighbours.size()));
			res = neighbours.get(CommonState.r.nextInt(neighbours.size()));
			
			//don't ask the same neighbour twice
			/*if(this.used.contains(res)) {
				neighbours.remove(res);
				res = null;
			}else {
				break;
			}*/
			break;
		}
		
		if(res!=null) {
			sTable.removeNeighbour(res);
			//returned.add(res);
			//increaseUsed(res);
			available_requests--;
		}
		
		
		
		return res;
	}
	


}
