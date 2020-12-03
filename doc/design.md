# Discv5 Design

This document explains the algorithms and data structures used by the service discovery protocol for Ethereum 2 (Disv5) designed and analysed in this research project.
Previous version (Discv4) utilises a Distributed Hash Table (DHT) based on Kademlia network to discover other nodes in the network. 
Using FINDNODE queries with appropriately chosen targets, the entire DHT can be sampled by a random walk to find all other participants. 
When building a distributed application, it is often desirable to restrict the search to participants which provide a certain service. 
A simple solution to this problem would be to simply split up the network and require participation in many smaller application-specific networks. 
However, such networks are hard to bootstrap and also more vulnerable to attacks which could isolate nodes.
To this end, in Discv5 Topics are introduced. This way a single network can support multiple services advertisement differentiated by a Topic index.
Nodes maintain an extra topic table (in addition to the Kademlia neighbours table) tracking their neighbors and advertise 'topics' (supported services) to other nodes.
This table stores information of other nodes by topic advertised.
In the following we describe the specification of this new Topic or Service Discovery by describing the terms and the different mechanisms used in the protocol: Topic Table, Topic Advertisement and Topic Search.

## Terms

* A 'topic' is an identifier for a service provided by a node.
* An 'advertiser' is a node providing a service that wants to be found.
* An 'ad' is the registration of an advertiser for a topic on another node.
* An 'advertisement medium' is a node on which an ad is stored.
* A 'searcher' is a node looking for ads for a topic.

## Topic Advertisement

The topic advertisement subsystem indexes participants by their provided services. A
node's provided services are identified by arbitrary strings called 'topics'. A node
providing a certain service is said to 'place an ad' for itself when it makes itself
discoverable under that topic. Depending on the needs of the application, a node can
advertise multiple topics or no topics at all. Every node participating in the discovery
protocol acts as an advertisement medium, meaning that it accepts topic ads from other
nodes and later returns them to nodes searching for the same topic.

### Topic Table

Nodes store ads for any number of topics and a limited number of ads for each topic. The
data structure holding advertisements is called the 'topic table'. The list of ads for a
particular topic is called the 'topic queue' because it functions like a FIFO queue of
limited length. The image below depicts a topic table containing three queues. The queue
for topic `T₁` is at capacity.

![topic table](./imgs/topic-queue-diagram.png)

The queue size limit is implementation-defined. Implementations should place a global
limit on the number of ads in the topic table regardless of the topic queue which contains
them. Reasonable limits are 100 ads per queue and 50000 ads across all queues. Since ENRs
are at most 300 bytes in size, these limits ensure that a full topic table consumes
approximately 15MB of memory.

Any node may appear at most once in any topic queue, that is, registration of a node which
is already registered for a given topic fails. Implementations may impose other
restrictions on the table, such as restrictions on the number of IP-addresses in a certain
range or number of occurrences of the same node across queues.

### Advertisement Protocol

In order to place an ad, the advertiser must present a valid ticket to the advertiser.

Tickets are opaque objects issued by the advertisement medium. When the advertiser first tries to place an ad without a ticket, it receives an initial ticket and a 'waiting time' which it needs to spend. The advertiser must come back after the waiting time has elapsed and present the ticket again. When it does come back, it will either place the ad successfully or receive another ticket and waiting time.

While tickets are opaque to advertisers, they are readable by the advertisement medium. The medium uses the ticket to store the cumulative waiting time, which is sum of all waiting times the advertiser spent. Whenever a ticket is presented and a new one issued in response, the cumulative waiting time is increased and carries over into the new ticket.

All nodes act as advertisement media and keep a table of 'topic queues'. This table stores the ads. The table has a limit on the number of ads in the whole table, and also has a limit on the number of ads in each queue. Ads move through the queue at a fixed rate. When the queue is full, and the last ad expires, a new ad can be stored at the beginning of the queue. An advertiser can only have a single ad in the queue, duplicate placements are rejected.

Topic queues are subject to competition. To keep things fair, the advertisement medium prefers tickets which have the longest cumulative waiting time. In addition to the ads, each queue also keeps the current 'best ticket', i.e. the ticket with the longest cumulative waiting time. When a ticket with a better time is submitted, it replaces the current best ticket. Once an ad in the queue expires, the best ticket is admitted into the queue and the node which submitted it is notified.

Tickets cannot be used beyond their lifetime. If the advertiser does not come back after the waiting time, all cumulative waiting time is lost and it needs to start over.

To keep ticket traffic under control, an advertiser requesting a ticket for the first time gets a waiting time equal to the cumulative time of the current best ticket. For a placement attempt with a ticket, the new waiting time is assigned to be the best time minus the cumulative waiting time on the submitted ticket.

The image below depicts a single ticket's validity over time. When the ticket is issued,
the node keeping it must wait until the registration window opens. The length of the
registration window is 10 seconds. The ticket becomes invalid after the registration
window has passed.

![ticket validity over time](./imgs/ticket-validity.png)

Since all ticket waiting times are assigned to expire when a slot in the queue opens, the
advertisement medium may receive multiple valid tickets during the registration window and
must choose one of them to be admitted in the topic queue. The winning node is notified
using a [REGCONFIRMATION] response.

Picking the winner can be achieved by keeping track of a single 'next ticket' per queue
during the registration window. Whenever a new ticket is submitted, first determine its
validity and compare it against the current 'next ticket' to determine which of the two is
better according to an implementation-defined metric such as the cumulative wait time
stored in the ticket.
The above description explains the storage and placement of ads on a single medium, but advertisers need to place ads redundantly on multiple nodes in order to be found.

The advertiser keeps a 'ticket table' to track its ongoing placement attempts. This table is made up of k-buckets of logarithmic distance to the topic hash, i.e. the table stores k advertisement media for every distance step. It is sufficient to use a small value of k such as k = 3. The ticket table is initialized and refreshed by performing lookups for the topic hash using the main node table.

For every node stored in the ticket table, the advertiser attempts to place an ad on the node and keeps the latest ticket issued by that node. It also keeps references to all tickets in a priority queue keyed by the expiry time of the ticket so it can efficiently access the next ticket for which a placement attempt is due.

Nodes/tickets are removed from their ticket table bucket when the ad is placed successfully or the medium goes offline. The removed entry is replaced when the ticket table is refreshed by a lookup.

### Search Protocol
