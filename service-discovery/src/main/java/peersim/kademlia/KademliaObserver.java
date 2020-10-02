package peersim.kademlia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.math.BigInteger;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
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
	private static IncrementalStats msg_deliv = new IncrementalStats();

	/**
	 * keep statistic of number of message sent
	 */
	public static IncrementalStats msg_sent = new IncrementalStats();

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
	public static IncrementalStats lookup_total = new IncrementalStats();

	/**
	 * keep statistic of number of register operation
	 */
	public static IncrementalStats register_ok = new IncrementalStats();
	
	
	public static HashMap<String, Set<BigInteger>> registeredTopics = new HashMap<String, Set<BigInteger>>();
	
	private static HashMap<BigInteger, Integer> nodeMsgReceived = new HashMap<BigInteger, Integer>();
	
	private static HashMap<BigInteger, Integer> nodeTopicStored = new HashMap<BigInteger, Integer>();

	/** Parameter of the protocol we want to observe */
	private static final String PAR_PROT = "protocol";
	
	private static FileWriter msgWriter; 

	/** Protocol id */
	private int pid;

	/** Prefix to be printed in output */
	private String prefix;

    private static KademliaProtocol kadProtocol;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
		try {
			msgWriter = new FileWriter("messages.csv");
			msgWriter.write("id,type,src,dst,topic,sent/received\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void addTopicRegistration(String topic, BigInteger registrant) {
		 if(!registeredTopics.containsKey(topic)){
	            HashSet<BigInteger> set = new HashSet<BigInteger>();
	            set.add(registrant);
	            registeredTopics.put(topic, set);
		 }else{
	            registeredTopics.get(topic).add(registrant);
	     }
		 
	}
	
	public static int topicRegistrationCount(String topic) {
		if(registeredTopics.containsKey(topic)){
			return registeredTopics.get(topic).size();
		}
		return 0;
	}
	
	public static void registerMsgReceived(BigInteger id, Message m) {
		if(!nodeMsgReceived.containsKey(id)) {
			nodeMsgReceived.put(id, 1);
		}else {
			nodeMsgReceived.put(id, nodeMsgReceived.get(id) + 1);
		}
		msg_deliv.add(1);
	}
	
	public static void reportMsg(Message m, boolean sent) {

        if (kadProtocol instanceof Discv5ProposalProtocol) {
		    try {
			    String result = "";
    			if(m.src == null) return; //ignore init messages
	    		result += m.id + "," + m.getType() +"," + m.src.getId() + "," + m.dest.getId() + ",";
		    	if(m.getType() == Message.MSG_REGISTER ||
			       m.getType() == Message.MSG_TOPIC_QUERY) {
				    result += ((Topic) m.body).topic +"," ;
    			}else {
	    			result += ",";
    			}
    			if(sent) {
	    			result += "sent\n";
		    	}
			    else {
				    result += "received\n";
    			}
	    		msgWriter.write(result);
		    	msgWriter.flush();
    		} catch (IOException e) {
	    		// TODO Auto-generated catch block
		    	e.printStackTrace();
		    }
		
	    }else if (kadProtocol instanceof Discv5TicketProtocol) {
		    try {
			    String result = "";
    			if(m.src == null) return; //ignore init messages
	    		result += m.id + "," + m.getType() +"," + m.src.getId() + "," + m.dest.getId() + ",";
		    	if(m.getType() == Message.MSG_TOPIC_QUERY) {
				    result += ((Topic) m.body).topic +"," ;
                }
                else if(m.getType() == Message.MSG_REGISTER) {
                    result += ((Ticket) m.body).getTopic() + ",";
    			}else {
	    			result += ",";
    			}
    			if(sent) {
	    			result += "sent\n";
		    	}
			    else {
				    result += "received\n";
    			}
	    		msgWriter.write(result);
		    	msgWriter.flush();
    		} catch (IOException e) {
	    		// TODO Auto-generated catch block
		    	e.printStackTrace();
		    }

        }
	
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
		String s = String.format("[time=%d]:[N=%d current nodes UP] [%d/%d find (succ/all)] [%d/%d query (succ/all))] [%d/%d msg(recv/sent)]", 
								CommonState.getTime(), 
								sz, 
								(int) find_ok.getSum(), 
								(int) find_total.getSum(), 
								(int) register_ok.getSum(), 
								(int) register_total.getSum(),
								(int) msg_deliv.getSum(),
								(int) msg_sent.getSum());
								//msg_deliv.getSum(), hopStore.getMin(), hopStore.getAverage(), hopStore.getMax(), (int) timeStore.getMin(), (int) timeStore.getAverage(), (int) timeStore.getMax());
		System.err.println(s);
		
		try {
			FileWriter writer = new FileWriter(CommonState.getTime() +  "_stats.py");
			boolean first = true;
			writer.write("topics = [");
			for(String topic: registeredTopics.keySet()) {
				if(first){
					writer.write("\'" + topic + "\'" + ": " + registeredTopics.get(topic).size());
					first = false;
				}else {
					writer.write(", \'" + topic + "\'" + ": " + registeredTopics.get(topic).size());
				}
				
			}
			writer.write("]\n");
			//////////////////////////////////////////////////////////////////////////////////
			first=true;
			writer.write("msgReceived = [");
			for(BigInteger node: nodeMsgReceived.keySet()) {
				if(first) {
					writer.write("\'" + node + "\'" + ": " + nodeMsgReceived.get(node));
					first = false;
				}else {
					writer.write(", \'" + node + "\'" + ": " + nodeMsgReceived.get(node));
				}
			}
			writer.write("]\n");
			
			first=true;
			writer.write("nodeTopicStored = [");
			for(BigInteger node: nodeTopicStored.keySet()) {
				if(first) {
					writer.write("\'" + node + "\'" + ": " + nodeTopicStored.get(node));
					first = false;
				}else {
					writer.write(", \'" + node + "\'" + ": " + nodeTopicStored.get(node));
				}
			}
			writer.write("]\n");
			writer.close();
			
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			FileWriter writer = new FileWriter(CommonState.getTime() +  "_registrations.csv");
			writer.write("host,topic,registrant\n");
			for(int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				kadProtocol = (KademliaProtocol)node.getProtocol(pid);
				if(kadProtocol instanceof Discv5ProposalProtocol) {
					String registrations = ((Discv5ProposalProtocol) kadProtocol).topicTable.dumpRegistrations();
					writer.write(registrations);
				}
				if(kadProtocol instanceof Discv5TicketProtocol) {
					String registrations = ((Discv5TicketProtocol) kadProtocol).topicTable.dumpRegistrations();
					writer.write(registrations);
				}
			}
			writer.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return false;
	}
}
