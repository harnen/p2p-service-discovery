package peersim.kademlia;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Collections;
import java.math.BigInteger;

import peersim.kademlia.Topic;
import peersim.kademlia.TopicRegistration;

public class Discv5TopicTable { // implements TopicTable {
    
    private int tableCapacity = KademliaCommonConfig.TOPIC_TABLE_CAP;
    private int adsPerQueue = KademliaCommonConfig.ADS_PER_QUEUE;
    private int adLifeTime = KademliaCommonConfig.AD_LIFE_TIME;
    private BigInteger hostID;

    // Per-topic registration table
    private HashMap<Topic, ArrayDeque<TopicRegistration>> topicTable;
    // Competing tickets for each topic 
    private HashMap<Topic, ArrayDeque<Ticket>> competingTickets;
    // All topic advertisements ordered by registration
    private ArrayDeque<TopicRegistration> allAds;

    private HashMap<Topic, Long> nextDecisionTime;

    private Logger logger;

    public Discv5TopicTable() {
        topicTable = new HashMap<Topic, ArrayDeque<TopicRegistration>>(); 
        competingTickets = new HashMap<Topic, ArrayDeque<Ticket>>();
        allAds = new ArrayDeque<TopicRegistration>();
        nextDecisionTime = new HashMap<Topic, Long>();
    }

    public void setHostID(BigInteger id){
        this.hostID = id;
		logger = Logger.getLogger(id.toString());

    }

    public void setAdLifeTime(int duration) {
        this.adLifeTime = duration;
    }

    public int getAdLifeTime() {
        return this.adLifeTime;
    }

    public int getCapacity() {
        return this.tableCapacity;
    }
    
    public void setCapacity(int capacity) {
        this.tableCapacity = capacity;
    }

    public void setAdsPerQueue(int qSize) {
        this.adsPerQueue = qSize;
    }

    private void updateTopicTable(long curr_time) {
		Iterator<TopicRegistration> it = allAds.iterator();
		while (it.hasNext()) {
    		TopicRegistration r = it.next();
        	if (curr_time - r.getTimestamp() >= this.adLifeTime) {
            	ArrayDeque<TopicRegistration> topicQ = topicTable.get(r.getTopic());
	            TopicRegistration r_same = topicQ.pop(); 
				it.remove(); //removes from allAds

        	    //TODO assert that r_same and r are the same registrations
			}
		}
    }

    private Ticket getBestTicket(Topic topic) {
        ArrayDeque<Ticket> ticketQ = competingTickets.get(topic);
        if (ticketQ == null) 
            return null;
        else if (ticketQ.size() > 0)
            return ticketQ.getFirst();
        else 
            return null;   
    }

    private void add_to_competingTickets (Topic topic, Ticket ticket) {
        ArrayDeque<Ticket> ticketQ = competingTickets.get(topic);
        if (ticketQ == null) {
            ArrayDeque<Ticket> newTicketQ = new ArrayDeque<Ticket>();
            newTicketQ.add(ticket);
            competingTickets.put(topic, newTicketQ);
        }
        else {
            if(!ticketQ.contains(ticket))
                ticketQ.add(ticket);
        }

    }

    private long getWaitingTime(TopicRegistration reg, long curr_time) {
        //System.out.println("Get Waiting time "+reg.getTopic().getTopic());

        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());
        long waiting_time;
        
