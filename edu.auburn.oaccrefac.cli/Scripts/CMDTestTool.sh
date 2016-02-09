#!/bin/bash
curdir=`basename "$PWD"`
if [ "$curdir" != "edu.auburn.oaccrefac.cli" ]
then
	echo "Error: This script must be run from the edu.auburn.oaccrefac.cli directory."
	exit
fi

outerinner[0]="outer"
outerinner[1]="inner"
outerinner[2]="inner2"

>./Scripts/errorlog.txt
>./Scripts/output.txt
>./Scripts/outputtemp.txt
cp examples/level1-CMD.c ./Scripts/inputtemp.txt



g++ ./examples/distribute_loops.c -o ./Scripts/runnable
./Scripts/runnable > result.txt






for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops ./Scripts/inputtemp.txt "$loopname"  > ./Scripts/outputtemp.txt 2>> ./Scripts/errorlog.txt	
		if [[ -s ./Scripts/outputtemp.txt ]]; then
			cp ./Scripts/outputtemp.txt ./Scripts/output.txt
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done

g++ ./examples/distribute_loops.c -o ./Scripts/runnable
./Scriptsrunnable > result2.txt

if cmp -s result.txt result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi

rm ./Scripts/outputtemp.txt
rm ./Scripts/inputtemp.txt
