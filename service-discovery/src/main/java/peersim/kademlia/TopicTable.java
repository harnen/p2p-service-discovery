package peersim.kademlia;

import peersim.kademlia.Registration;


interface TopicTable {

    public boolean register(Registration r, Topic t);
    public Registration[] getRegistration(Topic t);

  
}