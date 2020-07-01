package peersim.kademlia;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.math.BigInteger;


public class TopicRadiusBucket {
	
	public static final int trOutside=0;
	public static final int trInside=1;
	public static final int trNoAdjust=2;
	public static final int trCount=3;
	private static final int radiusTC=20*60; //time.Minute * 20
	private static final int respTimeout = 500; //* time.Millisecond


	private double []weights;
	private long lastTime;
	private double value;
	private TreeMap<BigInteger, Long> lookupSent;
	
	public TopicRadiusBucket(){
		weights = new double[3];
		lookupSent = new TreeMap<BigInteger,Long>();	
	}
	
	public void adjust(long now,double inside) {
		update(now);
		if(inside<=0) {
			weights[trOutside]+=1;
		}else {
			if(inside >= 1) {
				weights[trInside] += 1;
			} else {
				weights[trInside] += inside;
				weights[trOutside]+= 1 - inside;
			}
		}
	}
	
	private void update(long now) {
		if(now == lastTime) 
			return;
		
		float exp = -(now-lastTime)/(float)radiusTC;
		
		for (int i=0;i<trCount;i++)
		{
			weights[i] = weights[i] * exp;
		}

		lastTime = now;
		
        for(Entry<BigInteger, Long> entry : lookupSent.entrySet()){
        	if(now-entry.getValue()>respTimeout) {
				weights[trNoAdjust] += 1;
				lookupSent.remove(entry.getKey());
			}
        }

	}
	public void setValue(double value) {
		this.value = value;
	}
	
	public double getValue(){
		return value;
	}
	
	public double[] getWeights(){
		return weights;
	}
	
	public void deleteLookupSent(BigInteger hash){
		lookupSent.remove(hash);
	}
	
}
