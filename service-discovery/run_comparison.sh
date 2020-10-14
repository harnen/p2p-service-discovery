#!/bin/bash

mkdir -p ./logs/noticket/
echo "Running proposal"
./run.sh ./config/discv5noticket.cfg &> lognoticket
cp ./logs/*.csv ./logs/noticket/

mkdir -p ./logs/ticket/
echo "Running ticket"
./run.sh ./config/discv5ticket.cfg &> logticket
cp ./logs/*.csv ./logs/ticket/

python3 ./python/analyze.py ./logs/noticket/ ./logs/ticket/
