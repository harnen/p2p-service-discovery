package peersim.kademlia;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.Arrays;
import com.google.common.math.BigIntegerMath; 

/**
 * Some utility and mathematical function to work with BigInteger numbers and strings.
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class Util {

	/**
	 * Given two numbers, returns the length of the common prefix, i.e. how many digits (in base 2) have in common from the
	 * leftmost side of the number
	 * 
	 * @param b1
	 *            BigInteger
	 * @param b2
	 *            BigInteger
	 * @return int
	 */
	public static final int prefixLen(BigInteger b1, BigInteger b2) {

		String s1 = Util.put0(b1);
		String s2 = Util.put0(b2);

		int i = 0;
		for (i = 0; i < s1.length(); i++) {
			if (s1.charAt(i) != s2.charAt(i))
				return i;
		}

		return i;
	}

	/**
	 * return the distance between two number which is defined as (a XOR b)
	 * 
	 * @param a
	 *            BigInteger
	 * @param b
	 *            BigInteger
	 * @return BigInteger
	 */
	public static final BigInteger distance(BigInteger a, BigInteger b) {
		return a.xor(b);
	}

	
	/**
	 * return the distance between two number wich is defined as (a XOR b)
	 * 
	 * @param a
	 *            BigInteger
	 * @param b
	 *            BigInteger
	 * @return BigInteger
	 */
	public static final double logdistance(BigInteger a, BigInteger b) {
		return Math.log10((double)a.xor(b).doubleValue());
	}
	
	/**
	 * convert a BigInteger into a String (base 2) and lead all needed non-significative zeroes in order to reach the canonical
	 * length of a nodeid
	 * 
	 * @param b
	 *            BigInteger
	 * @return String
	 */
	public static final String put0(BigInteger b) {
		if (b == null)
			return null;
		String s = b.toString(2); // base 2
		while (s.length() < KademliaCommonConfig.BITS) {
			s = "0" + s;
		}
		return s;
	}
	
	public static double log2(BigInteger x)
	{
		double l = Math.log10(x.divide(BigInteger.valueOf(1000000)).doubleValue())+6;
		double n = Math.log10(x.doubleValue())/Math.log10(2);
		System.out.println("Log "+l+" "+n);
		//return (Math.log10(x.divide(BigInteger.valueOf(1000000)).doubleValue())+6)/Math.log10(2);
		return n;
	}
	
	public static BigInteger prefix(BigInteger address) {
		
		String prefix = address.toString(16).substring(0, 16);
	
		return new BigInteger(prefix,16);
	}

	

}
