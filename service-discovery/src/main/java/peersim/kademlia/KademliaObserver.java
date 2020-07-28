package peersim.kademlia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.util.IncrementalStats;

/**
 * This class implements a simple observer of search time and hop average in finding a node in the network
 * 
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class KademliaObserver implements Control {

	/**
	 * keep statistics of the number of hops of every message delivered.
	 */
	public static IncrementalStats hopStore = new IncrementalStats();

	/**
	 * keep statistics of the time every message delivered.
	 */
	public static IncrementalStats timeStore = new IncrementalStats();

	/**
	 * keep statistic of number of message delivered
	 */
	public static IncrementalStats msg_deliv = new IncrementalStats();

	/**
	 * keep statistic of number of find operation
	 */
	public static IncrementalStats find_total = new IncrementalStats();

	/**
	 * Successfull find operations
	 */
	public static IncrementalStats find_ok = new IncrementalStats();

	/**
	 * keep statistic of number of register operation
	 */
	public static IncrementalStats register_total = new IncrementalStats();

	/**
	 * keep statistic of number of register operation
	 */
	public static IncrementalStats register_ok = new IncrementalStats();

	/** Parameter of the protocol we want to observe */
	private static final String PAR_PROT = "protocol";

	/** Protocol id */
	private int pid;

	/** Prefix to be printed in output */
	private String prefix;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/**
	 * print the statistical snapshot of the current situation
	 * 
	 * @return boolean always false
	 */
	public boolean execute() {
		// get the real network size
		int sz = Network.size();
		for (int i = 0; i < Network.size(); i++)
			if (!Network.get(i).isUp())
				sz--;
		//[%d/%d  successful find operation] [D=%f msg deliv] [%f min h] [%f average h] [%f max h] [%d min l] [%d msec average l] [%d max l]
		String s = String.format("[time=%d]:[N=%d current nodes UP] [%d/%d find op] [%d/%d register operation]", 
								CommonState.getTime(), 
								sz, 
								(int) find_ok.getSum(), 
								(int) find_total.getSum(), 
								(int) register_ok.getSum(), 
								(int) register_total.getSum());
								//msg_deliv.getSum(), hopStore.getMin(), hopStore.getAverage(), hopStore.getMax(), (int) timeStore.getMin(), (int) timeStore.getAverage(), (int) timeStore.getMax());
		System.err.println(s);

		return false;
	}
}
