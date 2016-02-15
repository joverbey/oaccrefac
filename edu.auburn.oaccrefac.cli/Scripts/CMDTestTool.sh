#!/bin/bash
curdir=`basename "$PWD"`
if [ "$curdir" != "edu.auburn.oaccrefac.cli" ]
then
	echo "Error: This script must be run from the edu.auburn.oaccrefac.cli directory."
	exit
fi

if [ "$COMPILE" == "" ]; then
	# COMPILE="make -C ./examples/testcode-epcc/"
	COMPILE="gcc -o examples/testcode-epcc/oa -fopenmp examples/testcode-epcc/common.c examples/testcode-epcc/level1.c examples/testcode-epcc/main.c -lm"
fi
if [ "$RUN" == "" ]; then
	RUN="./examples/testcode-epcc/oa --datasize 1024 --reps 1"
fi

outerinner[0]="outer"
outerinner[1]="inner"
outerinner[2]="inner2"

>./Scripts/errorlog.txt
>./examples/testcode-epcc-temp/outputtemp.c

cp -a ./examples/testcode-epcc/. ./examples/testcode-epcc-temp
cp ./examples/testcode-epcc/level1-CMD.c ./examples/testcode-epcc-temp/inputtemp.c

echo "Compiling..."
$COMPILE
if [ $? -ne 0 ]; then
	echo "Compilation failed"
	exit 1
fi

echo "Running..."
$RUN | tee ./Scripts/result.txt

for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops ./examples/testcode-epcc-temp/inputtemp.c "$loopname"  > ./examples/testcode-epcc/outputtemp.c 2>> ./Scripts/errorlog.txt	
		if [[ -s ./examples/testcode-epcc-temp/outputtemp.c ]]; then
			cp ./examples/testcode-epcc-temp/outputtemp.c ./examples/testcode-epcc-temp/inputtemp.c
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done

cp ./examples/testcode-epcc-temp/inputtemp.c ./examples/testcode-epcc-temp/level1-CMD.c

#How to insert refactored file(outputtemp.c) into make?
make -C ./examples/testcode-epcc-temp/
./examples/testcode-epcc-temp/oa > ./Scripts/result2.txt

if cmp -s result.txt result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi

rm ./examples/testcode-epcc-temp/outputtemp.c
rm ./examples/testcode-epcc-temp/inputtemp.c
rm ./examples/testcode-epcc-temp/oa
rm ./examples/testcode-epcc/outputtemp.c
rm ./examples/testcode-epcc/oa
rm -r ./examples/testcode-epcc-temp
rm ./examples/testcode-epcc/*.o
