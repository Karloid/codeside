#!/bin/bash
./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config.txt &
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed.json &                         #test sim config
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_level.json &
sleep 2
#java -jar ./strategy13379.jar 127.0.0.1 31002 &
java -jar ./strategy17757.jar 127.0.0.1 31002 &
#java -jar ./strategy17348.jar 127.0.0.1 31002 &
#java -jar ./strategy16835.jar 127.0.0.1 31002 &
#java -jar ./strategy16735.jar 127.0.0.1 31002 &
#java -jar ./strategy812.jar 127.0.0.1 31002 &
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_keyboard.txt & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_level.json & sleep 2
#/kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed.json & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed_quick.json & sleep 2
#./kill.sh || true && echo 'startLr' && cd ./lr && ./aicup2019 --config config_seed_empty.json & sleep 1