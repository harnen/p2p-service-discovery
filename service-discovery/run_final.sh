#!/bin/bash

DEF_SIZE=1000
DEF_TOPIC=20
DEF_ZIPF=2
DEF_BUCKET_SIZE=1


SIZES='1000 5000 10000'
TOPICS='1 5 20 60'
ZIPFS='0.1 0.5 1 1.5'
#ZIPFS=''
BUCKET_SIZES='1 3 5 10'

IN_CONFIG='config/final.cfg'
OUT_CONFIG='config/tmp.cfg'


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
for SIZE in $SIZES
do
	echo running size $SIZE
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
#	./run.sh ./config/discv5ticket_search.cfg &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	grep '^SIZE' $OUT_CONFIG
done


SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
for TOPIC in $TOPICS
do
	echo running topic $TOPIC
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
#	./run.sh ./config/discv5ticket_search.cfg &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	grep '^control.0traffic.maxtopicnum' $OUT_CONFIG
done

SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
for ZIPF in $ZIPFS
do
	echo Running zipf $ZIPF
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
#	./run.sh ./config/discv5ticket_search.cfg &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	grep '^control.0traffic.zipf' $OUT_CONFIG
done

SIZE=$DEF_SIZE
TOPIC=$DEF_TOPIC
ZIPF=$DEF_ZIPF
BUCKET_SIZE=$DEF_BUCKET_SIZE
for BUCKET_SIZE in $BUCKET_SIZES
do
	echo Running bucket size: $BUCKET_SIZE
	cp $IN_CONFIG $OUT_CONFIG
	sed  -i "s/^SIZE .*$/SIZE $SIZE/g" $OUT_CONFIG 
	sed  -i "s/^control.0traffic.maxtopicnum .*$/control.0traffic.maxtopicnum $TOPIC/g" $OUT_CONFIG
	sed  -i "s/^control.0traffic.zipf .*$/control.0traffic.zipf $ZIPF/g"  $OUT_CONFIG
	sed  -i "s/^protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE .*$/protocol.3kademlia.SEARCH_TABLE_BUCKET_SIZE $BUCKET_SIZE/g" $OUT_CONFIG
#	./run.sh ./config/discv5ticket_search.cfg &> /dev/null
	mkdir -p ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp ./logs/*.csv ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	cp $OUT_CONFIG ./logs/s${SIZE}_t${TOPIC}_z${ZIPF}_b$BUCKET_SIZE
	grep '^control.3kademlia.SEARCH_TABLE_BUCKET_SIZE' $OUT_CONFIG
done
