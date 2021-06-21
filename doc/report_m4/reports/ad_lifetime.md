# Parameters evaluated

* Registrations lifetime: 5 min, 15 min, 30 min, 1 hour.

* Value selected: 15 min.

# Results

## Active registrations
<p align="center">
  <img src="../imgs/ad_lifetime/registration_origin.png" width="50%" />
</p>

## Traffic load

* Message quantity
<p align="center">
  <img src="../imgs/ad_lifetime/message_quantity.png" width="50%" />
</p>

* Message distribution

<p align="center">
  <img src="../imgs/ad_lifetime/messages_received2.png" width="50%" />
</p>

## Discovery

* Registrant discovery distribution

<p align="center">
  <img src="../imgs/ad_lifetime/registrant_distribution.png" width="50%" />
</p>

* Time between registration to first discovery

<p align="center">
  <img src="../imgs/ad_lifetime/min_time_discovery.png" width="50%" />
</p>

* Lookup hopcount

<p align="center">
  <img src="../imgs/ad_lifetime/lookup_hopcount.png" width="50%" />
</p>

## Table occupancy

<p align="center">
  <img src="../imgs/ad_lifetime/storage_utilisation.png" width="50%" />
</p>

# Conclusions

* Despite seeing longer registrations lifetime leads to more active registrations and better table occupancies and traffic load, in terms of overall message quantity in the simulation but also in terms of messages distribution between nodes, we believe is better to have short registrations times because this leads to a more dynamic network being easier for new nodes to register, with better distribution of discovered nodes and more resistant to sybil and dos attacks.
* For that reason, we selected 15 min registration lifetime as a configuration parameter.
