#!/bin/bash
cd ~/Code/oaccrefac/edu.auburn.oaccrefac.cli/

outerinner[0]="outer"
outerinner[1]="inner"
outerinner[2]="inner2"

for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	for j in outerinner
	do
		loopname+=${outerinner[$j]}
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops examples/level1-CMD.c $loopname
		echo $loopname
		loopname=$loopnameTemp
	done
done


