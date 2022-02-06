#!/bin/bash

ATTACK_TOPICS='1 3 5'
EVIL_PERCENTS='0.2'
UNIFORM=("nonUniform")

#IN_CONFIG='config/discv5dhtticket_topicattack.cfg'
IN_CONFIG='config/discv5dhtnoticket_topicattack.cfg'


function run_sim(){
    OUT_CONFIG="config/dhtnoticket_attackspam_topic${TOPIC}_${UNI}_attackerPercent${PERCENT_EVIL}.cfg"
	echo output config $OUT_CONFIG
	cp $IN_CONFIG $OUT_CONFIG
    dos2unix $OUT_CONFIG
    sed -i "s/^init.1uniqueNodeID.attackTopic .*$/init.1uniqueNodeID.attackTopic $TOPIC/g" $OUT_CONFIG
    sed -i "s/^init.1uniqueNodeID.percentEvil .*$/init.1uniqueNodeID.percentEvil $PERCENT_EVIL/g" $OUT_CONFIG
    sed -i "s/^init.1uniqueNodeID.idDistribution .*$/init.1uniqueNodeID.idDistribution $UNI/g" $OUT_CONFIG

    LOG_FILE="logs_dhtnoticket_attackTopic${TOPIC}_${UNI}_attackPercent${PERCENT_EVIL}"
    echo log file $LOG_FILE
    sed -i "s/^control.3.rangeExperiment .*$/control.3.rangeExperiment $LOG_FILE/g" $OUT_CONFIG
    ./run.sh $OUT_CONFIG &> /dev/null
}


for TOPIC in $ATTACK_TOPICS
do
    echo running attack topic $TOPIC
    for PERCENT_EVIL in $EVIL_PERCENTS
    do
        echo running percent evil $PERCENT_EVIL
        for UNI in ${UNIFORM[@]};
        do
        	echo running uniform $UNI
        	run_sim
        done
    done
done
