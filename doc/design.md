# Discv5 Design

This document explains the algorithms and data structures used by the service discovery protocol for Ethereum 2 (Disv5) designed and analysed in this research project.
Previous version (Discv4) utilises a Distributed Hash Table (DHT) based on Kademlia network to discover other nodes in the network. 
Using FINDNODE queries with appropriately chosen targets, the entire DHT can be sampled by a random walk to find all other participants. 
When building a distributed application, it is often desirable to restrict the search to participants which provide a certain service. 
A simple solution to this problem would be to simply split up the network and require participation in many smaller application-specific networks. 
However, such networks are hard to bootstrap and also more vulnerable to attacks which could isolate nodes.
To this end, in Discv5 Topics are introduced. This way a single network can support multiple services advertisement differentiated by a Topic index.

The topic advertisement subsystem indexes participants by their provided services. 
A node's provided services are identified by arbitrary strings called 'topics'. 
A node providing a certain service is said to 'place an ad' for itself when it makes itself
discoverable under that topic.
Depending on the needs of the application, a node can advertise multiple topics or no topics at all. 
Every node participating in the discovery protocol acts as an 'advertisement medium', meaning that it accepts topic ads from other nodes and later returns them to nodes searching for the same topic, keeping an extra topic table (in addition to the Kademlia neighbours table) tracking their neighbors by topic index.

In the following we describe the specification of this new Topic or Service Discovery by describing the terms and the different mechanisms used in the protocol: Topic Table, Topic Advertisement and Topic Search.

## Terms

* A 'topic' is an identifier for a service provided by a node.
* An 'advertiser' is a node providing a service that wants to be found.
* An 'ad' is the registration of an advertiser for a topic on another node.
* An 'advertisement medium' is a node on which an ad is stored.
* A 'searcher' is a node looking for ads for a topic.

## Topic Table

Advertisement mediums store ads for any number of topics and a limited number of ads for each topic. The
data structure holding advertisements in a 'topic table'. 
The list of ads for a particular topic is called the 'topic queue' because it functions like a FIFO queue of
limited length. 
The image below depicts a topic table containing three queues. 
The queue for topic `T₁` is at capacity.

![topic table](./imgs/topic-queue-diagram.png)

The queue size limit is implementation-defined. 
Implementations should place a global limit on the number of ads in the topic table regardless of the topic queue which contains them.
Reasonable limits are 100 ads per queue and 50000 ads across all queues. 
Since ENRs are at most 300 bytes in size, these limits ensure that a full topic table consumes approximately 15MB of memory.

Any node may appear at most once in any topic queue, that is, registration of a node which is already registered for a given topic fails. 
Also, implementations may impose other restrictions on the table, such as restrictions on the number of IP-addresses in a certain
range or number of occurrences of the same node across queues, similar to limitations of different neighbours in the same k-bucket to avoid sybil attacks.

## Topic Advertisement

All nodes act as advertisement media and keep a table of 'topic queues'. 
This table stores the ads and they are stored during `target-ad-lifetime`. When the `target-ad-lifetime` expires, the ad is removed from the queue and new ads can be accepted.
The table has a limit on the number of ads in the whole table, and also has a limit on the number of ads in each queue. 
Ads move through the queue at a fixed rate. When the queue is full, and the last ad expires, a new ad can be stored at the beginning of the queue. An advertiser can only have a single ad in the queue, duplicate placements are rejected.
In order to place an ad, the advertiser must present a valid ticket to the advertiser.


#### Ticket Registration

Tickets are opaque objects issued by the advertisement medium. 
When the advertiser first tries to place an ad without a ticket, it receives an initial ticket and a 'waiting time' which it needs to spend. 
The advertiser must come back after the waiting time has elapsed and present the ticket again. 
When it does come back, it will either place the ad successfully or receive another ticket and waiting time.

Enforcing this time limit prevents misuse of the topic index because any topic must be important enough to outweigh the cost of waiting. 
Imagine a group phone call: announcing the participants of the call using topic advertisement isn't a good use of the system because the topic exists only for a short time and will have very few participants. 
The waiting time prevents using the index for this purpose because the call might already be over before everyone could get registered.
Also, it prevents attackers overflowing topic indexes by regulating registrations in case of spamming attacks.

While tickets are opaque to advertisers, they are readable by the advertisement medium. 
The medium uses the ticket to store the cumulative waiting time, which is sum of all waiting times the advertiser spent. 
Whenever a ticket is presented and a new one issued in response, the cumulative waiting time is increased and carries over into the new ticket.
Topic queues are subject to competition. 
To keep things fair, the advertisement medium prefers tickets which have the longest cumulative waiting time when multiple tickets are received but not enough room in the topic table for them.
In addition to the ads, each queue also keeps the current 'best ticket', i.e. the ticket with the longest cumulative waiting time. 
When a ticket with a better time is submitted, it replaces the current best ticket. Once an ad in the queue expires, the best ticket is admitted into the queue and the node which submitted it is notified.

Tickets cannot be used beyond their lifetime. If the advertiser does not come back after the waiting time, all cumulative waiting time is lost and it needs to start over.

To keep ticket traffic under control, an advertiser requesting a ticket for the first time gets a waiting time equal to the cumulative time of the current best ticket. For a placement attempt with a ticket, the new waiting time is assigned to be the best time minus the cumulative waiting time on the submitted ticket.

