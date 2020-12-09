package peersim.kademlia;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class RegistrationLog {

	private BigInteger registrant;
	private Map<BigInteger,Long> registered;
	private Map<BigInteger,Long> registeredWaiting;

	private long registrationInitTime;
	private Map<BigInteger,Long> discovered;
	
    public static final long   MAX_VALUE = 0x7fffffffffffffffL;

	public RegistrationLog(BigInteger node, long currTime) {
		this.registrant = node;
		registered = new HashMap<BigInteger,Long>();
		discovered = new HashMap<BigInteger,Long>();
		registeredWaiting = new HashMap<BigInteger,Long>();
		this.registrationInitTime = currTime;
	}
	
	public BigInteger getRegistrant() {
		return registrant;
	}
	
	public void addRegistrar(BigInteger node, long currentTime, long waitingTime) {
		registered.put(node,currentTime);
		registeredWaiting.put(node,waitingTime);
	}
	
	public void addDiscovered(BigInteger node, long currentTime) {
		//System.out.println("Add discovered "+currentTime+" "+registered.get(node));
		if(registered.get(node)!=null)discovered.put(node,currentTime-registered.get(node));
		
	}
	
	public long getMinRegisterTime() {
		long regTime=MAX_VALUE;
		if(registeredWaiting.size()>0) {
			for(Long time : registeredWaiting.values())
				if (time.longValue()<regTime)regTime=time.longValue();
			return regTime;
		} else
		return 0;
	}
	
	public long getMinDiscoveryTime() {
		long discTime=MAX_VALUE;
		if(discovered.size()>0) {
			for(Long time : discovered.values())
				if (time.longValue()<discTime)discTime=time.longValue();
			return discTime;
		} else
			return 0;
	}
	
	public long getAvgRegisterTime() {
		long regTime=0;
		if(registeredWaiting.size()>0) {
			for(Long time : registeredWaiting.values())
				regTime+=time.longValue();
			return regTime/registeredWaiting.size();
		} else
			return 0;
	}
	
	public long getAvgDiscoveryTime() {
		long discTime=0;
		if(discovered.size()>0) {
			for(Long time : discovered.values())
				discTime+=time.longValue();
			return discTime/discovered.size();
		} else
			return 0;
	}
	
	public Map<BigInteger,Long> getRegistered(){
		return registered;
	}
	
	public Map<BigInteger,Long> getDiscovered(){
		return discovered;
	}
}
