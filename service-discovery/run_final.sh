#!/bin/bash

DEF_SIZE=1000
DEF_TOPIC=4
DEF_ZIPF=1
DEF_BUCKET_SIZE=17
DEF_BUCKET_ORDER=0

#SIZES='1000 5000 10000'
SIZES='1000 1500 2000'
TOPICS='1 4 20 60'
ZIPFS='0.1 0.5 1 1.5'
#ZIPFS=''
BUCKET_SIZES='1 3 5 10'
BUCKET_ORDERS='0 1 2'

IN_CONFIG='config/final.cfg'
OUT_CONFIG='config/tmp.cfg'


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
rm -rf logs/*
for SIZE in $SIZES
do
	echo running size $SIZE
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	grep '^SIZE' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p size_results
mv *.png size_results


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
rm -rf logs/*
for TOPIC in $TOPICS
do
	echo running topic $TOPIC
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	grep '^control.0traffic.maxtopicnum' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p topic_results
mv *.png topic_results

SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
rm -rf logs/*
for ZIPF in $ZIPFS
do
	echo Running zipf $ZIPF
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	grep '^control.0traffic.zipf' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p zipf_results
mv *.png zipf_results

SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
rm -rf logs/*
for BUCKET_SIZE in $BUCKET_SIZES
do
	echo Running bucket size: $BUCKET_SIZE
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	grep '^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p bucket_results
mv *.png bucket_results


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
BUCKET_ORDER=$DEF_BUCKET_ORDER
rm -rf logs/*
for BUCKET_ORDER in $BUCKET_ORDERS
do
	echo Running bucket order: $BUCKET_ORDER
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.LOOKUP_BUCKET_ORDER .*$/protocol.3kademlia.LOOKUP_BUCKET_ORDER $BUCKET_ORDER/g" $OUT_CONFIG
	./run.sh $OUT_CONFIG &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b${BUCKET_SIZE}_o${BUCKET_ORDER}
	grep '^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE' $OUT_CONFIG
done
rm logs/*.csv logs/*.cfg
python3 python/analyze.py logs/*
mkdir -p order_results
mv *.png order_results
