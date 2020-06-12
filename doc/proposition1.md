# Proposition 1

This is a proposition for topic registration in Ethereum DHT. 

The registrant tries to register on every node during its random walk towards H(t) and let the nodes on the path decide whether to accept it or not.

The decision could be taken based on the node's distance from H(t) (the closer, the higher chance of accepting) and the current number of registration among all the topics (the more registrations, the lower chance of accepting). It would create a balanced system where, if there are not many registrations in the system, nodes would accept any registration, but when the traffic is increasing, each node would start to specialize in topics close to its own ID.

If the registrant finds that it doesn't have incoming connection, it can repeat the process using alternative paths (or repeat on the same one if the registration decision is made nondeterministic).

The register message can be attached to FindNode message. The registrant tries to perform the regular DHT node lookup towards `H(t)`. At te same time, it would try to register on every node it queries. 


## Questions

* Should nodes involved in a topic also keep information about other nodes in the topic? So that if you find one, you should be able to find others?
* Should we have some replication? Or only rely on registrations performed during the random walk?
* Should we use multiple hash functions (or different salt)? If we use different hash functions, we'd have different `H(t)` for the same topic. It i) provide necessary redundancy (if the node close to `H(t)` is down, we don't loose all the information) ii) we could easily perform parallel lookups (similar to alpha, but here, it would go to different instead of the same `H(t)`
* Spamming more registration shouldn't result in more state created on different nodes - this is necessary to prevent spamming attacks.
