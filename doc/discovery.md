# DEVP2P service discovery

# Node discovery

### Node discovery process

Below is described the steps of the discovery process for DEVP2P to establish RLPx connections with other peers:

* A DEVP2P node supports a configurable number of concurrent transactions. By default this number is 50. Out of the 50 connections, one third is for outgoing connections (16) and two thirds for incoming (34). Incoming connections are not controlled by users (first come first served I guess) and outgoing connections are established after the discovery process using DEVP2P DHT.
* Once a new node is started, preconfigured bootstrap nodes are stored in the local table and 3 random lookups are performed to fill up the table.
* Results from lookups are stored in a bucket size buffer ordered by distance to the target.
* Every time there is an empty slot in the connections list, a node is read from the buffer (and consumed) and a new connection is started. If the buffer is empty the process waits for the buffer to be filled again.
* If the buffer is empty a lookup to random node is performed and the buffer filled with the random walk results according to the following. The lookup initiator starts by picking `α` closest nodes to the target it knows of. The initiator then sends concurrent `FINDNODE` packets to those nodes. `α` is a system-wide concurrency parameter, such as `3`. In the recursive step, the initiator resends `FINDNODE` to nodes it has learned about from previous queries. Of the `k` nodes the initiator has heard of closest to the target, it picks `α` that it has not yet queried and resends `FINDNODE` to them. Nodes that fail to respond quickly are removed from consideration until and unless they do respond. If a round of `FINDNODE` queries fails to return a node any closer than the closest already seen, the initiator resends the find node to all of the `k` closest nodes it has not already queried. The lookup terminates when the initiator has queried and gotten responses from the `k` closest nodes it has seen.




### Discovery table

* DEVP2P uses a Kademlia Distributed hash table with 17 buckets (257 buckets in v5???) and a bucket size of 16. Nodes in the buckets are ordered per log distance (check). If the bucket is already full, candidate nodes are stored in a so-called replacement list that stores up to 10 nodes (16 in v5 -check-).
* The DHT stores node entries. Each entry contains node id and network information (IP address, etc)
* A number of checks are performed before entering the table. TBC
* Every 5 s (on average), the last node of a random bucket is pinged and replaced with a random node from the respective replacement list if it fails to respond. In contrast to buckets, the replacement list is a simple FIFO queue that evicts the last entry every time a previously unknown node is added to the list.

Note: There are currently two implementations of the node discovery DHT, one for v4 and one for v5. I suspect the v5 version is just an improved version of v5 but the main functionalitites are the same. v5 version is actually not used yet, but have to check. 


### Network messages 

The following are the network messages sent between peers to exchange discovery information:

* `WHOAREYOU`: Contains the handshake challenge.
* `PING`: It is sent during liveness checks.
* `PONG`: It is the reply to `PING`.
* `FINDNODE`: It is a query for nodes in the given bucket.
* `NODES` / `NEIGHBOURS`: `NEIGHBOURS` is the reply to `FINDNODE` in v4. In v5 `NODES` is the reply to `FINDNODE` and `TOPICQUERY`.

## Topic discovery 

### Topic discovery process

### Topic register table

### Network messages 

* `REQUESTTICKET`: Requests a ticket for a topic queue.
* `TICKET`: It is the response to `REQUESTTICKET`.
* `REGTOPIC`: Registers the sender in a topic queue using a ticket.
* `REGCONFIRMATION`: Is the reply to `REGTOPIC`.
* `TOPICQUERY`: Ask nodes for a given topic.

# PeerSim implementation todo

* Event-based vs cycle based -> Event based
* App that will simulate DEVP2P node performing lookups based on connection list occupance.
* Determine simulation parameters