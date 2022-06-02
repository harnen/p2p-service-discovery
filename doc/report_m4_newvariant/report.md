# Milestone 4 - Performance evaluation / configation parameters report

## Intro
The document shows the results of an extensive evaleation for the Ethereum  P2P network (discv5), following the described [specs](https://github.com/datahop/p2p-service-discovery/blob/d67a7ccd2b4c2c6bec38f5987c99cb13ea074cdc/doc/specs.md), to test multiple configuration parameters and evaluate which ones perform the best. 

## Setup (default parameters)

* Network size: 2000 nodes
* Simulation time: 4h
* Kademlia Bucket size: 16
* Kademlia buckets: 16
* Ticket table bucket size: 5
* Ticket table buckets: 10
* Spam attack Evil node Ticket table bucket size: 100
* Search table bucket size: 16
* Search table buckets: 10
* Registration lifetime: 5 minutes
* Topics: 5
* Zipf exp: 0.7
* Lookup bucket strategy - all_bucket
* IP used in the attack [1,10,50]
* Malicious node % [5,10,20]

* Nodes for Topic: t1: 2000, t2 1272, t3: 803, t4: 496, t5: 218

## Configuration parameters evaluation

### Waiting time parameters

[Waiting time evaluation](reports/waiting_time.md)

### Registration lifetime

[Registration lifetime evaluation](reports/ad_lifetime.md)

### Ticket table parameters

[Ticket table parameters evaluation](reports/tickettable.md)

### Search table parameters

[Search table parameters evaluation](reports/searchtable.md)

### Topic table capacity

[Topic table size evaluation](reports/topictable.md)

### Registration Timeout

[Registration Timeout evaluation](reports/reg_timeout.md)

## Other evaluation

### Network size 

[Network size evaluation](reports/network_size.md)


### Security evaluation

[Security evaluation](reports/security_report.md)

## Conclusions

* Selected parameters:
  - Waiting time: Topic modifier exp: 15, Id Modifier exp: 0.4, Ip modifier exp: 0.4, Occupancy power: 4, Basemultiplier: 30.
  - Registration ad lifetime: 15 min.
  - Ticket table: 10 buckets, bucket size 5.
  - Search table: 17 buckets, bucket size 16.
  - Topic table capacity: 500.
  - Registration lifetime: 1*Adlifetime.

* The system performs well in worst case scenarios (e.g. big network size (10000 nodes), or under topic/hybrid sybil attacks.
