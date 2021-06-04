# Discv5 Specification

This document explains the design of the algorithms and data structures used by the service discovery protocol for Ethereum 2 (Disv5) designed and analysed in this research project.

Previous version of the discovery protocol used in Ethereum (DevP2P) network Discv4 utilises a Distributed Hash Table (DHT) based on Kademlia network to discover other nodes in the network. 
Using FINDNODE queries with appropriately chosen targets, the entire DHT can be sampled by a random walk to find all other participants. 
When building a distributed application, it is often desirable to restrict the search to participants which provide a certain service. 
A simple solution to this problem would be to simply split up the network and require participation in many smaller application-specific networks. 
However, such networks are hard to bootstrap and also more vulnerable to attacks which could isolate nodes.
To this end, in the new version of the discovery protocol for the Ethereum P2P network (Discv5) Topics are introduced. Topics can be considered as identifiers for a specific service. This way a single network can support multiple services advertisement at the same time differentiated by a Topic index. Any user of the network can be used to participate in the service discovery, even if it does not support the service, making it more resilient and efficient for non popular topics, compared with having independent service discovery networks for each topic.

The topic advertisement subsystem indexes participants by their provided services. 
A node's provided services are identified by arbitrary strings called 'topics'. 
A node providing a certain service is said to 'place an ad' for itself when it makes itself
discoverable under that topic.
Depending on the needs of the application, a node can advertise multiple topics or no topics at all. 
Every node participating in the discovery protocol acts as an 'advertisement medium', meaning that it accepts topic ads from other nodes and later returns them to nodes searching for the same topic, keeping an extra topic table (in addition to the Kademlia neighbours table) tracking their neighbors by topic index.

