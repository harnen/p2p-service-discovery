package peersim.kademlia;

import java.math.BigInteger;
import java.security.Security;
import java.util.Random;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


import peersim.kademlia.UniformRandomGenerator;
import peersim.core.CommonState;
import peersim.config.ParsedProperties;
import peersim.config.Configuration;
import java.lang.Math; 
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.io.Base64URLSafe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.apache.tuweni.devp2p.EthereumNodeRecord;
import org.json.JSONException;
import org.json.JSONObject;

public class Discv5GlobalTopicDnsTest{
    

	
    @Test
    public void dnsWaitingTimes() {
    	
    	JSONObject json = null;
    	try {
    	
    		 json = DnsListDistribution.readJsonFromUrl("https://raw.githubusercontent.com/ethereum/discv4-dns-lists/master/all.mainnet.ethdisco.net/nodes.json");
    	}catch(Exception e) {
    		System.err.println("Exception "+e);
    	}


        Set<String> set = json.keySet();
        Iterator<String> it = set.iterator();

        Discv5TopicTable table = new Discv5GlobalTopicTable();
        UniformRandomGenerator urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, 1);
        table.setCapacity(50000);
        table.setAdLifeTime(300000);

        long rtt_delay = 1;
        long curr_time = 0;
        long totalTime = 1000000;
        Ticket successful_ticket = null;
        Ticket failed_ticket = null;
        // Register 2 topic0 at times 0 and 1, then make decision at 2;
        //  register 2 topic2 at times 2 and 3, then make decision at 4
        //  ... 
        
        int[] topicRate= new int [] {1,2,3,4,5,6,7,8,9,10};

        //Ticket [] tickets = table.makeRegisterDecision(curr_time);

        List<Ticket> pendingTickets = new ArrayList<Ticket>();
        HashMap<Ticket,Long> failedTimes = new HashMap<Ticket,Long>(); 

		Security.addProvider(new BouncyCastleProvider());

        int[] occupancy = new int[topicRate.length];
        int reg=0;
        while(curr_time<totalTime) {
        	int topicnum=1;
        	if(curr_time%10000==0) {
	        	for(int i:topicRate) {
	        		for(int j=0;j<i;j++) {
	        			if(it.hasNext()) {
			        	    Topic topic = new Topic(new BigInteger("0"), "topic"+topicnum);
				            //System.out.println("At: " + curr_time + " ticketing for topic: " + topic.getTopic()+" "+reg+" "+set.size());
							EthereumNodeRecord enr = EthereumNodeRecord.fromRLP((Base64URLSafe.decode(json.getJSONObject(it.next()).getString("record").substring(4))));
			        	    KademliaNode advertiser = new KademliaNode(enr.publicKey().bytes().slice(KademliaCommonConfig.BITS/8).toUnsignedBigInteger(), enr.ip().toString(), enr.tcp());
				            //System.out.println(advertiser.getId()+" "+advertiser.getAddr()+" "+enr.ip().toString());
			        	    //KademliaNode advertiser = new KademliaNode(urg.generate(), "127.0.0.1", 0);
				            Ticket ticket = table.getTicket(topic, advertiser, rtt_delay, curr_time);
				            //System.out.println("At: " + curr_time + " waiting time: " + ticket.getWaitTime());
				            //assert(ticket.getWaitTime() == 0);
				            table.register_ticket(ticket, null, curr_time);
				            reg++;
	        			} else {
	        				break;
	        			}
	        		}
	        		topicnum++;
	        	}
        	}
        	curr_time+=100;
        	for(Ticket ticket: pendingTickets) {
        		//System.out.println("Ticket wait time "+ticket.getWaitTime());
        		if(failedTimes.get(ticket)!=null)
        			if(curr_time>=failedTimes.get(ticket)+ticket.getWaitTime())
        				table.register_ticket(ticket, null, curr_time);
        	}
        	
            Ticket [] regTickets = table.makeRegisterDecision(curr_time);

            for (Ticket t:regTickets) {
                if (t.isRegistrationComplete()) {
                    //System.out.println("At:" + curr_time + " successful registration for topic: " + t.getTopic().getTopic()+" "+t.getCumWaitTime()+" "+t.getOccupancy());
                    pendingTickets.remove(t);
                    
                    occupancy[Integer.valueOf(t.getTopic().getTopic().substring(5,t.getTopic().getTopic().length()))-1] = t.getOccupancy();
                }
                else { 
                   // System.out.println("At: " + curr_time + " failed ticket for topic: " + t.getTopic().getTopic() + " waiting time: " + t.getWaitTime()+" "+t.getCumWaitTime());
                    pendingTickets.remove(t);
                    pendingTickets.add(t);
                    failedTimes.put(t,curr_time);
                    occupancy[Integer.valueOf(t.getTopic().getTopic().substring(5,t.getTopic().getTopic().length()))-1] = t.getOccupancy();

                	//previousTime=curr_time;

                }
            }
        }
        
        int t=1;
        for(int i:occupancy) {
        	System.out.println("Occupancy topic"+t+" "+i);
        	t++;
        }
        
        int min = Arrays.stream(occupancy).min().getAsInt();
        int max = Arrays.stream(occupancy).max().getAsInt();

        System.out.println("Min:"+min+" Max:"+max+" "+(double)max/min);
        assert((double)max/min<1.5);

  
    }
}

