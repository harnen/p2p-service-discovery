# Milestone 4 - Performance evaluation / configation parameters report

## Intro
The document presents a security analysis of the proposed discovery protocol for the Ethereum  P2P network (discv5) following the described [specs](https://github.com/datahop/p2p-service-discovery/blob/d67a7ccd2b4c2c6bec38f5987c99cb13ea074cdc/doc/specs.md).


## Setup (default parameters)

* Network size: 2000 nodes
* Simulation time: 4h
* Kademlia Bucket size: 16
* Kademlia buckets: 16
* Ticket table bucket size: 3
* Ticket table buckets: 10
* Evil node Ticket table bucket size: 100
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

### Registration lifetime

### Ticket table parameters

### Search table parameters

### Topic table size

## Other evaluation

### Network size 

### Search strategies

### Security evaluation

## Conclusions
