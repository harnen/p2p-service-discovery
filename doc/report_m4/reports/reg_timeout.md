
# Parameters evaluated

* Registrations lifetime: 0.5 * Ad lifetime, 1 * Ad lifetime, 1.5 * Ad lifetime, 2 * Ad lifetime.

* Value selected: 1 Ad lifetime.

# Results

## Active registrations
<p align="center">
  <img src="../imgs/reg_timeout/registration_origin.png" width="50%" />
</p>

## Traffic load

* Message quantity
<p align="center">
  <img src="../imgs/reg_timeout/message_quantity.png" width="50%" />
</p>

* Message distribution

<p align="center">
  <img src="../imgs/reg_timeout/messages_received2.png" width="50%" />
</p>

## Discovery

* Registrant discovery distribution

<p align="center">
  <img src="../imgs/reg_timeout/registrant_distribution.png" width="50%" />
</p>

* Time between registration to first discovery

<p align="center">
  <img src="../imgs/reg_timeout/min_time_discovery.png" width="50%" />
</p>

* Lookup hopcount

<p align="center">
  <img src="../imgs/reg_timeout/lookup_hopcount.png" width="50%" />
</p>

## Table occupancy

<p align="center">
  <img src="../imgs/reg_timeout/storage_utilisation.png" width="50%" />
</p>

# Conclusions

* We selected 1 registration lifetime as a timeout parameter for the registrations waiting time. Increase this value does not seem to increase the performance but can be a threat for dos attacks. 
