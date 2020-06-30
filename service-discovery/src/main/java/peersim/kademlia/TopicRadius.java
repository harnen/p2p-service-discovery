package peersim.kademlia;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import peersim.core.CommonState;

public class TopicRadius {
	
	private static final int radiusBucketsPerBit = 8;
	private static final int minPeakSize = 40;
	private static final double maxNoAdjust = 20;
	
	Topic topic;
	BigInteger topicHashPrefix;
	BigInteger radius, minRadius;
	TopicRadiusBucket []buckets;
	boolean converged;        
	int radiusLookupCnt;
	
	TopicRadius(Topic t){
		topic=t;
		topicHashPrefix = new BigInteger(Arrays.copyOfRange(topic.topicID.toByteArray(), 0, 8));
		minRadius=radius=new BigInteger("0xffffffffffffffff",16);
	}
	
	
	int getBucketIdx(BigInteger address){
		//return 0;
		BigInteger prefix = new BigInteger(Arrays.copyOfRange(address.toByteArray(), 0, 8));
		//String prefix = 
		double log2=0.0;
		if(prefix!=topicHashPrefix) {
			log2 = Math.log((double)prefix.xor(topicHashPrefix).doubleValue());
		}
		int bucket = (64 - (int)log2)* radiusBucketsPerBit;
		int max = 64*radiusBucketsPerBit - 1;
		if(bucket>max) {
			return max;
		}
		if(bucket < 0) {
			return 0;
		}
		return bucket;
		
	}
	
	//return a random target corresponding to the given bucket number
	BigInteger targetForBucket(int bucket){
		/*	min := math.Pow(2, 64-float64(bucket+1)/radiusBucketsPerBit)
				max := math.Pow(2, 64-float64(bucket)/radiusBucketsPerBit)
				a := uint64(min)
				b := randUint64n(uint64(max - min))
				xor := a + b
				if xor < a {
					xor = ^uint64(0)
				}
				prefix := r.topicHashPrefix ^ xor
				var target common.Hash
				binary.BigEndian.PutUint64(target[0:8], prefix)
				globalRandRead(target[8:])
				return target*/
		double min = Math.pow(2,64-(double)bucket+1/radiusBucketsPerBit);
		double max = Math.pow(2, 64-(double)bucket/radiusBucketsPerBit);
		BigInteger a = BigDecimal.valueOf(min).toBigInteger();
		Random rnd = new Random();
		BigInteger b = new BigInteger(64,rnd).mod( BigDecimal.valueOf(max-min).toBigInteger());
		BigInteger xor = a.xor(b);
		if(xor.compareTo(a)==-1)
			xor=BigInteger.valueOf(0);
		BigInteger prefix = topicHashPrefix.xor(xor);
		BigInteger rand = new BigInteger(64,rnd);
		for(int i=0;i<8;i++) {
			rand.clearBit(i);
		}
		
		return prefix.xor(rand);

	}
	
	//choose bucketleft or right
	int chooseLookupBucket(int a, int b) {

		if(a<0) {
			a=0;
		}
		if(a>b) {
			return -1;
		}
		int c =0;
		for(int i=a;i<=b;i++) {
			if((i>buckets.length)||(buckets[i].getWeights()[TopicRadiusBucket.trNoAdjust] < maxNoAdjust)) {
				c++;
			}
		}
		if(c==0) {
			return -1;
		}
		int rnd = new Random().nextInt(c);
		for(int i=0;i<=b;i++) {
			if((i>buckets.length)||(buckets[i].getWeights()[TopicRadiusBucket.trNoAdjust] < maxNoAdjust)) {
				if(rnd==0) {
					return i;
				}
				rnd--;
			}
		}
		return -1;
		
	}

	//true if more lookups are necessary
	boolean needMoreLookups(int a, int b, float maxValue) {
		
		double max=0.0;
		
		if (a<0) {
			a=0;
		}
		if(b>=buckets.length) {
			b=buckets.length-1;
			if(buckets[b].getValue() > max) {
				max = buckets[b].getValue();
			}
		}
		
		if(b>=a) {
			for(int i=a;i<=b;i++) {
				if(buckets[i].getValue()>max) {
					max = buckets[i].getValue();
				}
			}
		}

		return maxValue-max < minPeakSize;
	}
	
	//radius calculator
	void recalcRadius() {
		
	}
	
	//return lookup
	BigInteger nextTarget(boolean forceRegular) {
		return BigInteger.valueOf(1); 
	}
	
	//adjust radius when ticket received
	void adjustWithTicket(long time, BigInteger targetHash/*,Ticketref t*/) {
		
	}
	
	//adjust radius when ticket received
	void adjust(long time, BigInteger targetHash, BigInteger addrHash, float inside) {
		int bucket = getBucketIdx(addrHash);
		
		if(bucket>=buckets.length) {
			return;
		}
		buckets[bucket].adjust(CommonState.getTime(),inside);
		buckets[bucket].deleteLookupSent(targetHash);
	}
		
}
