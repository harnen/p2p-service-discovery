package peersim.kademlia;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

import peersim.kademlia.Topic;
import sun.security.action.GetIntegerAction;
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
        this.size++;
    }

    public boolean register(Registration r, Topic tRegistered){
        //need to create a copy here. Without it - the topic class would be shared among 
        //all the class where it's registered
        Topic t = new Topic(tRegistered);
        t.setHostID(this.hostID);
        //if we have space, always add the registration
        if(size < capacity){
            System.out.println("Size lower than capacity - adding");
            add(r, t);
            return true;
        //table is full
        }else{
            //new topic is further closer/equal distance from the hostID than the furthest one currently in table
            if(t.compareTo(table.lastKey()) >= 0){
                table.get(table.lastKey()).remove(0);
                //if a topic has no more registration - remove it
                if(table.get(table.lastKey()).size() == 0) table.remove(table.lastKey());
                this.size--;
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

    public int getCapacity(){
        return this.capacity;
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }
    
    public int getSize(){
        return this.size;
    }

    public BigInteger getHostID(){
        return this.hostID;
    }

    public void clear(){
        this.table.clear();
        this.size = 0;
    }

    public String toString(){
        //need a final variable inside lambda expressions below
        final StringBuilder result = new StringBuilder();
        result.append("--------------------------------\n");
        result.append("Proposal1Topic Table size: " + this.size + "/" + this.capacity + " hostID: " + this.hostID);
        this.table.forEach((k, v) -> {
            
            result.append("\n" + k.toString() + ":");
            v.forEach((Registration reg) ->{
                result.append(" " + reg.toString());
            });
        });
        result.append("\n--------------------------------");
        return result.toString();
    }
    
}