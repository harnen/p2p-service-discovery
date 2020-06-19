package peersim.kademlia;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

import peersim.kademlia.Topic;
import peersim.kademlia.Registration;


public class Proposal1TopicTable implements TopicTable {


    private int capacity = KademliaCommonConfig.TOPIC_TABLE_CAP;
    private int size = 0;

    private SortedMap<Topic, List<Registration>> table;
    private BigInteger hostID;

    public Proposal1TopicTable(BigInteger hostID){
        table = new TreeMap<Topic, List<Registration>>();
        this.hostID = hostID;
    }

    private void add(Registration r, Topic t){
        if(!table.containsKey(t)){
            List list = new ArrayList<Registration>();
            list.add(r);
            table.put(t, list);
        }else{
            table.get(t).add(r);
        }
    }

    public boolean register(Registration r, Topic t){
        //if we have space, always add the registration
        if(size < capacity){
            add(r, t);
            return true;
        //table is full
        }else{
            //new topic is further closer/equal distance from the hostID than the furthest one currently in table
            if(t.compareTo(table.lastKey()) >= 0){
                table.get(table.lastKey()).remove(0);
                //if a topic has no more registration - remove it
                if(table.get(table.lastKey()).size() == 0) table.remove(table.lastKey());
                add(r, t);
                return true;
            }
        }
        
        return false;
    }

    public Registration[] getRegistration(Topic t){
        if(table.containsKey(t)){
            table.get(t).toArray();
        }
        
        return null;
    }
    
    
}