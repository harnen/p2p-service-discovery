# Milestone 2 (Topic table structure) report

## Setup
The document presents a performance evaluation of the proposed topic table structure and waiting time computation discussed in the
the [new specs](https://github.com/datahop/p2p-service-discovery/blob/d67a7ccd2b4c2c6bec38f5987c99cb13ea074cdc/doc/specs.md). 

A waiting time is computed based on a function which evaluates an incoming registration based on its diversity from the existing registrations in the topic table. The diversity evaluation investigates three aspects of a registration: 
1. *Source IP address* of the registrant that issued the request.
2. *Node ID* of the registrant that issued the request.
3. The topic that the registrant desires to register. 

Corresponding to each of these three components are three **modifier functions**. Each modifier function returns a value which is higher (lower) when the diversity of the request (from the existing registrations) is worse (better). For example, an *IP modifier* computes a value based on the diversity of the source IP address of the registrant. The waiting time is computed as the multiplication of the three modifiers (see the spec for details). 

In the performance results we evaluate a single topic table using a python-based [simulator] (https://github.com/datahop/p2p-service-discovery/tree/add_ips/service-discovery/python). 

We use two types of workloads consisting of registrations from both malicious and honest nodes: 
1. **Random topic ID (Spam):** In this workload, registrations from attackers use a pool of *three IP addresses* and *ten node IDs*. The attackers attempt to register random topic IDs which are not used by honest nodes.
2. **Target topic ID (Topic Attack):** In this workload, registrations from attackers again use a pool of *three IP addresses* and *ten node IDs*. However, this time they also target a specific topic IDs that are used by honest nodes. We evaluate the cases of targeting most popular topic and least popular topic. 

In both workloads, the rate of attacker registrations is ten times the rate of registrations from honest nodes. Once their registrations are successful, both the attackers and honest nodes send a new registration (i.e., ticket) request once their ads expire. 

* Simulation time: 2000 seconds.
* Zipf distribution exponent of 2.0 for the topic distribution with 100 topics.
* No topic queue limit.
* Registration lifetime (i.e., expire after): 30 seconds.

## Random topic ID traffic results

In the first experiment, we use the spam workload where attackers use random topic IDs. We initially use a very small **topic table capacity of 25**. This is an extreme case with topic table having very little capacity.

<p align="center">
  <img src="./imgs/Spam_Cap25/spam_cap_25_occupancy" width="32%" />
  <img src="./imgs/Spam_Cap25/spam_cap_25_reqs" width="32%" />
  <img src="./imgs/Spam_Cap25/spam_cap_25_waiting" width="32%" />
</p>

In these graphs (and the ones below) the **green** lines are for the honest registrations and **red** lines are for the malicious registrations. In the leftmost plot, we observe the occupancy of the topic table from malicious (red line) and honest (green line) registrants. We observe that honest registrations dominate the storage space of the topic table. In the middle plot, we observe the acceptance rate of registrations from malicious and hones registrants. Aligned with the occupancy results, the middle plot shows that the ratio of registrations accepted from honest registrants is higher than the ratio of registrations accepted from malicious nodes. The rightmost plot demonstrates the waiting time returned to malicious and honest registrants over time. Here, we also observe that malicious nodes obtain a much higher waiting time than honest nodes due to diversity modifiers. 

The below graphs demonstrate the values computed by the three modifier functions. The top graph shows the IP modifier, the middle graph shows the node ID modifier, and the bottom graph shows the topic ID modifier values. For this workload, the registrations from the attackers exhibit a low diversity in IP address and node ID aspects, while their topic diversity is high. Because the waiting time is the multiplication of these three components, the waiting time returned to malicious registrants are still much higher. This is also because the range of values returned by the topic ID modifier is much lower by design compared to node ID and IP address modifiers. 

<p align="center">
  <img src="./imgs/Spam_Cap25/spam_cap_25_occupancy" width="80%" />
</p>

We also plot the same results with a **large topic table capacity of 1000**. In this case, we still observe that the topic table successfully limits the registrations from malicious nodes as demonstrated in the plots below.

<p align="center">
  <img src="./imgs/Spam_Cap1000/spam_cap_1000_occupancy" width="32%" />
  <img src="./imgs/Spam_Cap1000/spam_cap_1000_reqs" width="32%" />
  <img src="./imgs/Spam_Cap1000/spam_cap_1000_waiting" width="32%" />
</p>

## Target topic ID 

Here we will experiment with malicious nodes attacking the most popular topic ID instead of generating random topic IDs.

