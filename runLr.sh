#!/bin/bash
./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config.txt & sleep 2