In this document we describe the design of the new topics system designed for Discv5. The design has been based in the requirements described in [this document](https://github.com/ethereum/devp2p/blob/master/discv5/discv5-rationale.md), along with the requirements defined [here](requirements.md).

The wire protocol used in Discv5 is detailed in the following [document](https://github.com/ethereum/devp2p/blob/master/discv5/discv5-wire.md).
(Note: TICKET response messages and TOPICQUERY response messages may include other nodes for the same topic id distance in the same response to help with the discovery of other nodes without requiring sending specific FINDNODE messages)

In the following we describe the specification of this new Topic or Service Discovery by describing the terms and the different mechanisms used in the protocol: Topic Table, Topic Advertisement and Topic Search.

## Terms

* A 'topic' is an identifier for a service provided by a node.
* An 'advertiser' is a node providing a service that wants to be discovered.
* An 'ad' (i.e., advertisement) is the registration of an advertiser for a topic on another node.
* A 'registrar' is a node that is acting as the advertisement medium in a service protocol interaction, i.e., a node that is contacted to store an ad belonging to an advertiser.
* A 'searcher' is a node looking for ads for a topic.


## Topic Table
The registrars store ads in a topic table. The total size of the topic table is limited by `topic_table_capacity`, however no per-topic limits are imposed since existing number of topics in the network is unknown. Reasonable `topic_table_capacity` is 50,000 ads. Since ENRs are at most 300 bytes in size, these limits ensure that a full topic table consumes approximately 15MB of memory. 

In order to place an ad, the advertiser must present a valid ticket to the advertiser. We discuss the details on the tickets in sections below. Each ad added to the table is stored for `target-ad-lifetime`. When the `target-ad-lifetime` expires, the ad is removed. An advertiser can only have a single ad for a specific topic in the queue, duplicate placements are rejected (althought the same node may attempt placing ads for multiple topics at the same registrar).

The topic table is shared across multiple nodes and topics and should be diverse and follow max-min fairness policy. Popular topics/nodes should not dominate the topic table and prevent less popular ones from registering. This is achieved using the waiting time function discussed below. 

### Tickets
In order to place an ad on a registrar, the advertiser must present a valid 'ticket' to the registrar.
Tickets are opaque objects issued by the registrars. 
An advertiser willing to register an ad at a registrar must first obtain a ticket by sending a 'ticket request' message to the registrar.  
In response to the ticket request, the registrar issues an initial ticket containing a 'waiting time' and sends the ticket to the advertiser in a 'ticket response' message. 
The advertiser can come back to the registrar (to register an ad) after the waiting time has elapsed and present the ticket in a 'registration request' message. 
If the advertiser comes back to the registrar, the advertiser can either place the ad (and notify the advertiser of a successful registration) or issue another ticket with a new waiting time in another ticket response message.
Assignment of 'waiting times' is the only way the registrars can control the registrations in order to both:
*  Throttle ad placement rate to prevent overflowing of topic table: when the topic table is full, the advertisers must wait for already placed ads to expire first before they are allowed to register new ads.
*  Prioritise registrations to achieve a diverse set of ads in the topic table. For example, registrations for less popular topics or registrations from advertisers that increase IP diversity (in the set of registrar IP addresses that currently have an ad in the table) can be prioritised over others. This is useful to reduce the impact of Sybil attacks on the service discovery system. 

Waiting times will be calculated according to a ['Waiting time function'](#waiting-time-function).
Enforcing this time limit prevents misuse of the topic table because any topic must be important enough to outweigh the cost of waiting for ad placement. 
Imagine a group phone call: announcing the participants of the call using topic advertisement isn't a good use of the system because the topic exists only for a short time and will have very few participants. 
The waiting time prevents using the topic table for this purpose because the call might already be over before everyone could get registered.
Also, it prevents attackers from overflowing topic table by regulating registrations in case of spamming attacks.

While tickets are opaque to advertisers, they are readable by the registrar that issued the ticket. 
The registrar uses the ticket to store the cumulative waiting time, which is sum of all waiting times the advertiser has already spent. 
Whenever a ticket is presented and a new one issued in response, the cumulative waiting time of the ticket is increased and carries over into the new ticket.

Ads should remain in the queue for a constant amount of time, the `target-ad-lifetime`. To maintain this guarantee, new registrations are throttled and registrants must wait for a certain amount of time before they are admitted. When a node attempts to place an ad, it receives a 'ticket' which tells them how long they must wait before they will be accepted. It is up to the registrant node to keep the ticket and present it to the advertisement
medium when the waiting time has elapsed. Waiting times calculation are based on topic table occupancy, described in [here](#waiting-time-function) 

Tickets are opaque objects storing arbitrary information determined by the issuing node.
While details of encoding and ticket validation are up to the implementation, tickets must
contain enough information to verify that:

- The node attempting to use the ticket is the node which requested it.
- The ticket is valid for a single topic only.
- The ticket can only be used within the registration window.
- The ticket can't be used more than once.

Implementations may choose to include arbitrary other information in the ticket, such as
the cumulative wait time spent by the advertiser. A practical way to handle tickets is to
encrypt and authenticate them with a dedicated secret key:

    ticket       = aesgcm_encrypt(ticket-key, ticket-nonce, ticket-pt, '')
    ticket-pt    = [src-node-id, src-ip, topic, req-time, wait-time, cum-wait-time]
    src-node-id  = node ID that requested the ticket
    src-ip       = IP address that requested the ticket
    topic        = the topic that ticket is valid for
    req-time     = absolute time of REGTOPIC request
    wait-time    = waiting time assigned when ticket was created
    issue-time   = time when the first registration attempt ticket was created

Tickets cannot be used beyond their lifetime. If the advertiser does not come back after the waiting time, all cumulative waiting time is lost and the advertiser must start over.
The image below depicts a single ticket's validity over time. When the ticket is issued, the node keeping it must wait until the registration window opens. The length of the registration window is implementation dependent, but by default `10 seconds` is used. The ticket becomes invalid after the registration window has passed.

![ticket validity over time](./imgs/ticket-validity.png)


## Ticket Registration

The main goal of this protocol is to distribute advertisements to be found within the network. 
An important issue is how advertisers distribute their ads among registrar nodes. 
Since every node may act as an advertisement medium for any topic, advertisers and searchers looking for ads must somehow meet at common registrars. 
Ideally, the topic search should be fast even when the number of advertisers for a topic is much smaller than the number of all live nodes.
Given that in a decentralised setting, advertisers and registrars can not apriori agree on a subset of nodes to serve as the advertisement media for the topics, the main challenge for nodes is to find the "right" set of nodes to send advertisements and topic search queries so that they quickly meet at common nodes.  

Below are three naive approaches for the selection of nodes for registering ads and searching the peers for a topic ID: 
 1. "Walk" the DHT, exhaustively finding all neighbors in each bucket starting with the closest bucket. 
Obviously, such an approach would be unscalable as it would lead to excessive overhead on the network in terms of number of messages and would require huge storage space to register ads. 
 2. A node can select a random subset of nodes by, for instance, picking a random Node ID from each bucket distance and finding the closest node to that ID. 
This approach would be lightweight, but the downside is the potential inefficiency of search operations; that is, it could potentially take a lot of time and search messages for advertisers to find peers at registrars, especially for less popular services with small sets of peers.  
NOTE: On the other hand, when the number of nodes advertising a topic is at least a certain percentage of the whole discovery network (rough estimate: at least 1%), ads may simply be placed on random nodes because searching for the topic on randomly selected nodes can locate the ads quickly enough.
 3. Using node(s) closest to the topic hash or hash(topic ID), i.e., mapping the topic ID to the node ID space by using the hash of the topic ID.
This is an efficient approach, but it leads to poor load-balancing in terms of balance of load across registrars, because registrars whose IDs are close to the hash of a popular topic ID receive a lot of search and registration traffic, while the rest of the nodes receive very little traffic. 

However, these naives approaches are not efficient in terms of overhead, search time or uniform distibution of discovered nodes, or do not follow the requirements described in [here](requirements.md).

In order to perform an optimal advetisiment registration placement among the registrars topic tables, a ticket table structure is used to follow and control the active on-going registrations per node. 

The ticket registration process follows the next steps:

1. `K` nodes are selected for each bucket of the [ticket table structure](#ticket-table) (depending on the bucket size). Only `K` nodes are selected per bucket and no new registrations are started till the advertisement registration on these nodes have expired in case of succesful registration or the registration has been discarded after `T` attempts on the selected node without succesful registration.
2.  REGTOPIC request is sent to the selected node without attaching any TICKET.
3.  Registrar node replies with a TICKET response. This message includes the TICKET a waiting time and a ticket issue time.
4.  The advertiser replies after the waiting time with another REGTOPIC request with the previously received TICKET attached to it.
5.  The registrer replies with a new TICKET response with a new TICKET and a new waiting time in case the registration is not succesful. 
6.  The registration is succesful when the [waiting time calculated](#waiting-time-function) at the registrar is not greater than the difference of the current time and the initial ticket issue time. In this case the registrar sends a REGCONFIRMATION response to the advertiser.
7.  The process is restarted with a new selected node in the same bucket after the advertisement expires in case of succesful registration, or after `T` in case of unsuccesful registration.

<!--As discussed in ['Ticket Table'](#ticket-table), advertisers can perform parallel registrations at each and every bucket (relative to the topic hash), resulting with k on-going registrations per bucket. However, registering at all the bucket distances in parallel means that advertisers for a topic will all attempt to register at the nodes closest to the topic hash. Therefore, this approach also suffers from the same load-balancing problem with the third approach above. 

A better approach is to sequentially "walk" through the buckets starting from the farthest bucket and proceed incrementally with buckets closer to the topic hash. Initially, an advertiser initiate k registrations at registrars located in the farthest bucket from the topic hash. Once these k registrations are complete, then the advertiser initiates k new registrations with registrars in the next closest bucket, and once these k registrations are complete, the advertiser starts another k registrations in the next closest bucket and  so on. The walk though the buckets is terminated when either a stopping condition occurs or when k registrations are complete in the closest bucket to the topic hash. 

In the current implementation, the stopping condition occurs when the elapsed time since the first successful registration (at the farthest bucket to the topic hash) exceeds target-ad-lifetime. Once the stopping condition occurs, an advertiser "backs off" and restarts the topic registrations from the farthest bucket to the topic hash. We are currently investigating other stopping conditions such as one that is probabilistically triggered depending either on the occupancy of topic queues at registrars or on the changes in the observed sequence of cumulative waiting times, and so on. 

A better approach is to sequentially "walk" through the buckets starting from the farthest bucket and proceed incrementally with buckets closer to the topic hash. During the walk, the advertiser observes the achieved cumulative waiting times to successfully place ads (the elapsed time from the first ticket request to the receipt of notification of successful ad placement). Initially, the advertiser starts with only k registrars at the farthest bucket from the topic hash. Once the registrations are complete, then the advertiser initiates k registrations in the next closest bucket, adding this bucket to the current "bucket window" (the sequence of buckets for which there are on-going registrations or placed ads). The advertiser also keeps track of the expiration times of already placed ads and replaces expired ads with a fresh registration (by adding a new node to the ticket table at the corresponding  bucket).

 The advertiser increases the "bucket window" until either the last bucket in the window is the closest bucket to the topic hash or a stopping condition occurs. A possible stopping condition is the sudden increase in the average cumulative waiting times in the last bucket. Because the number of nodes is halved for each new bucket compared to the previous one, the waiting times are expected to be doubled (increase linearly). If the increase in the cumulative waiting time is higher than a linear increase in the last bucket, then the bucket window is reduced. Following the conventions of the TCP congestion protocol, an advertiser can apply an additive increase multiplicative decrease approach to the bucket window size. This means halving the bucket window size, which essentially means halving the number of ads (i.e., waiting until half of the ads in the last half of the buckets expire) and then proceeding with incrementally increasing the window size. -->



<!--Topic queues are subject to competition. 
To keep things fair, the advertisement medium prefers tickets which have the longest cumulative waiting time when multiple tickets are received for the same topic but not enough room in the topic table for them.
In addition to the ads, each queue also keeps the current 'best ticket', i.e. the ticket with the longest cumulative waiting time. 
When a ticket with a better time is submitted, it replaces the current best ticket. Once an ad in the queue expires, the best ticket is admitted into the queue and the node which submitted it is notified.-->


<!--To keep ticket traffic under control, an advertiser requesting a ticket for the first time gets a waiting time equal to the cumulative time of the current best ticket. For a placement attempt with a ticket, the new waiting time is assigned to be the best time minus the cumulative waiting time on the submitted ticket.-->



### Ticket Table

The above description explains the storage and placement of ads on a single registrar, but the advertisers need to distribute ads redundantly on multiple nodes in order to speed up its discovery and to be discovered by more searchers at once. 

An advertiser maintains a per-topic 'ticket table' for each topic advertised in order to keep track of the ongoing registration attempts.
This table can be equivalent to the routing table used in Kademlia protocol, but instead of storing nodes based on distance for routing purposes, nodes are stored based on distance to topic ID to keep track of on-going registrations.

This table is made up of k-buckets of logarithmic distance to the topic hash (topic ID), i.e. the table stores k registrars for every distance step (bucket). 
It is sufficient to use a small value of k such as `k=3`. 
For this table no replacement list is used, different from the Kademlia routing table.
Ticket table buckets are filled from the local routing table (Kademlia DHT Table) with the same distance to the topic hash.

For every node stored in the ticket table, the advertiser attempts to place an ad on the node and keeps the latest ticket issued by that node. It also keeps references to all pending tickets in a priority queue keyed by the expiry time of the ticket so it can efficiently access the next ticket for which a placement attempt is due.

Once a ticket request is sent 


<!--In this project we evaluated two different approaches to remove tickets from ticket table:
* Removing the ticket after the registration lifetime expired: In this case we remove a ticket from the table, not after this registration has taken place, but after the registration has expired. This way we control the number of active registration, bounded to the number of buckets * bucket size.
* Removing the ticket once the registration is successful: This approach removes the ticket as soon as the registration has complete. This way the number of ongoing registrations is much bigger and only depends on the time required to place a registration, that will cause the bucket space keeps occupied and no other registrations can take place meanwhile. This approach implies more registrations and therefore more overhead, but a better distribution of registration placed, especially for non popular topics and node with identifiers distant from topic id.-->


### Bucket refresh

'Ticket table' needs to be initialised and refreshed to fill up all the per-distance k-buckets.
Ideally, all k-buckets should be constantly full, meaning that the node place registrations at registrars in all distances to the topic hash.
An option to fill up all k-buckets would be to send periodic lookups for the specific distance to the topic hash, but since there are some distances that tend to be empty in the id space, sending periodic lookups for the topic hash may create an additional overhead that can be too expensive and create too much traffic in the network.
To avoid that, initially, the 'ticket table' k-buckets are filled performing local DHT routing table lookups to all distances to the 'topic hash'.
In addition to that, every time a node sends a ticket request, the registrar replies with the closest nodes to 'the topic hash' that it knows.
This helps filling up k-buckets without sending additional lookups.
Also, when performing topic search (sending lookups for specific topics), closest known nodes to 'the topic hash' are attached by the registrar node in the response.

There is also a refresh bucket process, similar to the Kademlia DHT table, where periodically a random bucket is checked for empty buckets. 
The refresh time used is `refresh_time=10 seconds`.
During the refresh process, the empty slots can be filled from the local DHT table list, and optionally a lookup (Kademlia FINDNODE) can be performed towards the topic hash.
Also, all nodes in the bucket are pinged to check they are still alive. In case they are not, tickets are removed from the table and replaced with new nodes.

### Waiting time function

Waiting time function is used to calculate the waiting time reported to registering advertising nodes to regulate and control the ticket registration. The function directly shapes the structure of the topic table, determines its diversity and performs flow control. The function should also protect against all kinds of attacks, where a malicious actor tries to exhaust resources of the registrar. At the same time, no hard limits on the registrant IPs/IDs/registered topic should be imposed, allowing the table to be used in various environments. 

An important consideration when computing waiting times is to maintain a deterministic behaviour. In other words, there should not be a randomness in the waiting time computation; otherwise, advertisers will be tempted to send multiple requests to the same registrar in an effort to obtain different (i.e., smaller) waiting times. The waiting for a specific request follows the formula below (we assume that the ads contain registrant IP, ID and topic):
```
waiting_time(ad) = base_time * max(IP_modifier, ID_modifier, topic_modifier)
base_time = `target-ad-lifetime` / topic_table_capacity
```
The base time is determined based on the lifetime of an ad and capacity of the table. Longer `target-ad-lifetime` yields longer waiting times, while increased capacity of the table yields shorter waiting time. However, the latter is compoensated by the modifiers explained below. 

```
IP_modifier = count(already_in_topic_table(ad[IP]))^5
ID_modifier = count(already_in_topic_table(ad[ID]))^5
topic_modifier = count(already_in_topic_table(ad[topic]))^2
```
All the modifiers increase with increasing number of the same items already in the table. Thus it's getting increasingly difficult to register ads for the same IP/ID/topic. For instance, ads for less popular topic will receive lower waiting times than popular ones. Note that the table does not prevent anyone from registering, but rather makes it slower for already popular items. Such a mechanism promotess diversity in the table and protects against Sybill attacks. An attacker with a limited poll of IP addresses won't be able to dominate the table. The low exponent for the `topic_modifier` is motivated by the topics in the network that are likely to follow a zipf distribution. In contast, honest nodes IPs/IDs should follow a uniform distribution. 




## Topic Search

The purpose of placing ads is being discovered by searchers. 
Searchers maintain a separate table that they use to keep track of on-going searches called the 'search table'. 
Similar to the 'ticket table', the search table also stores k-buckets of advertisement media by distance to the topic hash, and a new 'search table' is created for each topic lookup.
The k factor of the search table should be relatively large in order to make the search efficient.
By default we use `k=16` similarly to the Kademlia DHT.
Tickets are not required for search and nodes can not be added multiple times in the same k-bucket.

To find ads, the searcher simply queries the nodes in the search table for ads. In order to find new results, bucket entries are replaced when the node fails to answer or when it answers with an empty list of ads. 
Bucket entries of the search table should also be replaced whenever the table is refreshed by a lookup.

<!--How does this deal with topic popularity?
In earlier research, we kept trying to estimate the 'radius' (i.e. popularity) of the topic in order to determine the advertisement media.

I think the proposed advertisement algorithm will track popularity automatically because the cumulative waiting time required for placement just grows the closer you get to the topic hash. Nodes will keep trying to out-wait each other close to the center. Further away from the topic, waiting times will be more reasonable and everyone will be able to find their place there. When the topic shrinks, the required 'best time' will shrink also.

For search, estimation is less necessary and we should try to see how efficient it is in the way specified above. I think it might just work out.-->


### Search strategies

For the lookup process, we perform `ALPHA=3` parallel lookups to three different nodes. 
In case not enough `LOOKUP_LIMIT=50` results have been received for the first `ALPHA` lookups, additional `ALPHA` parallel lookups are performed until reaching `LOOKUP_LIMIT` or `MAX_LOOKUP_HOPS=50`.
We implemented and evaluated different search strategies in order to choose which nodes from which buckets ask first when performing a lookup.

* Minimum bucket: A random node is picked from the first non-empty bucket starting with the minimum distance to the topic hash bucket.

* Random: A random node is picked from a random bucket every time.

* All buckets: A random node is picked from a bucket following a round-robin approach. It starts picking a random node from the highest distance bucket and follows to the next distance in the bucket list.

### Bucket refresh

Similarly to 'ticket table', 'search table' needs to be initialised and refreshed to fill up all the per-distance k-buckets.
Ideally, all k-buckets should be constantly full, making it possible to query any distance to the topic hash.
Since there are some distances that tend to be empty in the id space, sending periodic lookups for the topic hash my create and additional overhead that can create too much traffic in the network.
To avoid that, initially, 'search table' k-buckets are filled performing local DHT routing table lookups to all distances to the 'topic hash'.
In addition to that, every time an advertiser sends a ticket request and when  performing topic search at a registrar, the registrar replies with the closest nodes to 'the topic hash' that it knows, helping to fill up the k-buckets of ticket tables without advertisers sending additional (Kademlia FINDNODE) lookups.

There is also a refresh process, similar to the Kademlia DHT table, where periodically a random bucket is checked for empty buckets. 
The refresh time used is `refresh_time=10 seconds`.
When empty slots during the refresh process, optionally, lookups are performed to the topic hash in case is empty.
Also, the last node in the bucket is pinged to check it is still alive. In case it is not, it is removed from the table.

<!--*The search table is initialized and refreshed by performing lookups for the topic hash on using the main node table.*-->
