package peersim.kademlia;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
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
    // The best current ticket for each queue TODO:
    private HashMap<Topic, Ticket> bestTickets;
    // Competing tickets for each topic 
    private HashMap<Topic, ArrayList<Ticket>> competingTickets;
    // All topic advertisements ordered by registration
    private ArrayDeque<TopicRegistration> allAds;


    public Discv5TopicTable() {
        topicTable = new HashMap<Topic, ArrayDeque<TopicRegistration>>(); 
        bestTickets = new HashMap<Topic, Ticket>(); 
        competingTickets = new HashMap<Topic, ArrayList<Ticket>>();
        allAds = new ArrayDeque<TopicRegistration>();
    }

    public void setHostID(BigInteger id){
        this.hostID = id;
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
        	if (curr_time - r.getTimestamp() > this.adLifeTime) {
            	ArrayDeque<TopicRegistration> topicQ = topicTable.get(r.getTopic());
	            TopicRegistration r_same = topicQ.pop(); 
				it.remove(); //removes from allAds

        	    //TODO assert that r_same and r are the same registrations
			}
		}
    }

    private long getWaitingTime(TopicRegistration reg, long curr_time) {
        //System.out.println("Get Waiting time "+reg.getTopic().getTopic());

        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());
        long waiting_time;
        
        /*if(topicQ!=null) {
        for(Iterator<TopicRegistration> itr = topicQ.iterator();itr.hasNext();)  {
        	TopicRegistration t = itr.next();
            System.out.println(t.getTopic().getTopic()+" "+t.getNode().getId());
         }
        }*/
        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //Iterator<TopicRegistration> it = topicQ.iterator();
            System.out.println("Ad already registered by this node");
            return -1;
        }

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
    
    public void register_ticket(Ticket ticket, Message m) {
        Topic ti = ticket.getTopic();
        Topic topic = new Topic(ti.topic);
        //topic.setHostID(this.hostID);

        ticket.setMsg(m);
        ArrayList<Ticket> ticketList = competingTickets.get(topic);
        if (ticketList == null) {
            ArrayList<Ticket> l = new ArrayList<Ticket>();
            l.add(ticket);
            competingTickets.put(topic, l);
        }
        else {
            ticketList.add(ticket);
        }
    }

    private void register(TopicRegistration reg) {
        ArrayDeque<TopicRegistration> topicQ = this.topicTable.get(reg.getTopic());
        if (topicQ != null)
            topicQ.add(reg);
        else {
            ArrayDeque<TopicRegistration> q = new ArrayDeque<TopicRegistration>();
            q.add(reg);
            //System.out.println("Add topictable "+reg.getTopic().getTopic());
            this.topicTable.put(reg.getTopic(), q);
        }
        this.allAds.add(reg);
    }
    
    public Ticket [] makeRegisterDecisionForTopic(Topic ti, long curr_time) {   
        Topic topic = new Topic(ti.topic);
        //topic.setHostID(this.hostID);
        
        ArrayList<Ticket> ticketList = competingTickets.get(topic);
        if (ticketList == null) {
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

        Ticket bestTicket = bestTickets.get(topic);
        if (bestTickets == null) {
            System.out.println("This should not happen: bestTicket is null");
        }

        /*if (!ticketList.contains(bestTicket)) {
            System.out.println("This should not happen: bestTicket is not in competing tickets");
        }*/

        //Register as many tickets as possible (subject to resource 
        //availability in topicTable) starting with best tickets
        ArrayList<Ticket> newTicketList = new ArrayList<Ticket>();
        updateTopicTable(curr_time);
        Collections.sort(ticketList);
        for(Ticket ticket : ticketList) {
            TopicRegistration reg = new TopicRegistration(ticket.getSrc(), topic, curr_time);
            reg.setTimestamp(curr_time);
            long waiting_time = getWaitingTime(reg, curr_time);
            if (waiting_time == -1) {
                System.out.println("already registered advert");
                ticket.setRegistrationComplete(false);
                ticket.setWaitTime(waiting_time);
            }
            else if (waiting_time == 0) {
                register(reg);
                ticket.setRegistrationComplete(true);
                if (ticket.equals(bestTicket)) {
                    bestTickets.remove(topic);
                }
                //System.out.println("Registered topic: " + topic.getTopic() + " at node: " + ticket.getSrc());
            }
            else { //waiting_time > 0
                waiting_time = (waiting_time - ticket.getRTT() - KademliaCommonConfig.ONE_UNIT_OF_TIME > 0) ? waiting_time - ticket.getRTT() - KademliaCommonConfig.ONE_UNIT_OF_TIME : 0;
                ticket.updateWaitingTime(waiting_time);
                ticket.setRegistrationComplete(false);
                newTicketList.add(ticket);
            }
        }

        Ticket [] tickets = (Ticket []) ticketList.toArray(new Ticket[ticketList.size()]);
        //update competingTickets and bestTicket
        competingTickets.remove(topic);
        if (newTicketList.size() > 0) {
            competingTickets.put(topic, newTicketList);
            bestTicket = newTicketList.get(0);
            for (Ticket ticket : newTicketList) {
                if (ticket.getWaitTime() < bestTicket.getWaitTime())
                    bestTicket = ticket;
            }
            bestTickets.put(topic, bestTicket);
        }
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
        
        if (waiting_time == -1) {
            //already registered
            return new Ticket(topic, curr_time, waiting_time, advertiser, rtt_delay);
        }
        Ticket best_ticket = bestTickets.get(topic);
        if (best_ticket != null) {
            long next_register_time = best_ticket.getReqTime() + best_ticket.getCumWaitTime();
            waiting_time = (next_register_time - curr_time - KademliaCommonConfig.ONE_UNIT_OF_TIME >= rtt_delay) ? next_register_time - curr_time - rtt_delay - KademliaCommonConfig.ONE_UNIT_OF_TIME : 0;

            return new Ticket(topic, curr_time, waiting_time, advertiser, rtt_delay);
        }
        else { // no best ticket exists
            waiting_time = (waiting_time - rtt_delay - KademliaCommonConfig.ONE_UNIT_OF_TIME > 0) ? waiting_time - rtt_delay - KademliaCommonConfig.ONE_UNIT_OF_TIME : 0;
            Ticket ticket = new Ticket(topic, curr_time, waiting_time, advertiser, rtt_delay);
            bestTickets.put(topic, ticket);
            return ticket;
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
                    System.out.println("Error in topic table lookup !");
                    String result = "Unable to find identical topics: ";
                    result += topic.toString();
                    result += "\n";
                    result += ti.toString();
                    result += "\n";
                    System.out.println(result);
                }
            }
            return new TopicRegistration[0];
        }

        return (TopicRegistration []) topicQ.toArray(new TopicRegistration[topicQ.size()]);
    }
    //TODO
    public String dumpRegistrations() {
    	String result = "";
    	for(Topic topic: topicTable.keySet()) {
    		ArrayDeque<TopicRegistration> regQ = topicTable.get(topic);
    		for(TopicRegistration reg: regQ) {
    			result += this.hostID + ",";
    			result += reg.getTopic().getTopic() + ",";
    			result += reg.getNode().getId()+ "\n";
    		}	
    	}
        return result;
    }
}
