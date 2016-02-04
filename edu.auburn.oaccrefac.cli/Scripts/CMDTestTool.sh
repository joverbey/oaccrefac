#!/bin/bash
cd ~/Code/oaccrefac/edu.auburn.oaccrefac.cli/

outerinner[0]="outer"
outerinner[1]="inner"
outerinner[2]="inner2"

> ./Scripts/errorlog.txt
> ./Scripts/output.txt

for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops examples/level1-CMD.c "$loopname"  > ./Scripts/output.txt 2>> ./Scripts/errorlog.txt
		echo "Exit code is $?"
		echo $loopname
		loopname=$loopnameTemp
	done
done


