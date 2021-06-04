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

    private static final int amplify = 1; 
    private static final int minimumBaseWaitingTime = 100;
    private static final double groupModifierExp = 1;
    private static final double topicModifierExp = 2;
    private static final double ipModifierExp = 5;
    private static final double idModifierExp = 5;

	public Discv5GlobalTopicTable() {
        super();
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
        
        
        //System.out.println("Competing tickets "+getNumberOfCompetingTicketsPerTopic(t));

        //compute ticket waiting time
        long waiting_time = getWaitingTime(reg, curr_time, null);
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
       
        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //logger.warning("Ad already registered by this node");
            return -1;
        }
      
        // Compute the time when the next available slot is available in the topic table
        if(allAds.size() == this.tableCapacity) {
            TopicRegistration r = allAds.getFirst();
            long age = curr_time - r.getTimestamp();
            waiting_time = this.adLifeTime - age;
        }
        else {
            waiting_time = 0;
        }

        return waiting_time;
    }
    
   protected long getWaitingTime(TopicRegistration reg, long curr_time, Ticket ticket) {
        long waiting_time=0;
    	long baseWaitingTime;
    	long cumWaitingTime = 0;

    	if(ticket!=null) // if this is the first (initial) ticket request, ticket will be null
            cumWaitingTime=ticket.getCumWaitTime()+ 2*ticket.getRTT();

        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());

        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //logger.warning("Ad already registered by this node");
            return -1;
        }
    
        //baseWaitingTime = KademliaCommonConfig.AD_LIFE_TIME / this.tableCapacity;
        baseWaitingTime = minimumBaseWaitingTime;
        double modifier = Math.pow(getTopicModifier(reg)*getIPModifier(reg)*getIdModifier(reg),groupModifierExp);
        long neededTime = (long) (baseWaitingTime * modifier);
        //long neededTime = minimumBaseWaitingTime*(long)getTopicModifier(reg);
        waiting_time = neededTime - cumWaitingTime;
        
        int size = topicQ!=null?topicQ.size():0;
        //System.out.println("Modifiers topic "+reg.getTopic().getTopic()+" "+getTopicModifier(reg)+" "+getIPModifier(reg)+" "+getIdModifier(reg)+" "+size);
        //System.out.println("Waiting time "+baseWaitingTime+" "+modifier+" "+neededTime+" "+cumWaitingTime+" "+waiting_time);

        if(waiting_time<0)
            waiting_time = getWaitingTime(reg, curr_time);
 
        return waiting_time;
    }
    
    /*protected long getWaitingTime(TopicRegistration reg, long curr_time, Ticket ticket) {
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());
        long waiting_time=0;
    	long baseWaitingTime;
    	long cumWaitingTime = 0;
    	if(ticket!=null)cumWaitingTime=ticket.getCumWaitTime();

        // check if the advertisement already registered before
        if ( (topicQ != null) && (topicQ.contains(reg)) ) {
            //logger.warning("Ad already registered by this node");
            return -1;
        }
    

        baseWaitingTime = 1000;

        int topicSize = topicQ!=null?topicQ.size():0;

		//int competing = (competingTickets.get(reg.getTopic())!=null)?competingTickets.get(reg.getTopic()).size():0;

		int competing=0;
		//System.out.println("Get waiting time size:"+topicSize+" competing:"+competing);

        waiting_time = (long) (Math.pow(topicSize+competing,1.5) * baseWaitingTime) - cumWaitingTime - 2*ticket.getRTT();
        if(waiting_time<0)waiting_time=0;

        return waiting_time;
    }*/
    
    private double getTopicModifier(TopicRegistration reg) {
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());

        int competing=getNumberOfCompetingTicketsPerTopic(reg.getTopic());

        int topicSize = topicQ!=null?topicQ.size():0;

    	return Math.pow(topicSize+competing,amplify*topicModifierExp);
    }
    
    private double getIPModifier(TopicRegistration reg) {

    	double counter=0.0;
		Iterator<TopicRegistration> it = allAds.iterator();
		while (it.hasNext()) {
    		TopicRegistration r = it.next();
    		if(r.getNode().getAddr().equals(reg.getNode().getAddr()))counter++;
		}

    	return Math.pow(counter+1,amplify*ipModifierExp);
    }
    
    private double getIdModifier(TopicRegistration reg) {

    	double counter=0.0;
		Iterator<TopicRegistration> it = allAds.iterator();
        BigInteger reg_id;

        // Use attackerID for registrant ID if node is evil
        if (reg.getNode().is_evil)
            reg_id = reg.getNode().getAttackerId(); 
        else
            reg_id = reg.getNode().getId();

		while (it.hasNext()) {
    		TopicRegistration r = it.next();
            if (r.getNode().is_evil) { // if node is evil, use its attackerId as node id
                if(r.getNode().getAttackerId().equals(reg_id))
                    counter++;
            }
            else 
    		    if(r.getNode().getId().equals(reg_id))
                    counter++;
		}

    	return Math.pow(counter+1,amplify*idModifierExp);
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
        
        if(competingTickets!=null) {
	        ArrayList<Ticket> ticket_list = competingTickets.get(t);
	        if(ticket_list!=null)
	        	num_tickets += ticket_list.size();
	        
        }
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
            /*if(topicTable.get(topic)==null)
            	System.out.println("Get Competing by topic "+topic.getTopic()+" competing:"+tickets.size());
            else 
            	System.out.println("Get Competing by topic "+topic.getTopic()+" competing:"+tickets.size()+" occupancy:"+topicTable.get(topic).size());
            */
            ticketCompetingList.put(topic.getTopic(),tickets.size());
            ticketList.addAll(tickets);

            //System.out.println("Get Competing "+topic.getTopic()+" "+competingTickets.get(topic).size()+" "+ticketList.size());

        }
        Collections.sort(ticketList);
        updateTopicTable(curr_time);

        //Register as many tickets as possible (subject to availability of space in the table)

        for(Ticket ticket: ticketList) {

            TopicRegistration reg = new TopicRegistration(ticket.getSrc(), ticket.getTopic(), curr_time);
            reg.setTimestamp(curr_time);

            //long waiting_time = getWaitingTime(reg, curr_time,ticket);
        	long waiting_time = getWaitingTime(reg, curr_time,ticket);
        	//System.out.println("Ticket waiting time "+waiting_time);

        	int topicOccupancy = 0;
            if(this.topicTable.get(reg.getTopic())!=null)
                topicOccupancy = this.topicTable.get(reg.getTopic()).size();
        
            if (containsReg(reg)) { 
                // rejected because a registration from ticket src for topic already exists
                ticket.setRegistrationComplete(false);
                ticket.setWaitTime(-1);
                waiting_time=-1;
            }
            //else if ( (waiting_time == 0) && (topicOccupancy < adsPerQueue) && (this.allAds.size() < tableCapacity) ) { //accepted ticket
            else if ((this.allAds.size() < tableCapacity) && waiting_time==0){ //accepted ticket
                register(reg);
                ticket.setRegistrationComplete(true);
                ticket.setOccupancy(topicOccupancy);
                KademliaObserver.reportCumulativeTime(ticket.getTopic(), ticket.getCumWaitTime());
            }
            else { //waiting_time > 0, reject (for now) due to space
                waiting_time = (waiting_time- ticket.getRTT() > 0) ? waiting_time - ticket.getRTT() : 0;
                ticket.updateWaitingTime(waiting_time);
                ticket.setRegistrationComplete(false);
                ticket.setOccupancy(topicOccupancy);
                
            }
            KademliaObserver.reportWaitingTime(ticket.getTopic(), waiting_time);
        }
        for (Topic topic: topicSet) {
            nextDecisionTime.remove(topic);
            competingTickets.remove(topic);
        }
        Ticket [] tickets = (Ticket []) ticketList.toArray(new Ticket[ticketList.size()]);
        return tickets;
    }
    
    
    private boolean containsReg(TopicRegistration reg) {
    	
        ArrayDeque<TopicRegistration> topicQ = topicTable.get(reg.getTopic());

	    if ( (topicQ != null) && (topicQ.contains(reg)) ) {
	        //logger.warning("Ad already registered by this node");
	        return true;
	    }
	    return false;
    }
    
    private long nextExpirationTime(long curr_time) {
        TopicRegistration r = allAds.getFirst();
        long age = curr_time - r.getTimestamp();
        return this.adLifeTime - age;
    }
    
    
    private double getTopicsEntropyModifier(Topic topic) {
    	
    	//System.out.println("Looking for entropy topic "+topic.getTopic()+" "+topicTable.get(topic).size());
        double entropy1 = 0.0;
        for (ArrayDeque<TopicRegistration> q: topicTable.values()) {
            double p = (double)q.size() / (double)allAds.size();
            //System.out.print("P "+p+" ");
            entropy1 += p * Math.log(p) / Math.log(2);
            //System.out.print("entropy1 "+entropy1+" ");

        }
        entropy1 *=-1;
       // System.out.println("entropy1 "+entropy1);
        double entropy2 = 0.0;
       // System.out.print("entropy ");
        for (Topic t: topicTable.keySet()) {
    //    	System.out.print("topic "+t.getTopic()+" "+topicTable.get(t).size()+" ");
        	double p;
        	if(topic.equals(t)) 
        		p = (double)(topicTable.get(t).size()+1) / (double)allAds.size();
        	else 
        		p = (double)topicTable.get(t).size() / (double)allAds.size();

            entropy2 += p * Math.log(p) / Math.log(2);
        }
        entropy2 *=-1;
    //    System.out.println(" entropy");
        
        System.out.println("Entropy1:"+entropy1+" entropy2:"+entropy2+" return:"+10*entropy1/entropy2);
        return 10*entropy1/entropy2;
    }
    
}



