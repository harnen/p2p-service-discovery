package peersim.kademlia;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import peersim.core.CommonState;

public class TopicRadius {
	
	private static final int radiusBucketsPerBit = 8;
	private static final int minPeakSize = 40;
	private static final double maxNoAdjust = 20;
	private static final double targetWaitTime = 10*60*1000;//TODO define waiting time in cycles
	private static final BigInteger maxRadius = new BigInteger("0xffffffffffffffff",16);
	private static final int minSlope = 1;
	private static final double minRightSum = 20;
	private static final int lookupWidth = 8;
	
	Topic topic;
	BigInteger topicHashPrefix;
	BigInteger radius, minRadius;
	List<TopicRadiusBucket> buckets;
	boolean converged;        
	int radiusLookupCnt;
	
	TopicRadius(Topic t){
		topic=t;
		topicHashPrefix = new BigInteger(Arrays.copyOfRange(topic.topicID.toByteArray(), 0, 8));
		minRadius=radius=maxRadius;
		buckets = new ArrayList<TopicRadiusBucket>();
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

		double min = Math.pow(2,64-(double)bucket+1/radiusBucketsPerBit);
		double max = Math.pow(2, 64-(double)bucket/radiusBucketsPerBit);
		BigInteger a = BigDecimal.valueOf(min).toBigInteger();
		BigInteger b = newRandomMax(BigDecimal.valueOf(max-min).toBigInteger());
		BigInteger xor = a.xor(b);
		
		if(xor.compareTo(a)==-1)
			xor=BigInteger.valueOf(0);
		
		return calculatePrefix(xor);

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
			if((i>buckets.size())||(buckets.get(i).getWeight(TopicRadiusBucket.trNoAdjust) < maxNoAdjust)) {
				c++;
			}
		}
		if(c==0) {
			return -1;
		}
		int rnd = new Random().nextInt(c);
		for(int i=0;i<=b;i++) {
			if((i>buckets.size())||(buckets.get(i).getWeight(TopicRadiusBucket.trNoAdjust) < maxNoAdjust)) {
				if(rnd==0) {
					return i;
				}
				rnd--;
			}
		}
		return -1;
		
	}

	//true if more lookups are necessary
	boolean needMoreLookups(int a, int b, double maxValue) {
		
		double max=0.0;
		
		if (a<0) {
			a=0;
		}
		if(b>=buckets.size()) {
			b=buckets.size()-1;
			if(buckets.get(b).getValue() > max) {
				max = buckets.get(b).getValue();
			}
		}
		
		if(b>=a) {
			for(int i=a;i<=b;i++) {
				if(buckets.get(i).getValue()>max) {
					max = buckets.get(i).getValue();
				}
			}
		}

		return maxValue-max < minPeakSize;
	}
	
	//radius calculator
	private int recalcRadius() {

		int maxBucket = 0;
		double maxValue = 0.0;
		long now = CommonState.getTime();
		double v = 0.0;
		int radiusLookup;
		
		for(int i=0;i<buckets.size();i++) {
			buckets.get(i).update(now);
			v+=buckets.get(i).getWeight(TopicRadiusBucket.trOutside)-buckets.get(i).getWeight(TopicRadiusBucket.trInside);
			buckets.get(i).setValue(v);

		}
		int slopeCross=-1;
		
		for(int i=0;i<buckets.size();i++) {
			v=buckets.get(i).getValue();
			if(v<i*minSlope) {
				slopeCross=i;
				break;
			}
			if(v > maxValue){
				maxValue = v;
				maxBucket = i + 1;
			}
		}
		int minRadBucket = buckets.size();
		double sum=0.0;
		
		while(minRadBucket>0&&sum<minRightSum) {
			minRadBucket--;
			TopicRadiusBucket b = buckets.get(minRadBucket);
			sum+=(b.getWeight(TopicRadiusBucket.trInside)+b.getWeight(TopicRadiusBucket.trOutside));
		}
		minRadius=BigDecimal.valueOf(Math.pow(2,64-(double)minRadBucket/radiusBucketsPerBit)).toBigInteger();
		
		int lookupLeft=-1;
		if(needMoreLookups(0,maxBucket-lookupWidth-1,maxValue)) {
			lookupLeft=chooseLookupBucket(maxBucket-lookupWidth,maxBucket-1);
		}
		
		int lookupRight=-1;
		if(slopeCross!=maxBucket&&(minRadBucket<=maxBucket||needMoreLookups(maxBucket+lookupWidth,buckets.size()-1,maxValue))) {
			while(buckets.size()<=maxBucket+lookupWidth) {
				buckets.add(new TopicRadiusBucket());
			}
			lookupRight=chooseLookupBucket(maxBucket,maxBucket+lookupWidth-1);
		}
		
		if(lookupLeft==-1) {
			radiusLookup=lookupRight;
		} else {
			if(lookupRight==-1) {
				radiusLookup=lookupLeft;
			} else {
				if(randUint(2)==0) {
					radiusLookup = lookupLeft;
				} else {
					radiusLookup = lookupRight;
				}
			}
		}
		
		/*if(radiusLookup==-1) {
			converged=true;
			int rad = maxBucket;
			if(minRadBucket<rad) {
				rad=minRadBucket;
			}
			radius=BigInteger.valueOf(0);
			if(rad>0) {
				radius=BigDecimal.valueOf(Math.pow(2,64-(double)rad/radiusBucketsPerBit)).toBigInteger();
			}
		}*/
		return radiusLookup;
	}
	
	//return lookupinfo for random target according to radius
	LookupInfo nextTarget(boolean forceRegular) {
		
		if(!forceRegular) {
			int radiusLookup = recalcRadius();
			if(radiusLookup!=-1) {
				BigInteger target = targetForBucket(radiusLookup);
				return new LookupInfo(target,topic,true);
			}
		}
		
		BigInteger radExt = radius.divide(BigInteger.valueOf(2));
		
		if(radExt.compareTo(maxRadius.subtract(radius))==1) {
			radExt=maxRadius.subtract(radius);
		}
		BigInteger rnd = newRandomMax(radius).add(newRandomMax(radExt.multiply(BigInteger.valueOf(2))));
		if(rnd.compareTo(radExt)==1) {
			rnd = rnd.subtract(radExt);
		} else {
			rnd = radExt.subtract(rnd);
		}
		BigInteger target = topicHashPrefix.xor(rnd);
		return new LookupInfo(target,topic,false);

	}
	
	//adjust radius when ticket received
	void adjustWithTicket(long time, BigInteger targetHash/*,Ticketref t*/) {
		int wait = 0;//TODO calculate regtime - isuetime from ticket
		double inside = (double)wait/targetWaitTime - 0.5;
		if(inside > 1) {
			inside = 1;
		}
		if(inside < 0) {
			inside = 0;
		}
		adjust(CommonState.getTime(),targetHash,targetHash,inside);
	}
	
	//adjust radius when ticket received
	void adjust(long time, BigInteger targetHash, BigInteger addrHash, double inside) {
		int bucket = getBucketIdx(addrHash);
		
		if(bucket>=buckets.size()) {
			return;
		}
		buckets.get(bucket).adjust(CommonState.getTime(),inside);
		buckets.get(bucket).deleteLookupSent(targetHash);
	}
	
	private BigInteger newRandomMax(BigInteger max) {
		Random rnd = new Random();
		return new BigInteger(64,rnd).mod(max);
		
	}
	
	private int randUint(int max) {
		if(max<2) {
			return 0;
		}
		Random rnd = new Random();
		return rnd.nextInt(max);

	}
	
	
	private BigInteger calculatePrefix(BigInteger value) {
		BigInteger prefix = topicHashPrefix.xor(value);
		Random rnd = new Random();
		BigInteger rand = new BigInteger(64,rnd);
		for(int i=0;i<8;i++) {
			rand.clearBit(i);
		}
		
		return prefix.xor(rand);
	}
		
}
