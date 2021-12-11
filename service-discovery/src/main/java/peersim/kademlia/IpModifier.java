package peersim.kademlia;

import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
//import inet.ipaddr.ipv4.IPv4AddressTrie;

public class IpModifier {
	
	
	private int[] scores;
	
    protected static final int baseMultiplier = 30;

	public IpModifier() {
		
		scores = new int[33];
		
	}
	
	
	public double getModifier() {
		
		int score = 0;
		
		for (int i = 1; i< scores.length;i++) {
			if(scores[i]>i)score+=scores[i]-i;
		}

		//-(self.root.getCounter()) * (1 - pow(2, 33))
		return score/-(scores[0]*(1-Math.pow(2,33)));
	}
	
	public void newAddress(String address) {
		
		IPAddressString addr = new IPAddressString(address);
		IPv4Address ipv4Addr = addr.getAddress().toIPv4();
		String binAddr = ipv4Addr.toBinaryString();
		
		scores[0]++;

		for (int i = 0; i < binAddr.length(); i++){
			if(binAddr.charAt(i)=='1') {
				scores[i+1]++; 
			}
		}
		
		/*for (int i = 0; i< scores.length;i++) {
			System.out.print("Score "+i+" "+scores[i]);
			System.out.println();
		}*/

		
		
	}
	
	
	public void removeAddress(String address) {
		IPAddressString addr = new IPAddressString(address);
		IPv4Address ipv4Addr = addr.getAddress().toIPv4();
		//trie.remove(ip4Addr);
		
		String binAddr = ipv4Addr.toBinaryString();
		
		scores[0]--;

		for (int i = 0; i < binAddr.length(); i++){
			if(binAddr.charAt(i)=='1') {
				scores[i+1]--; 
			}
		}
	}

}
