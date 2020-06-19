package peersim.kademlia;

import peersim.kademlia.Registration;



interface TopicTable {

    boolean register(Registration r);

    Registration[] getTopic(String t); 
  
}