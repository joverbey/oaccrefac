#!/bin/bash
PLDT_CLASSPATH=../org.eclipse.ptp.pldt.openacc.cli/lib/*:../org.eclipse.ptp.pldt.openacc.cli/bin:../org.eclipse.ptp.pldt.openacc.core/bin
declare -a names
names="$(java -cp $PLDT_CLASSPATH FindName "examples/testcode-epcc/level1-CMD.c")"
read -a nameArray <<<$names
thing=0
for i in "${nameArray[@]}"
do
	thing=$(($thing + 1))
done

echo $thing