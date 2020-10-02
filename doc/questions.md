# Questions

## Discv5 Ticketing Protocol

* Currently the registrars (advertisers) maintain on-going registrations in a ticket-table, where the table holds k=3 tickets with peers at each distance step from the topic hash. A ticket for a registration is removed from the ticket-table once the registration is successful. As soon as there is space in the ticket  the ticket request from the ticket-table once the advertisement for the ticket requests when an ad is successfully placed.

* The ticket-based approach is currently producing a lot of registration and lookup traffic. It would be good to have a way of driving both lookup and registration processes in order to control and limit the rate of outgoing lookup and registration messages. 

* In order to control the rate of registration and lookup messages emitted by nodes, we need a way to drive both the registration and lookup processes. 

* For registration operations, One possible approach is to `pause' or slow-down the registration process at a node when i) the node receives sufficient traffic for the protocol associated with the topic (discovered by enough peers) or ii) there are sufficient number of active (unexpired) registrations by the node. 

* For the lookup operations, it seems wasteful for nodes to send topic lookups to all the distances at the same rate. It could be better to send more lookup requests to the nodes further away from the topic hash. Lookups can also overwhelm (i.e., create hotspots) for popular topics, if not done properly.


