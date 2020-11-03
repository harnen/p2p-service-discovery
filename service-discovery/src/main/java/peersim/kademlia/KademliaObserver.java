package peersim.kademlia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
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
import peersim.kademlia.operations.RegisterOperation;
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
    
    public static TreeMap<String, Integer> activeRegistrations = new TreeMap<String, Integer>();

    public static TreeMap<String, Integer> activeRegistrationsByMalicious = new TreeMap<String, Integer>();
    
    private static HashMap<BigInteger, Integer> nodeMsgReceived = new HashMap<BigInteger, Integer>();
    
    private static HashMap<BigInteger, Integer> nodeTopicStored = new HashMap<BigInteger, Integer>();
    
    private static HashMap<BigInteger, BigInteger> nodeInfo = new HashMap<BigInteger, BigInteger>();
    private static HashSet<BigInteger> writtenNodeIDs = new HashSet<BigInteger>();

    private static FileWriter msgWriter;
    private static FileWriter opWriter; 
    
    private static HashMap<Topic,Integer> regByTopic;
    private static HashMap<BigInteger,Integer> regByRegistrant;
    private static HashMap<BigInteger,Integer> regByRegistrar;

    private static int avgCounter=0;

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
            regByTopic = new HashMap<Topic,Integer>();
            regByRegistrant = new HashMap<BigInteger,Integer>();
            regByRegistrar = new HashMap<BigInteger,Integer>();
          
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTopicRegistration(Topic t, BigInteger registrant) {
         String topic = t.getTopic();
         if(!registeredTopics.containsKey(topic)){
                //System.out.println("addTopicRegistration "+topic);

                HashSet<BigInteger> set = new HashSet<BigInteger>();
                set.add(registrant);
                registeredTopics.put(topic, set);
         }else{
                registeredTopics.get(topic).add(registrant);
                //System.out.println("addTopicRegistration "+topic+" "+registeredTopics.get(topic).size());

         }

        if(!nodeInfo.containsKey(registrant)) 
        {
            nodeInfo.put(registrant, t.getTopicID());
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
                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount()+ ","+((LookupOperation) op).maliciousDiscoveredCount()   + "," + ((LookupOperation)op).discoveredCount() +","+ ((LookupOperation)op).discoveredToString() + "," + ((LookupOperation)op).topic.topic+ "\n";
            } else if (op instanceof RegisterOperation) {
                result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount() + "," + ","  + "," + "," + ((RegisterOperation)op).topic.topic+ "\n";
            } else {
            	;//result += op.operationId + "," + op.getClass().getSimpleName() + ","  + op.srcNode +"," + op.destNode + "," + op.getUsedCount() + "," +op.getReturnedCount() + "," + ","  + "," + "," + "\n";
            }
            opWriter.write(result);
            opWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
 
	
	 /*public static void reportOperation(Operation op) {
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
	        
	}*/
	public static void reportActiveRegistration(Topic t, boolean is_evil) {
        if (is_evil) {
            if (!activeRegistrationsByMalicious.containsKey(t.getTopic())) {
                activeRegistrationsByMalicious.put(t.getTopic(), new Integer(1));
            } 
            else {
                Integer num = activeRegistrationsByMalicious.get(t.getTopic());
                num += 1;
                activeRegistrationsByMalicious.put(t.getTopic(), num);
            }
        }
        else {
            if (!activeRegistrations.containsKey(t.getTopic())) {
                activeRegistrations.put(t.getTopic(), new Integer(1));
            } 
            else {
                Integer num = activeRegistrations.get(t.getTopic());
                num += 1;
                activeRegistrations.put(t.getTopic(), num);
            }
        }
    }

    public static void reportExpiredRegistration(Topic t, boolean is_evil) {
        if (is_evil) {
            Integer num = activeRegistrationsByMalicious.get(t.getTopic());
            num -= 1;
            activeRegistrationsByMalicious.put(t.getTopic(), num);
        }
        else {
            Integer num = activeRegistrations.get(t.getTopic());
            num -= 1;
            activeRegistrations.put(t.getTopic(), num);
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

    private void write_registration_stats() {
        if (activeRegistrations.size() == 0)
            return;
        try {
            String filename = "./logs/registration_stats.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                String title = "time";
                for (String topic : activeRegistrations.keySet()) {
                    title += "," + topic + "-normal";
                    title += "," + topic + "-evil";
                }
                title += "\n";
                writer.write(title);
            }
            else {
                writer = new FileWriter(myFile, true);
            }
            writer.write("" + CommonState.getTime());
            for (String topic : activeRegistrations.keySet()) {
                writer.write("," + activeRegistrations.get(topic));
                Integer maliciousRegistrants = activeRegistrationsByMalicious.get(topic);
                if (maliciousRegistrants == null) 
                    writer.write(",0");
                else
                {
                    writer.write("," + activeRegistrationsByMalicious.get(topic));
                    //activeRegistrationsByMalicious.put(topic, 0);
                }
                //activeRegistrations.put(topic, 0);
            }
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write_node_info() {
        try {
            String filename = "./logs/node_information.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                if (nodeInfo.size() < Network.size())
                    return;
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                writer.write("nodeID,topicID,is_evil?\n");
            }
            else {
                
                writer = new FileWriter(myFile, true);
            }

            for(int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();
                BigInteger id = kadProtocol.getNode().getId();
                if (writtenNodeIDs.contains(id))
                    continue;
                int is_evil = kadProtocol.getNode().is_evil ? 1 : 0; 
                writer.write(id + "," + nodeInfo.get(id) + "," + is_evil + "\n");
                writtenNodeIDs.add(id);
            }
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
                

    private void write_registered_topics_average() {
    	try {
	        String filename = "./logs/registeredTopics.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("topic,count\n");
	        for(Topic t : regByTopic.keySet()) {
	        	//System.out.println("Topic "+t.getTopic()+" "+regByTopic.get(t)/avgCounter);
	        	writer.write(t.topic);
	        	writer.write(",");
	        	writer.write(String.valueOf(regByTopic.get(t).intValue()/avgCounter));
	        	writer.write("\n");
	        }
	    	writer.close();

    	}catch(IOException e) {
    		//e.printStackTrace();
    	}

    }
    
    private void write_registered_registrar_average() {
    	try {
	        String filename = "./logs/registeredRegistrar.csv";
	        File myFile = new File(filename);
	        FileWriter writer;
	        if (myFile.exists())myFile.delete();
	        myFile.createNewFile();
	        writer = new FileWriter(myFile, true);
	        writer.write("nodeId,count\n");

	        for(BigInteger t : regByRegistrar.keySet()) {
	        	//System.out.println("Re "+t.getTopic()+" "+regByRegistrar.get(t)/avgCounter);
	        	writer.write(String.valueOf(t));
	        	writer.write(",");
	        	writer.write(String.valueOf(regByRegistrar.get(t).intValue()/avgCounter));
	        	writer.write("\n");
	        }
	    	writer.close();

    	}catch(IOException e) {
    		//e.printStackTrace();
    	}

    }

    private void write_eclipsing_results() {

        int num_eclipsed_nodes = 0;
        HashSet<BigInteger> eclipsed_nodes = new HashSet<BigInteger>();
        HashSet<BigInteger> uneclipsed_nodes = new HashSet<BigInteger>();
        HashSet<BigInteger> evil_nodes = new HashSet<BigInteger>();
        try {
            String filename = "./logs/eclipse_counts.csv";
            File myFile = new File(filename);
            FileWriter writer;
            if (!myFile.exists()) {
                myFile.createNewFile();
                writer = new FileWriter(myFile, true);
                writer.write("time,numberOfNodes,eclipsedNodes,UnEclipsedNodes,EvilNodes\n");
            }
            else {
                writer = new FileWriter(myFile, true);
            }

            for(int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();

                if (kadProtocol.getNode().is_evil) {
                    evil_nodes.add(kadProtocol.getNode().getId());
                }
                else {
                    if (is_eclipsed(kadProtocol.getNode())) {
                        eclipsed_nodes.add(kadProtocol.getNode().getId());
                        num_eclipsed_nodes += 1;
                    }
                    else {
                        uneclipsed_nodes.add(kadProtocol.getNode().getId());
                    }
                }
            }
            writer.write(CommonState.getTime() + "," + String.valueOf(num_eclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(eclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(uneclipsed_nodes));
            writer.write("," + Util.bigIntegetSetToString(evil_nodes) + "\n");
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private boolean is_eclipsed(KademliaNode node) {
        if (node.is_evil)
            //Don't include malicious nodes in the count
            return false;

        if (node.getOutgoingConnections().size() == 0)
            return false;

        for (KademliaNode outConn : node.getOutgoingConnections())
            if (!outConn.is_evil)
                return false;
        /*
        for (KademliaNode inConn : node.getIncomingConnections())
            if (!inConn.is_evil)
                return false;
        */

        return true;
    }
    /**
     * print the statistical snapshot of the current situation
     * 
     * @return boolean always false
     */
    public boolean execute() {
    	avgCounter++;
        try {
            FileWriter writer = new FileWriter("./logs/" + CommonState.getTime() +  "_registrations.csv");
            writer.write("host,topic,registrant\n");
            
            
            for(int i = 0; i < Network.size(); i++) {
            	
                Node node = Network.get(i);
                kadProtocol = node.getKademliaProtocol();
                
                HashMap<Topic,Integer> topics = new HashMap<Topic,Integer>();
                
                if(kadProtocol instanceof Discv5ProposalProtocol) {
                	topics = ((Discv5ProposalProtocol) kadProtocol).topicTable.getRegbyTopic();
                } else if(kadProtocol instanceof Discv5TicketProtocol) {
                	topics = ((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyTopic();
                }
                
                for(Topic t: topics.keySet()) {
            		int count = 0;
            		if(regByTopic.get(t)!=null)count=regByTopic.get(t);
            		count+=topics.get(t);
            		regByTopic.put(t, count);
                }
                
                if(kadProtocol instanceof Discv5ProposalProtocol) {
                    String registrations = ((Discv5ProposalProtocol) kadProtocol).topicTable.dumpRegistrations();
                    writer.write(registrations);
                    int count=0;
                    if(regByRegistrar.get(kadProtocol.getNode().getId())!=null) {
                    	count = regByRegistrar.get(kadProtocol.getNode().getId());
                    	count+=((Discv5ProposalProtocol) kadProtocol).topicTable.getRegbyRegistrar();
                    }
                    regByRegistrar.put(kadProtocol.getNode().getId(), count);
                }
                if(kadProtocol instanceof Discv5TicketProtocol) {
                    String registrations = ((Discv5TicketProtocol) kadProtocol).topicTable.dumpRegistrations();
                    writer.write(registrations);
                    int count=0;
                    if(regByRegistrar.get(kadProtocol.getNode().getId())!=null) {
                    	count = regByRegistrar.get(kadProtocol.getNode().getId());
                    	count+=((Discv5TicketProtocol) kadProtocol).topicTable.getRegbyRegistrar();
                    }
                    regByRegistrar.put(kadProtocol.getNode().getId(), count);
                }
                /*for(Topic t : regByTopic.keySet())
                {
                	System.out.println("Topic "+t.getTopic()+" "+regByTopic.get(t)/avgCounter);
                }*/

            }
            writer.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        write_registered_topics_average();
        write_registered_registrar_average();
        write_eclipsing_results();
        write_registration_stats();
        write_node_info();

        return false;
    }

}
