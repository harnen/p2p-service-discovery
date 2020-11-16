package peersim.kademlia;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class RegistrationLog {

	private BigInteger registrant;
	private Map<BigInteger,Long> registered;
	private long registrationInitTime;
	private Map<BigInteger,Long> discovered;
	
    public static final long   MAX_VALUE = 0x7fffffffffffffffL;

	public RegistrationLog(BigInteger node, long currTime) {
		this.registrant = node;
		registered = new HashMap<BigInteger,Long>();
		discovered = new HashMap<BigInteger,Long>();
		this.registrationInitTime = currTime;
	}
	
	public BigInteger getRegistrant() {
		return registrant;
	}
	
	public void addRegistrar(BigInteger node, long currentTime) {
		registered.put(node,currentTime);
		
	}
	
	public void addDiscovered(BigInteger node, long currentTime) {
		discovered.put(node,currentTime);
		
	}
	
	public long getMinRegisterTime() {
		long regTime=MAX_VALUE;
		if(registered.size()>0) {
			for(Long time : registered.values())
				if (time.longValue()<regTime)regTime=time.longValue();
			return regTime - registrationInitTime;
		} else
		return 0;
	}
	
	public long getMinDiscoveryTime() {
		long discTime=MAX_VALUE;
		if(discovered.size()>0) {
			for(Long time : discovered.values())
				if (time.longValue()<discTime)discTime=time.longValue();
			return discTime - registrationInitTime;
		} else
			return 0;
	}
	
	public long getAvgRegisterTime() {
		long regTime=0;
		if(registered.size()>0) {
			for(Long time : registered.values())
				regTime+=time.longValue() - registrationInitTime;
			return regTime/registered.size();
		} else
			return 0;
	}
	
	public long getAvgDiscoveryTime() {
		long discTime=0;
		if(discovered.size()>0) {
			for(Long time : discovered.values())
				discTime+=time.longValue() - registrationInitTime;
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
