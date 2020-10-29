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
import peersim.kademlia.operations.LookupOperation;
import peersim.kademlia.operations.LookupTicketOperation;
import peersim.kademlia.operations.Operation;
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

    private static FileWriter msgWriter;
    private static FileWriter opWriter; 

    /** Prefix to be printed in output */
    private String prefix;

    private static KademliaProtocol kadProtocol;

	public KademliaObserver(String prefix) {
		this.prefix = prefix;
		try {
			msgWriter = new FileWriter("./logs/messages.csv");
			msgWriter.write("id,type,src,dst,topic,sent/received\n");
			opWriter = new FileWriter("./logs/operations.csv");
			//opWriter.write("id,type,src,dst,hops,malicious,discovered,discovered_list,topic\n");
            opWriter.write("id,type,src,dst,used_hops,returned_hops,malicious,discovered,discovered_list,topic\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void addTopicRegistration(String topic, BigInteger registrant) {
		 if(!registeredTopics.containsKey(topic)){
				//System.out.println("addTopicRegistration "+topic);

	            HashSet<BigInteger> set = new HashSet<BigInteger>();
	            set.add(registrant);
	            registeredTopics.put(topic, set);
		 }else{
	            registeredTopics.get(topic).add(registrant);
				//System.out.println("addTopicRegistration "+topic+" "+registeredTopics.get(topic).size());

	     }
		 
	}
	
	
	public static int topicRegistrationCount(String topic) {
		if(registeredTopics.containsKey(topic)){
			return registeredTopics.get(topic).size();
		}
		return 0;
	}
	
	 public static void reportOperation(Operation op) {
	        try {
	            //System.out.println("Report operation "+op.getClass().getSimpleName());
	            String result = "";     
	            String type = "";

	            if (op instanceof LookupOperation || op instanceof LookupTicketOperation) {
	                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.returned.size() + "," +op.used.size()+ ","+((LookupOperation) op).maliciousDiscoveredCount()   + "," + ((LookupOperation)op).discoveredCount() +","+ ((LookupOperation)op).discoveredToString() + "," + ((LookupOperation)op).topic.topic+ "\n";
	            //else
	            //    result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.returned.size() + "\n";
	                opWriter.write(result);
	                opWriter.flush();
	            }

	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        
	}
	
	public static void reportMsg(Message m, boolean sent) {
        if (kadProtocol instanceof Discv5ProposalProtocol) {
            try {
                String result = "";
                if(m.src == null) return; //ignore init messages
                
                result += m.id + "," + m.messageTypetoString() +"," + m.src.getId() + "," + m.dest.getId() + ",";
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
                result += m.id + "," + m.messageTypetoString() +"," + m.src.getId() + "," + m.dest.getId() + ",";
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

    private boolean is_eclipsed(KademliaNode node) {
        if (node.is_evil)
            //Don't include malicious nodes in the count
            return false;

        if (node.getOutgoingConnections().size() == 0 && node.getIncomingConnections().size() == 0)
            return false;

        for (KademliaNode outConn : node.getOutgoingConnections())
            if (!outConn.is_evil)
                return false;

        for (KademliaNode inConn : node.getIncomingConnections())
            if (!inConn.is_evil)
                return false;
        
        return true;
    }
    /**
     * print the statistical snapshot of the current situation
     * 
     * @return boolean always false
     */
    public boolean execute() {
        try {
            FileWriter writer = new FileWriter("./logs/" + CommonState.getTime() +  "_registrations.csv");
            writer.write("host,topic,registrant\n");
            for(int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();

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

        int num_eclipsed_nodes = 0;
        try {
            String filename = "./logs/eclipse_counts.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                writer.write("time,numberOfNodes\n");
            }
            else {
                writer = new FileWriter(myFile, true);
            }

            for(int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();

                if (is_eclipsed(kadProtocol.getNode()))
                    num_eclipsed_nodes += 1;
            }
            writer.write(CommonState.getTime() + "," + String.valueOf(num_eclipsed_nodes) + "\n");
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        return false;
    }
}
