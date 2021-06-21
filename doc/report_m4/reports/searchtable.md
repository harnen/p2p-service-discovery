

# Parameters evaluated

* Search table bucket size: 3, 5, 10, 16.
* Search table bucket number: 5,10,17,256.

* Value selected: 16 bucket size, 17 buckets.

# Results


## Discovery

* Registrant discovery distribution

<p align="center">
  <img src="../imgs/search_table/bucket_size/registrant_distribution.png" width="35%" />
  <img src="../imgs/search_table/nbucket/registrant_distribution.png" width="35%" />
</p>

* Time between registration to first discovery

<p align="center">
  <img src="../imgs/search_table/bucket_size/min_time_discovery.png" width="35%" />
  <img src="../imgs/search_table/nbucket/min_time_discovery.png" width="35%" />
</p>

* Lookup hopcount

<p align="center">
  <img src="../imgs/search_table/bucket_size/lookup_hopcount.png" width="35%" />
  <img src="../imgs/search_table/nbucket/lookup_hopcount.png" width="35%" />
</p>


# Conclusions

* We selected the same values used in the Ethereum DHT, since we do not have issues with the traffic load by increasing bucket size of bucket number as we have with the ticket table, but increasing it is not going to improve the search performance.
