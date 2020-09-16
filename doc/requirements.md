# Requirements

## Actors

## Assumptions
We assume that no single node can be trusted, but assume an honest majority of the partiicpants. 
Note that a single malicious participant can produce multiple fake (Sybil) identities, but will be limited in the number of the IP addresses they control. 



## Requirements

* No single registrant gets eclipsed when advertising its services (topics). Aka no registrants can be globally denied registrations

* No single registrar

* User querying for certain topic discover the majority (all?) registrants within a bounded amount of time.

### Fairness

* All the registrants within the same topic have the same chance of being discovered. 
* The number of total registrations per topic is proportional to the popularity of the topic (the number of registrants).
* Load balance - no node registrar receives significantly more traffic than other (other normal circumstances). 