        //System.out.println("Topic "+reg.getTopic().topic+" "+topicQ.size());
        /*if(topicQ!=null) {
        for(Iterator<TopicRegistration> itr = topicQ.iterator();itr.hasNext();)  {
        	TopicRegistration t = itr.next();
            System.out.println(t.getTopic().getTopic()+" "+t.getNode().getId());
         }
        }*/
        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //logger.warning("Ad already registered by this node");
            return -1;
        }
        if (topicQ != null)
            assert topicQ.size() <= this.adsPerQueue;

        // compute the waiting time
        if (topicQ != null && topicQ.size() == this.adsPerQueue) {
            TopicRegistration r = topicQ.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else if(allAds.size() == this.tableCapacity) {
            TopicRegistration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else {
            waiting_time = 0;
        }

        return waiting_time;
    }
    
    public boolean register_ticket(Ticket ticket, Message m, long curr_time) {
        Topic ti = ticket.getTopic();
        Topic topic = new Topic(ti.topic);
        //topic.setHostID(this.hostID);

        ticket.setMsg(m);
        add_to_competingTickets(topic, ticket);

        return setDecisionTime(ticket.getTopic(), curr_time + KademliaCommonConfig.ONE_UNIT_OF_TIME);
    }

    private void register(TopicRegistration reg) {
        ArrayDeque<TopicRegistration> topicQ = this.topicTable.get(reg.getTopic());
        if (topicQ != null) {
            topicQ.add(reg);
            //System.out.println(this +" Add topictable "+reg.getTopic().getTopic()+" "+topicQ.size());
        }else {
            ArrayDeque<TopicRegistration> q = new ArrayDeque<TopicRegistration>();
            q.add(reg);
            this.topicTable.put(reg.getTopic(), q);
        }

        this.allAds.add(reg);
    }
    
    public Ticket [] makeRegisterDecisionForTopic(Topic ti, long curr_time) {   
        nextDecisionTime.remove(ti);
        Topic topic = new Topic(ti.topic);
        //topic.setHostID(this.hostID);
        
        ArrayDeque<Ticket> ticketQ = competingTickets.get(topic);
        if (ticketQ == null) {
            /*
            System.out.println("This should not happen");
            System.out.println("Lookup topic: " + topic.toString());
            System.out.println("My HostID: " + this.hostID);
    	    String result = "Topic in TicketList: ";
    	    for(Topic t: competingTickets.keySet()) {
                result += t.toString();
                System.out.println("Comparison: " + topic.compareTo(t));
                System.out.println("Equality check: " + topic.equals(t));
            }
            System.out.println(result);
	        //System.exit(-1);
            */
            return new Ticket[0];
        }    
        if (ticketQ !=null && ticketQ.size() == 0) {
            return new Ticket[0];
        }

        Ticket bestTicket = ticketQ.getFirst();
        // list of tickets to respond with MSG_REGISTER_RESPONSE
        ArrayList<Ticket> responseList = new ArrayList<Ticket>();

        //Register as many tickets as possible (subject to resource availability)
        updateTopicTable(curr_time);
		Iterator<Ticket> it = ticketQ.iterator();
        while(it.hasNext()) {
            Ticket ticket = it.next();
            TopicRegistration reg = new TopicRegistration(ticket.getSrc(), topic, curr_time);
            reg.setTimestamp(curr_time);
            long waiting_time = getWaitingTime(reg, curr_time);

        	int topicOccupancy = 0;
        	if(this.topicTable.get(reg.getTopic())!=null)
                topicOccupancy = this.topicTable.get(reg.getTopic()).size();
            
            //assert waiting_time != -1;

            if (waiting_time == -1) {
                ticket.setRegistrationComplete(false);
                ticket.setWaitTime(waiting_time);
                responseList.add(ticket);
            }
            else if (waiting_time == 0 && topicOccupancy <adsPerQueue && this.allAds.size()<tableCapacity) {
                register(reg);
                ticket.setRegistrationComplete(true);
                //ticket.setWaitTime(waiting_time);
                it.remove();
                responseList.add(ticket);
                assert !ticketQ.contains(ticket);
            }
            else { //waiting_time > 0
                assert waiting_time > 0;
                waiting_time = (waiting_time - ticket.getRTT() > 0) ? waiting_time - ticket.getRTT() : 0;
                if (ticket.getReqTime() + ticket.getRTT() + ticket.getWaitTime() + KademliaCommonConfig.ONE_UNIT_OF_TIME  <= curr_time) {
                    responseList.add(ticket);
                }
                ticket.updateWaitingTime(waiting_time);
                ticket.setRegistrationComplete(false);
            }
        }

        Ticket [] tickets = (Ticket []) responseList.toArray(new Ticket[responseList.size()]);
        return tickets;
    }

    public Ticket getTicket(Topic t, KademliaNode advertiser, long rtt_delay, long curr_time) {
        Topic topic = new Topic(t.topic);
        //topic.setHostID(this.hostID);
        // System.out.println("Get ticket "+topic.getTopic());
        TopicRegistration reg = new TopicRegistration(advertiser, topic, curr_time);

        //update the topic table (remove expired advertisements)
        updateTopicTable(curr_time);

        //compute ticket waiting time
        long waiting_time = getWaitingTime(reg, curr_time);

        Ticket ticket = new Ticket (topic, curr_time, waiting_time, advertiser, rtt_delay);
        
        if (waiting_time == -1) {
            //already registered
            return ticket;
        }
        ArrayDeque<Ticket> ticketQ = competingTickets.get(topic);
        
        Ticket best_ticket = null;
        if (ticketQ != null && ticketQ.size() != 0)
            best_ticket = ticketQ.getFirst();

        if (best_ticket != null) {
            long next_register_time = best_ticket.getReqTime() + best_ticket.getCumWaitTime();
            waiting_time = (next_register_time - curr_time >= rtt_delay) ? next_register_time - curr_time - rtt_delay : 0;
            ticket.setWaitTime(waiting_time);

        }
        else { // no best ticket exists
            waiting_time = (waiting_time - rtt_delay > 0) ? waiting_time - rtt_delay : 0;
            ticket.setWaitTime(waiting_time);
        }
     
        return ticket;
    }

    // Returns true if there is no makeRegisterDecisionForTopic scheduled for the topic at decision time
    public boolean setDecisionTime(Topic topic, long decisionTime) {
        Long time = nextDecisionTime.get(topic);

        if (time == null) {
            nextDecisionTime.put(topic, new Long(decisionTime));
            return true;
        }
        else if (time > decisionTime)
        {
            //nextDecisionTime.put(topic, new Long(decisionTime));
            return true;
        }
        else if (time == decisionTime)
            return false;
        else 
        {
            return true;
        }

    }

    public TopicRegistration[] getRegistration(Topic t){
        Topic topic = new Topic(t.topic);
        //topic.setHostID(this.hostID);
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(topic);

        if (topicQ == null) {
            //TODO remove the check below: 
    	    for(Topic ti: topicTable.keySet()) {
                if (ti.getTopic() == topic.getTopic()) {
                    logger.warning("Error in topic table lookup !");
                    String result = "Unable to find identical topics: ";
                    result += topic.toString();
                    result += "\n";
                    result += ti.toString();
                    result += "\n";
                    //System.out.println(result);
                }
            }
            return new TopicRegistration[0];
        }
      
        List<TopicRegistration> result = new ArrayList<>();
        
        int i=0;
        for(Iterator<TopicRegistration> iter=topicQ.iterator();iter.hasNext()&&i<KademliaCommonConfig.K;i++)
        	result.add(iter.next());
        //for(Iterator<TopicRegistration> iter=topicQ.iterator();iter.hasNext()&&i<2;i++)
        //	result.add(iter.next());
        
        return (TopicRegistration []) result.toArray(new TopicRegistration[result.size()]);
    }
    //TODO
    public String dumpRegistrations() {
    	String result = "";
    	for(Topic topic: topicTable.keySet()) {
    		ArrayDeque<TopicRegistration> regQ = topicTable.get(topic);
    		for(TopicRegistration reg: regQ) {
    			result += this.hostID + ",";
    			result += reg.getTopic().getTopic() + ",";
    			result += reg.getNode().getId()+ ",";
    			result += reg.getTimestamp() +"\n";
    		}	
    	}
        return result;
    }

    public float percentMaliciousRegistrations() {
        int num_registrations = 0;
        int num_evil = 0;
    	for(Topic topic: topicTable.keySet()) {
    		ArrayDeque<TopicRegistration> regQ = topicTable.get(topic);
    		for(TopicRegistration reg: regQ) {
                num_registrations += 1;
                KademliaNode n = reg.getNode();
                if (n.is_evil)
                    num_evil += 1;
            }
        }
        return ((float) num_evil)/num_registrations;
    }
    
    
    public HashMap<Topic,Integer> getRegbyTopic(){
    	//System.out.println("Reg by topic");
        HashMap<Topic,Integer> regByTopic = new HashMap<Topic,Integer>();
        for(Topic t: topicTable.keySet())
        {
        	ArrayDeque<TopicRegistration> regs = topicTable.get(t);
      
        	regByTopic.put(t, regs.size());
        }
        
        return regByTopic;

    }
    
    public int getRegbyRegistrar(){
        return allAds.size();
    }
    
    public HashMap<BigInteger,Integer> getRegbyRegistrant(){
        HashMap<BigInteger,Integer> regByRegistrant = new HashMap<BigInteger,Integer>();
    	 for(ArrayDeque<TopicRegistration> t: topicTable.values())
         {
    		Object[] treg = t.toArray();
   
    		for(Object reg : treg)
    		{
    			int count=0;
    			if(regByRegistrant.get(((TopicRegistration)reg).getNode().getId())!=null)count=regByRegistrant.get(((TopicRegistration)reg).getNode().getId());
    			count++;
    			regByRegistrant.put(((TopicRegistration)reg).getNode().getId(),count);
    	    	//System.out.println("Table "+((TopicRegistration)reg).getNode().getId()+" "+count);
    		}
         }
        return regByRegistrant;

    }
}
