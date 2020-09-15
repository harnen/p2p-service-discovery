package peersim.kademlia;

import peersim.kademlia.TopicRegistration;


interface TopicTable {

    public boolean register(TopicRegistration r, Topic t);
    public TopicRegistration[] getRegistration(Topic t);
    public String dumpRegistrations();

  
}