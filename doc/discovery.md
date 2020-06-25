# DEVP2P service discovery

# Node discovery

### Node discovery process

Below is described the steps of the discovery process for DEVP2P to establish RLPx connections with other peers:

* A DEVP2P node supports a configurable number of concurrent transactions. By default this number is 50. Out of the 50 connections, one third is for outgoing connections (16) and two thirds for incoming (34). Incoming connections are not controlled by users (first come first served I guess) and outgoing connections are established after the discovery process using DEVP2P DHT.
* Once a new node is started, preconfigured bootstrap nodes are stored in the local table and 3 random lookups are performed to fill up the table.
* Results from lookups are stored in a bucket size buffer ordered by distance to the target.
* Every time there is an empty slot in the connections list, a node is read from the buffer (and consumed) and a new connection is started. If the buffer is empty the process waits for the buffer to be filled again.
* If the buffer is empty a lookup to random node is performed and the buffer filled with the random walk results according to the following. The lookup initiator starts by picking `α` closest nodes to the target it knows of. The initiator then sends concurrent `FINDNODE` packets to those nodes. `α` is a system-wide concurrency parameter, such as `3`. In the recursive step, the initiator resends `FINDNODE` to nodes it has learned about from previous queries. Of the `k` nodes the initiator has heard of closest to the target, it picks `α` that it has not yet queried and resends `FINDNODE` to them. Nodes that fail to respond quickly are removed from consideration until and unless they do respond. If a round of `FINDNODE` queries fails to return a node any closer than the closest already seen, the initiator resends the find node to all of the `k` closest nodes it has not already queried. The lookup terminates when the initiator has queried and gotten responses from the `k` closest nodes it has seen.

Note: This is the process for Discv4 protocol. Discv5 is not used yet in the current implementation, it is only used to advertise Light Client service (nodes that relays transactions for non-full nodes) but not for searching. I assume Discv5 process would be the same but doing searches within a certain radius for the specific topic (ETH service), although is not clear to me  how to define the radius.



### Discovery table

* DEVP2P uses a Kademlia Distributed hash table with 17 buckets (257 buckets in v5???) and a bucket size of 16. Nodes in the buckets are ordered per log distance (check). If the bucket is already full, candidate nodes are stored in a so-called replacement list that stores up to 10 nodes (16 in v5 -check-).
* The DHT stores node entries. Each entry contains node id and network information (IP address, etc)
* A number of checks are performed before entering the table. TBC
* Every 5 s?(on average), the last node of a random bucket is pinged and replaced with a random node from the respective replacement list if it fails to respond. In contrast to buckets, the replacement list is a simple FIFO queue that evicts the last entry every time a previously unknown node is added to the list. This should be checked for v5.


### Network messages 

The following are the network messages sent between peers to exchange discovery information:

* `WHOAREYOU`: Contains the handshake challenge.
* `PING`: It is sent during liveness checks.
* `PONG`: It is the reply to `PING`.
* `FINDNODE`: It is a query for nodes in the given bucket.
* `NODES` / `NEIGHBOURS`: `NEIGHBOURS` is the reply to `FINDNODE` in v4. In v5 `NODES` is the reply to `FINDNODE` and `TOPICQUERY`.

### Lookup protocol

`A -> B FINDNODE(randomId)`

`A <- B   WHOAREYOU (including id-nonce, enr-seq)`

`A -> B   FINDNODE (with authentication header, encrypted with new initiator-write-key)`

`A <- B   NODES (encrypted with new recipient-write-key)`


### Liveness protocol

* Checking node liveness whenever a node is to be added to a bucket is impractical and creates a DoS vector. Implementations should perform liveness checks asynchronously with bucket addition and occasionally verify that a random node in a random bucket is live by sending PING. When the PONG response indicates that a new version of the node record is available, the liveness check should pull the new record and update it in the local table.
* When responding to FINDNODE, implementations must avoid relaying any nodes whose liveness has not been verified. This is easy to achieve by storing an additional flag per node in the table, tracking whether the node has ever successfully responded to a PING request.


## Topic discovery 

### Topic advertisement process

* A node that provides certain service (called topic)  tries to 'place an ad' for itself when it makes itself discoverable under that topic.
* Depending on the needs of the application, a node can advertise multiple topics or no topics at all. 
* Nodes keep a 'topics table', accepting topic ads from other nodes and later returning them to nodes searching for the same topic.
* Topics registration are throttled and registrants must wait for a certain amount of time before they are admitted, in order to keep a constant advertisement lifetime.
* The waiting time constant is defined to `target-ad-lifetime = 15min` Don't know why.
* This is achieved via tickets.

#### Tickets

* In order to limit the number of registrations a node can issue, there is a process that uses tickets to set the registration pace.
* When a node attempts to place an ad, it receives a 'ticket' which tells them how long they must wait before they will be accepted.
* The registrant node should keep the ticket and present it to the node that receives the registration,  when the waiting time has elapsed.
* The waiting time constant is, set to 15 min. `target-ad-lifetime = 15min`.
* If the table or the topic queue is not full, the ad can be placed inmediately.
* When the table is full, the waiting time is assigned based on the lifetime of the oldest ad across the whole table, i.e. the registrant must wait for a table slot to become available
* When the topic queue is full, the waiting time depends on the lifetime of the oldest ad in the queue. The assigned time is `target-ad-lifetime - oldest-ad-lifetime` in this case.


