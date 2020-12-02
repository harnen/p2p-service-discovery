package peersim.kademlia;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import java.util.Random;

import peersim.core.CommonState;

public class TopicRadius {
	
	private static final int radiusBucketsPerBit = 8;
	private static final int minPeakSize = 40;
	private static final double maxNoAdjust = 20;
	public static final long targetWaitTime = 10*60*1000;//TODO define waiting time in cycles
	public static final BigInteger maxRadius = new BigInteger("ffffffffffffffff",16);
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
		topicHashPrefix = Util.prefix(t.topicID);
		minRadius=radius=maxRadius;
		buckets = new ArrayList<TopicRadiusBucket>();
	}

	public int getBucketIdx(BigInteger address){
		//return 0;
		BigInteger prefix = Util.prefix(address);
		//BigInteger prefix = BigInteger.valueOf(address.longValue());
		//String prefix = 
		double log2=0.0;
		if(prefix!=topicHashPrefix) {
			//System.out.println("getBucketIdx not hash prefix hashprefix "+topicHashPrefix+" xor:"+prefix.xor(topicHashPrefix)+" "+prefix.xor(topicHashPrefix).longValue());
			log2 = Util.log2(prefix.xor(topicHashPrefix));
		}
		int bucket = (int)((64 - log2)* radiusBucketsPerBit);
		int max = 64*radiusBucketsPerBit - 1;
		//System.out.println("getBucketIdx addr:"+address+" prefix:"+prefix+ " log2:"+log2+" bucket:"+bucket);

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

		double min = Math.pow(2,64-(double)(bucket+1)/radiusBucketsPerBit);
		double max = Math.pow(2, 64-(double)bucket/radiusBucketsPerBit);
		BigInteger a = BigDecimal.valueOf(min).toBigInteger();
		//System.out.println("Min "+min+" max "+max);
		BigInteger b = newRandomMax(BigDecimal.valueOf(max-min).toBigInteger());
		//System.out.println("Min "+min+" max "+max+" a "+a+" b "+b);
		BigInteger xor = a.add(b);
		
		if(xor.compareTo(a)==-1)
			xor= new BigInteger("FFFFFFFFFFFFFFFF",16);
		
		return calculatePrefix(xor);

	}
	
	//choose bucketleft or right
	int chooseLookupBucket(int a, int b) throws Exception {

		if(a<0) {
			a=0;
		}
		if(a>b) {
			return -1;
		}
		int c =0;
		for(int i=a;i<=b;i++) {
			if((i>=buckets.size())||(buckets.get(i).getWeight(TopicRadiusBucket.trNoAdjust) < maxNoAdjust)) {
				c++;
			}
		}
		if(c==0) {
			return -1;
		}
		int rnd = CommonState.r.nextInt(c);
		for(int i=a;i<=b;i++) {
			if((i>=buckets.size())||(buckets.get(i).getWeight(TopicRadiusBucket.trNoAdjust) < maxNoAdjust)) {
				if(rnd==0) {
					return i;
				}
				rnd--;
			}
		}
		throw new Exception("Exception message");
		
	}
	
	BigInteger getTopicHashPrefix()
	{
		return topicHashPrefix;
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
		int radiusLookup=-1;
		
		for(int i=0;i<buckets.size();i++) {
			buckets.get(i).update(now);
			v+=buckets.get(i).getWeight(TopicRadiusBucket.trOutside)-buckets.get(i).getWeight(TopicRadiusBucket.trInside);
			//System.out.println("Ticketstore recalcRadius: v:"+v+" bucketoutside:"+buckets.get(i).getWeight(TopicRadiusBucket.trOutside)+" bucketinside:"+buckets.get(i).getWeight(TopicRadiusBucket.trInside)+" value:"+buckets.get(i).getValue());	

			buckets.get(i).setValue(v);

		}
		int slopeCross=-1;
		
		for(int i=0;i<buckets.size();i++) {
			v=buckets.get(i).getValue();
			//System.out.println("Ticketstore recalcRadius: v:"+v+" i:"+i+" maxValue:"+maxValue+" slopeCross: "+slopeCross);
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
		
		try {
			int lookupLeft=-1;
			if(needMoreLookups(0,maxBucket-lookupWidth-1,maxValue)) {
				lookupLeft=chooseLookupBucket(maxBucket-lookupWidth,maxBucket-1);
			}
			
			int lookupRight=-1;
			//System.out.println("Ticketstore recalcRadius: slopeCross:"+slopeCross+" maxBucket:"+maxBucket+" minRadBucket "+minRadBucket+" needMoreLookups:"+needMoreLookups(maxBucket+lookupWidth,buckets.size()-1,maxValue));
	
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
			//System.out.println("Ticketstore recalcRadius: maxBucket:"+maxBucket+" slopeCross:"+slopeCross+" minRadBucket:"+minRadBucket+" lookupLeft:"+lookupLeft+" lookupRight:"+lookupRight+" maxValue:"+maxValue);

		}catch(Exception e) {};
		

		

		if(radiusLookup==-1) {
			converged=true;
			int rad = maxBucket;
			if(minRadBucket<rad) {
				rad=minRadBucket;
			}
			radius=BigInteger.valueOf(0);
			if(rad>0) {
				radius=BigDecimal.valueOf(Math.pow(2,64-(double)rad/radiusBucketsPerBit)).toBigInteger();
			}
		}
		//System.out.println("Ticketstore recalcRadius: Return radiusLookup:"+radiusLookup);

		return radiusLookup;
	}
	
	//return lookupinfo for random target according to the radius calculated
	LookupInfo nextTarget(boolean forceRegular) {
		
		if(!forceRegular) {
			int radiusLookup = recalcRadius();
			//System.out.println("Radiuslookup "+radiusLookup);
			if(radiusLookup!=-1) {
				BigInteger target = targetForBucket(radiusLookup);
				//System.out.println("target "+target);
				return new LookupInfo(target,topic,true);
			}
		}
		
		BigInteger radExt = radius.divide(BigInteger.valueOf(2));
		//System.out.println("radExt "+radExt);

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
	
	//adjust radius when ticket received based on waiting time and target waiting time
	void adjustWithTicket(long time, BigInteger targetHash,long regTime, long issueTime/*,Ticketref t*/) {
		long wait = regTime-issueTime;//TODO calculate regtime - isuetime from ticket
        //System.out.println("adjustWithTicket wait time: " + wait);
	
		double inside = (double)wait/targetWaitTime - 0.5;
		//System.out.println("RegTime:"+regTime+" issueTime:"+issueTime+" wait:"+wait+" inside:"+inside);
		if(inside > 1) {
			inside = 1;
		}
		if(inside < 0) {
			inside = 0;
		}
		adjust(time,targetHash,targetHash,inside);
	}
	
	//adjust radius when ticket received
	void adjust(long time, BigInteger targetHash, BigInteger addrHash, double inside) {
		int bucket = getBucketIdx(addrHash);
		//System.out.println("Ticketstore adjust: adjust "+targetHash+" addr "+addrHash+" bucket "+bucket+" nbuckets "+buckets.size()+" inside "+inside);

		if(bucket>=buckets.size()) {
			return;
		}
		buckets.get(bucket).adjust(time,inside);
		buckets.get(bucket).deleteLookupSent(targetHash);
	}
	
	private BigInteger newRandomMax(BigInteger max) {
		if (max.compareTo(BigInteger.valueOf(2))==-1) {
			return BigInteger.valueOf(0);
		}
		//Random rnd = new Random();
		//System.out.println("Random "+max);
		return new BigInteger(64,CommonState.r).mod(max);
		
	}
	
	private int randUint(int max) {
		if(max<2) {
			return 0;
		}
		//Random rnd = new Random();
		return CommonState.r.nextInt(max);

	}
	
	
	private BigInteger calculatePrefix(BigInteger value) {
		BigInteger prefix = topicHashPrefix.xor(value);
		//System.out.println("Prefix "+prefix+" "+topicHashPrefix+" "+value);
		UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
		String rand = urg.generate().toString(16);
		//System.out.println("Rand1 "+rand.toString(16));
		//System.out.println("Rand "+prefix.toString(16).concat(rand.substring(16,rand.length())));
		
		return new BigInteger(prefix.toString(16).concat(rand.substring(16,rand.length())),16);
	}
		
}
