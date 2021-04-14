package peersim.kademlia;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Collections;
import java.math.BigInteger;

import peersim.kademlia.Topic;
import peersim.kademlia.TopicRegistration;
import peersim.core.CommonState;

// Round-robin Discv5 Topic table
public class Discv5GlobalTopicTable extends Discv5TopicTable { // implements TopicTable 

   
	public Discv5GlobalTopicTable() {
        super();
        System.out.println("Global topic table");
    }
  
    
   /* protected long getWaitingTime(TopicRegistration reg, long curr_time) {
    	super(reg,curr_time);
    }*/
   
    protected Ticket getTicket(Topic t, KademliaNode advertiser, long rtt_delay, long curr_time) {
        Topic topic = new Topic(t.topic);
        //topic.setHostID(this.hostID);
        //System.out.println("Get ticket "+topic.getTopic() + " " + this.hostID);
        TopicRegistration reg = new TopicRegistration(advertiser, topic, curr_time);

        //update the topic table (remove expired advertisements)
        updateTopicTable(curr_time);
        
        
        System.out.println("Competing tickets "+getNumberOfCompetingTicketsPerTopic(t));

        //compute ticket waiting time
        long waiting_time = getWaitingTime(reg, curr_time);
        int queueOccupancy = topicQueueOccupancy(t);
        
        if (waiting_time == -1) {
            //already registered
            KademliaObserver.reportWaitingTime(topic, waiting_time);
            return new Ticket (topic, curr_time, waiting_time, advertiser, rtt_delay, queueOccupancy);
        }
        
        waiting_time = (waiting_time - rtt_delay > 0) ? waiting_time - rtt_delay : 0;
     
        return new Ticket (topic, curr_time, waiting_time, advertiser, rtt_delay, queueOccupancy);
    }
    
    protected long getWaitingTime(TopicRegistration reg, long curr_time) {
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
        //if (topicQ != null)
        //    assert topicQ.size() <= this.adsPerQueue;

        // compute the waiting time
        /*if (topicQ != null && topicQ.size() == this.adsPerQueue) {
            TopicRegistration r = topicQ.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else */
        if(allAds.size() == this.tableCapacity) {
            TopicRegistration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else {
            //waiting_time = 0;
        	waiting_time = (long) Math.pow(2,getNumberOfCompetingTicketsPerTopic(reg.getTopic()))*1000;
        }

        //assert waiting_time <= this.adLifeTime && waiting_time >= 0;

        return waiting_time;
    }

    private int getNumberOfCompetingTickets() {
        int num_tickets = 0;
        for (Map.Entry<Topic,ArrayList<Ticket>> entry : this.competingTickets.entrySet()) {
            ArrayList<Ticket> ticket_list = entry.getValue();
            num_tickets += ticket_list.size();
        }
        return num_tickets;

    }

    private int getNumberOfCompetingTopics() {

        int num_topics = 0;
        for (Map.Entry<Topic,ArrayList<Ticket>> entry : this.competingTickets.entrySet()) {
            ArrayList<Ticket> ticket_list = entry.getValue();
            if (ticket_list.size() > 0 )
                num_topics += 1; 
        }

        return num_topics;
    }
    
    private int getNumberOfCompetingTicketsPerTopic(Topic t) {
        int num_tickets = 0;
        
        ArrayList<Ticket> ticket_list = competingTickets.get(t);
        if(ticket_list!=null)
        	num_tickets += ticket_list.size();
        
        return num_tickets;

    }
    
    protected Ticket [] makeRegisterDecision(long curr_time) {   
        // Determine which topics are up for decision
    	
    	ticketCompetingList.clear();
        HashSet<Topic> topicSet = new HashSet<Topic>();
        for (Topic topic : this.competingTickets.keySet()) {
            ArrayList<Ticket> tickets = this.competingTickets.get(topic);
            if (tickets != null && !tickets.isEmpty())
                topicSet.add(topic);
        }
        if (topicSet.isEmpty()) {
            return new Ticket[0];
        }

        // list of tickets to respond with MSG_REGISTER_RESPONSE
        //ArrayList<Ticket> responseList = new ArrayList<Ticket>();

        ArrayList<Ticket> ticketList = new ArrayList<Ticket>();
        for (Topic topic : topicSet) {
            ArrayList<Ticket> tickets = this.competingTickets.get(topic);
        	//System.out.println("Get Competing by topic "+topic.getTopic()+" "+tickets.size());
            ticketCompetingList.put(topic.getTopic(),tickets.size());
            ticketList.addAll(tickets);
            nextDecisionTime.remove(topic);
            competingTickets.remove(topic);
        }
        Collections.sort(ticketList);
        updateTopicTable(curr_time);

        //Register as many tickets as possible (subject to availability of space in the table)
        for(Ticket ticket: ticketList) {
            TopicRegistration reg = new TopicRegistration(ticket.getSrc(), ticket.getTopic(), curr_time);
            reg.setTimestamp(curr_time);
            long waiting_time = getWaitingTime(reg, curr_time);

        	int topicOccupancy = 0;
            if(this.topicTable.get(reg.getTopic())!=null)
                topicOccupancy = this.topicTable.get(reg.getTopic()).size();
        
            if (waiting_time == -1) { 
                // rejected because a registration from ticket src for topic already exists
                ticket.setRegistrationComplete(false);
                ticket.setWaitTime(waiting_time);
            }
            //else if ( (waiting_time == 0) && (topicOccupancy < adsPerQueue) && (this.allAds.size() < tableCapacity) ) { //accepted ticket
            else if (this.allAds.size() < tableCapacity) { //accepted ticket
                register(reg);
                ticket.setRegistrationComplete(true);
                KademliaObserver.reportCumulativeTime(ticket.getTopic(), ticket.getCumWaitTime());
            }
            else { //waiting_time > 0, reject (for now) due to space
                waiting_time = (waiting_time - ticket.getRTT() > 0) ? waiting_time - ticket.getRTT() : 0;
                ticket.updateWaitingTime(waiting_time);
                ticket.setRegistrationComplete(false);

                
            }
            KademliaObserver.reportWaitingTime(ticket.getTopic(), waiting_time);
        }
        Ticket [] tickets = (Ticket []) ticketList.toArray(new Ticket[ticketList.size()]);
        return tickets;
    }
    
}




