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

![topic table](./img/topic-queue-diagram.png)

The queue size limit is implementation-defined. Implementations should place a global
limit on the number of ads in the topic table regardless of the topic queue which contains
them. Reasonable limits are 100 ads per queue and 50000 ads across all queues. Since ENRs
are at most 300 bytes in size, these limits ensure that a full topic table consumes
approximately 15MB of memory.

Any node may appear at most once in any topic queue, that is, registration of a node which
is already registered for a given topic fails. Implementations may impose other
restrictions on the table, such as restrictions on the number of IP-addresses in a certain
range or number of occurrences of the same node across queues.

### Tickets

Ads should remain in the queue for a constant amount of time, the `target-ad-lifetime`. To
maintain this guarantee, new registrations are throttled and registrants must wait for a
certain amount of time before they are admitted. When a node attempts to place an ad, it
receives a 'ticket' which tells them how long they must wait before they will be accepted.
It is up to the registrant node to keep the ticket and present it to the advertisement
medium when the waiting time has elapsed.

The waiting time constant is:

    target-ad-lifetime = 15min

The assigned waiting time for any registration attempt is determined according to the
following rules:

- When the table is full, the waiting time is assigned based on the lifetime of the oldest
  ad across the whole table, i.e. the registrant must wait for a table slot to become
  available.
- When the topic queue is full, the waiting time depends on the lifetime of the oldest ad
  in the queue. The assigned time is `target-ad-lifetime - oldest-ad-lifetime` in this
  case.
- Otherwise the ad may be placed immediately.

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
    cum-wait     = cumulative waiting time of this node

### Registration Window

The image below depicts a single ticket's validity over time. When the ticket is issued,
the node keeping it must wait until the registration window opens. The length of the
registration window is 10 seconds. The ticket becomes invalid after the registration
window has passed.

![ticket validity over time](./img/ticket-validity.png)

Since all ticket waiting times are assigned to expire when a slot in the queue opens, the
advertisement medium may receive multiple valid tickets during the registration window and
must choose one of them to be admitted in the topic queue. The winning node is notified
using a [REGCONFIRMATION] response.

Picking the winner can be achieved by keeping track of a single 'next ticket' per queue
during the registration window. Whenever a new ticket is submitted, first determine its
validity and compare it against the current 'next ticket' to determine which of the two is
better according to an implementation-defined metric such as the cumulative wait time
stored in the ticket.

### Advertisement Protocol

This section explains how the topic-related protocol messages are used to place an ad.

Let us assume that node `A` provides topic `T`. It selects node `C` as advertisement
medium and wants to register an ad, so that when node `B` (who is searching for topic `T`)
asks `C`, `C` can return the registration entry of `A` to `B`.

Node `A` first attempts to register without a ticket by sending [REGTOPIC] to `C`.

    A -> C  REGTOPIC [T, ""]

`C` replies with a ticket and waiting time.

    A <- C  TICKET [ticket, wait-time]

Node `A` now waits for the duration of the waiting time. When the wait is over, `A` sends
another registration request including the ticket. `C` does not need to remember its
issued tickets since the ticket is authenticated and contains enough information for `C`
to determine its validity.

    A -> C  REGTOPIC [T, ticket]

Node `C` replies with another ticket. Node `A` must keep this ticket in place of the
earlier one, and must also be prepared to handle a confirmation call in case registration
was successful.

    A <- C  TICKET [ticket, wait-time]

Node `C` waits for the registration window to end on the queue and selects `A` as the node
which is registered. Node `C` places `A` into the topic queue for `T` and sends a
[REGCONFIRMATION] response.

    A <- C  REGCONFIRMATION [T]
