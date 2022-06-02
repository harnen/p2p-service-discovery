# Requirements

## Actors
We assume an Ethereum network nodes all being part of Ethereum DHT. Each node can play any (or both) of the two roles: 
* Registrants - nodes that register for a specific topic and want to be discovered by their peers. The registrants make themselves discoverable by placing advertisements on registrants. 
* Registrars - a node that accepts registrations made by registrants and respond to topic queries. When asked for a specific topic, a registrar should respond with registrants that registered for the topic the registrar is aware of. 
* Query node - a node that tries to discover registrants under a specific topic. 

## Assumptions
* **A1** - the network consists of a large amount of nodes (1k-10k)
* **A2** - each node can register to multiple topics
* **A3** - the popularity of the topics may vary significantly and follows a zipf distribution
* **A4** - no single node can be trusted.
* **A5** - a single malicious participant can produce multiple fake (Sybil) identities, but will be limited in the number of the IP addresses they control. 
* **A6** - malicious nodes may be present in the network. The attackers may send any type and amount of messages to any other node, limited only by their the attacker resources (bandwidth, CPU). 


## Design goals
Under the assumption listed above, we target to achieve the following goals:
* **G1** - all the registrants (regardless of the topic they register for) should be able to place their advertisements in the network. Aka no registrants can be globally denied registrations.
* **G2** - all the registrants within each topic should have a similar probability of being discovered by their peers. 
* **G3** - the load (in terms of sent and received messages) should be equally distributed across all the nodes regardless of their ID and location in the network
* **G4** - the registration operation should be efficient in terms of time (fast) for all the registrants
* **G5** - the registration operation should be efficient in terms of overhead (low amount of sent/received messages) for all the registrants
* **G6** - the lookup operation should be efficient in terms of time (fast) and messages sent (hop count) for all the query nodes
* **G7** - the number of total registrations per topic should be proportional to the popularity of the topic (the number of registrants).
* **G8** - the protocol should be resistant to network dynamic (nodes joining leaving)
* **G9** - the protocol should be resistant to sybil attacks lanched by malicious nodes
