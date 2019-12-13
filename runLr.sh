#!/bin/bash
./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config.txt & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_level.json & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed.json & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed_quick.json & sleep 2