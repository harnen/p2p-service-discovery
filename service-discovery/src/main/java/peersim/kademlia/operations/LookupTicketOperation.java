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
	int lastAskedBucket;

	public LookupTicketOperation(BigInteger srcNode, SearchTable sTable, Long timestamp, Topic t) {
		super(srcNode, timestamp, t);
		this.sTable = sTable;
		lastAskedBucket = KademliaCommonConfig.BITS;
	}

	private ArrayList<BigInteger> getRandomBucketNeighbours(){
		ArrayList<BigInteger> neighbours = new ArrayList<BigInteger>();
		int tries=0;
		while((neighbours.size() == 0)&&(tries<sTable.getnBuckets())) {
			//int distance = ThreadLocalRandom.current().nextInt(KademliaCommonConfig.BITS-sTable.getnBuckets(),KademliaCommonConfig.BITS);
			int distance = KademliaCommonConfig.BITS - CommonState.r.nextInt(sTable.getnBuckets());
			tries++;
			Collections.addAll(neighbours, sTable.getNeighbours(distance));
		}
		return neighbours;
	}
	
	private ArrayList<BigInteger> getMinBucketNeighbours(){
		ArrayList<BigInteger> neighbours = new ArrayList<BigInteger>();
		for(int dist = sTable.getbucketMinDistance(); dist <= KademliaCommonConfig.BITS; dist++) {
			Collections.addAll(neighbours, sTable.getNeighbours(dist));
			if(neighbours.size() != 0)
				break;
		}
		return neighbours;
	}
	
	private ArrayList<BigInteger> getAllBucketNeighbours(){
		ArrayList<BigInteger> neighbours = new ArrayList<BigInteger>();
		int tries = 0;
		for(; tries<sTable.getnBuckets(); lastAskedBucket--, tries++) {
			if(neighbours.size() != 0) break;
			if(lastAskedBucket < (KademliaCommonConfig.BITS - sTable.getnBuckets())) lastAskedBucket = KademliaCommonConfig.BITS;
			
			Collections.addAll(neighbours, sTable.getNeighbours(lastAskedBucket));
		}
		return neighbours;
	}
	

	public BigInteger getNeighbour() {
		BigInteger res = null;
		ArrayList<BigInteger> neighbours = null;
		
		switch(KademliaCommonConfig.LOOKUP_BUCKET_ORDER) {
			case KademliaCommonConfig.RANDOM_BUCKET_ORDER:
				neighbours = getRandomBucketNeighbours();
				break;
			case KademliaCommonConfig.CLOSEST_BUCKET_ORDER:
				neighbours = getMinBucketNeighbours();
				break;
			case KademliaCommonConfig.ALL_BUCKET_ORDER:
				neighbours = getAllBucketNeighbours();
				break;
		}
		
		if(neighbours.size() != 0) {
			//res = neighbours.get(ThreadLocalRandom.current().nextInt(neighbours.size()));
			res = neighbours.get(CommonState.r.nextInt(neighbours.size()));
			
			//We should never get the same neighbour twice
			assert !this.used.contains(res);
		}
		
		if(res!=null) {
			//System.out.println("Searching to node "+res);
			sTable.removeNeighbour(res);
			//returned.add(res);
			//increaseUsed(res);
			available_requests--;
		}else {
			System.out.println("Returning null");
		}
		
		
		
		
		return res;
	}
	


}