The image below depicts a single ticket's validity over time. When the ticket is issued, the node keeping it must wait until the registration window opens. The length of the registration window is implementation dependent, but by default `10 seconds` is used. The ticket becomes invalid after the registration window has passed.

![ticket validity over time](./imgs/ticket-validity.png)

#### Ticket Table

The above description explains the storage and placement of ads on a single medium, but advertisers need to place ads redundantly on multiple nodes in order to be found. 

The advertiser keeps a 'ticket table' per topic advertised to track its ongoing placement attempts.
This table is made up of k-buckets of logarithmic distance to the topic hash, i.e. the table stores k advertisement media for every distance step. 
It is sufficient to use a small value of k such as `k=3`. 
In order to avoid having empty buckets, each k-bucket has a replacement list (backup nodes stored in a second list that are used when an empty solt in the k-bucket) with `replacements_size=10`
For every node stored in the ticket table, the advertiser attempts to place an ad on the node and keeps the latest ticket issued by that node. It also keeps references to all tickets in a priority queue keyed by the expiry time of the ticket so it can efficiently access the next ticket for which a placement attempt is due.

Nodes/tickets are removed from their ticket table bucket when the ad is placed successfully or the medium goes offline. The removed entry is replaced when the ticket table is refreshed by a lookup.

#### Bucket refresh

'Ticket table' needs to be initialised and refreshed to fill up all the per-distance k-buckets.
Ideally, all k-buckets should be constantly full, meaning that the node has active registrations in 'advertising medium' in all distances to the topic hash.
Since there are some distances that tend to be empty in the id space, sending periodic lookups for the topic hash my create and additional overhead that can be too expensive and create too much traffic in the network.
To avoid that, initially, the 'ticket table' k-buckets are filled performing local DHT routing table lookups to all distances to the 'topic hash'.
In addition to that, every time a node sends a ticket request, the 'advertising medium' replies with the closest nodes to 'the topic hash' that it knows.
This helps filling up k-buckets without sending additional lookups.
Also, when performing topic searchs (sending lookups for specific topics), closest nodes to 'the topic hash' are attached in the response.

There is also a refresh process, similar to the Kademlia DHT table, where periodically a random bucket is checked for empty buckets. 
The refresh time used is `refresh_time=10 seconds`.
When empty slots during the refresh process, they can be filled from the replacement list and, optionally, perform lookups to the topic hash in case is empty.

## Topic Search

The purpose of placing ads is being discovered by searchers.

Searchers on a topic also keep a table, the 'search table'. 
Like the 'ticket table', this table also stores k-buckets of advertisement media by distance to the topic hash, and a new 'search table' is created for each topic lookup.
The k factor of the search table should be relatively large in order to make the search efficient.
By default we use `k=16` similarly to the Kademlia DHT.
Tickets are not required for search and nodes can not be added multiple times after in the same k-bucket.

To find ads, the searcher simply queries the nodes in the search table for ads. In order to find new results, bucket entries are replaced when the node fails to answer or when it answers with an empty list of ads. 
Bucket entries of the search table should also be replaced whenever the table is refreshed by a lookup.

<!--How does this deal with topic popularity?
In earlier research, we kept trying to estimate the 'radius' (i.e. popularity) of the topic in order to determine the advertisement media.

I think the proposed advertisement algorithm will track popularity automatically because the cumulative waiting time required for placement just grows the closer you get to the topic hash. Nodes will keep trying to out-wait each other close to the center. Further away from the topic, waiting times will be more reasonable and everyone will be able to find their place there. When the topic shrinks, the required 'best time' will shrink also.

For search, estimation is less necessary and we should try to see how efficient it is in the way specified above. I think it might just work out.-->


#### Search strategies

For the lookup process, we perform `ALPHA=3` parallel lookups to three different nodes. 
In case not enough `TOPIC_PEER_LIMIT` results have been received for the first `ALPHA` lookups, additional  `ALPHA` parallel lookups are performed until reaching `TOPIC_PEER_LIMIT` or `MAX_LOOKUP_HOPS`.
We implemented and evaluated different search strategies in order to choose which nodes from which buckets ask first when performing a lookup.

* Minimum bucket: A random node is picked from the non-empty bucket with the minimum distance to the topic hash.

* Random: A random node is picked from a random bucket. This can be performed in two ways: 
  1 - A random node is picked from all nodes in the table. 
  2 - A random node is picked from a random bucket each time, till all buckets have been used.

#### Bucket refresh

Similarly to 'ticket table',  'search table' needs to be initialised and refreshed to fill up all the per-distance k-buckets.
Ideally, all k-buckets should be constantly full, making it possible to query any distance to the topic hash.
Since there are some distances that tend to be empty in the id space, sending periodic lookups for the topic hash my create and additional overhead that can create too much traffic in the network.
To avoid that, initially, 'search table' k-buckets are filled performing local DHT routing table lookups to all distances to the 'topic hash'.
In addition to that, every time a node sends a ticket request and when  performing topic searchs, the 'advertising medium' replies with the closest nodes to 'the topic hash' that it know, helping filling up k-buckets without sending additional lookups.

There is also a refresh process, similar to the Kademlia DHT table, where periodically a random bucket is checked for empty buckets. 
The refresh time used is `refresh_time=10 seconds`.
When empty slots during the refresh process, they can be filled from the replacement list and, optionally, perform lookups to the topic hash in case is empty.

<!--*The search table is initialized and refreshed by performing lookups for the topic hash on using the main node table.*-->

