#!/bin/sh

PWD=`pwd`
CMD="sudo docker run -ti --rm -p 8100:8100 -p 35729:35729 -v $PWD:/sumaris-app:rw sumaris-app:release"
echo "Executing: CMD"
$CMD