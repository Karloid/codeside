#!/bin/bash
ps aux | grep -ie aicup2019 | awk '{print $2}' | xargs kill -9 || (true)