#### Topic radius and radius estimation

* When the number of nodes advertising a topic is at least a certain percentage of the whole discovery network (rough estimate: at least 1%), ads may simply be placed on random nodes because searching for the topic on randomly selected will locate the ads quickly enough.
* But when the number of advertisers is small, to keep topic search fast enough, advertisers should concentrate in a subset of the region of node ID address space, within a certain distance to the topic hash `H(t)`, called radius.
* To place their ads, participants simply perform a random walk within the currently estimated radius and run the advertisement protocol by collecting tickets from all nodes encountered during the walk and using them when their waiting time is over.
* But how to estimate the topic radius? This is the question. Instead we propose [Proposition1](doc/proposition1.md), but we could come up with something to compare.
* Current implementation uses the waiting time as an indicator of how many other nodes are attempting to place ads in a certain region to estimate the radius. This is achieved by keeping track of the average time to successful registration within segments of the address space surrounding the topic hash. Advertisers initially assume the radius is 2^256, i.e. the entire network. As tickets are collected, the advertiser samples the time it takes to place an ad in each segment and adjusts the radius such that registration at the chosen distance takes approximately target-ad-lifetime / 2 to complete.

### Topic search process

* To find nodes for a topic, the searcher generates random node IDs inside the estimated topic radius and performs Kademlia lookups for these IDs. All (intermediate) nodes encountered during lookup are asked for topic queue entries using the TOPICQUERY packet.

* Radius estimation for topic search is similar to the estimation procedure for advertisement, but samples the average number of results from TOPICQUERY instead of average time to registration. The radius estimation value can be shared with the registration algorithm if the the same topic is being registered and searched for.


### Topic register table
* Nodes store ads in a table orderd by topic with a limited number of ads for each topic. Default total max entries are 10000 and max entries per topic are 50.
* The list of ads for a particular topic is called the 'topic queue' because it functions like a FIFO queue of limited length. 
* A node must appear only once in any topic queue.
* Ip addresses restrictions per queue should be considered. 


### Network messages 

* `REQUESTTICKET`: Requests a ticket for a topic queue.
* `TICKET`: It is the response to `REQUESTTICKET`.
* `REGTOPIC`: Registers the sender in a topic queue using a ticket.
* `REGCONFIRMATION`: Is the reply to `REGTOPIC`.
* `TOPICQUERY`: Ask nodes for a given topic.

### Lookup protocol

Same v4 lookup [protocol](https://github.com/ethereum/devp2p/blob/master/discv5/discv5-theory.md#lookup) is used in the following situations:

* Bucket refresh: chooseBucketRefreshTarget selects random refresh targets to keep all Kademlia buckets filled with live connections and keep the network topology healthy. This requires selecting addresses closer to our own with a higher probability in order to refresh closer buckets too. This algorithm approximates the distance distribution of existing nodes in the table by selecting a random node from the table and selecting a target address  with a distance less than twice of that of the selected node. This algorithm will be improved later to specifically target the least recently used buckets.
* Table refresh: Lookup to self node when refresh process. This happens when booting, and after specific times depending on the table occupancy. Need to check timings. Don't know what is the difference purpose between this and the previous case. Need to check.
* To collect tickets: There is a topicRegisterLookup timing set at 100ms that periodically checks local advertising topics and collect tickets to register to new nodes. Need to complete.
* topicSearch lookup target determined by topic radius 

### Related issues
* https://github.com/ethereum/devp2p/issues/111
* https://github.com/ethereum/devp2p/issues/112
* https://github.com/ethereum/devp2p/issues/136
* https://github.com/vacp2p/research/issues/15
* https://github.com/vacp2p/research/issues/15
* Disv5 research
* Existing simulator: https://github.com/zilm13/discv5/tree/161315190a647552aec64e800c13e92aa89a5282
# PeerSim implementation TODO

* Event-based vs cycle based -> Event based
* Determine simulation parameters
* Development items for state-of-the-art discv5 implementation: 

	* App that will simulate DEVP2P node performing lookups based on connection list occupance.
	* Lookup [process](https://github.com/ethereum/devp2p/blob/master/discv5/discv5-theory.md#lookup)
	* Topic table
	* Ticket store
	* Ticket registration windows
	* Liveness checks
	* bucker replacement cache
	* Messages:
  		* `FINDNODE`: 
  		* `PING`: 
  		* `PONG`: 
 	   * `NEIGHBOURS`:
  		* `REQUESTTICKET`: 
  		* `TICKET`: 
  		* `REGTOPIC`: 
  		* `REGCONFIRMATION`: 
  		* `TOPICQUERY`: 

* Development items for our proposal: (michal)


# Geth implementation doubts

* V5 version of the discovery table has 257 instead of 17. Why? In the eclipse attack papers say the following: In response to the eclipse attack, Geth ≥ 1.8.0 restricts the number of buckets to 17, starting from the furthest distance of 255 to the minimum possible log-distance of 239.
The log-distance metric leads to a skewed distribution of nodes between buckets: most of the lower buckets are empty, since the probability to fall into a specific bucket decays exponentially with the associated distance.
* Need to check current implementation radius estimation. Not clear in the docs
* How is multipath search done?
