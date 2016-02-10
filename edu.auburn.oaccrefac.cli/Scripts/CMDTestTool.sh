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
>./Scripts/output.c
>./examples/testcode-epcc/outputtemp.c
cp ./examples/testcode-epcc/level1-CMD.c ./examples/testcode-epcc/inputtemp.c



make -C ./examples/testcode-epcc/
./examples/testcode-epcc/oa > ./Scripts/result.txt

for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops ./examples/testcode-epcc/inputtemp.c "$loopname"  > ./examples/testcode-epcc/outputtemp.c 2>> ./Scripts/errorlog.txt	
		if [[ -s ./Scripts/outputtemp.c ]]; then
			cp ./examples/testcode-epcc/outputtemp.c ./Scripts/inputtemp.c
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done



g++ ./examples/testcode-epcc/output.c -o ./examples/testcode-epcc/runnable
./examples/testcode-epcc/runnable > result2.txt

if cmp -s result.txt result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi

rm ./Scripts/outputtemp.c
rm ./Scripts/inputtemp.c
rm ./examples/testcode-epcc/runnable
rm ./examples/testcode-epcc/outputtemp.c
