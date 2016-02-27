#!/bin/bash
curdir=`basename "$PWD"`
if [ "$curdir" != "edu.auburn.oaccrefac.cli" ]
then
	echo "Error: This script must be run from the edu.auburn.oaccrefac.cli directory."
	exit
fi

printf "Please enter the desired refactoring exactly as listed below: \nDistributeLoops\nFuseLoops\nInterchangeLoops\nIntroduceKernelsLoop\nIntroduceParrallelLoop\nLoopCutting\nStripMine\nTileLoops\nUnroll\n"

read refactoring
if [ "$refactoring" != "DistributeLoops" ] && [ "$refactoring" != "FuseLoops" ] && [ "$refactoring" != "InterchangeLoops" ] && [ "$refactoring" != "IntroduceKernelsLoop" ] && [ "$refactoring" != "IntroduceParallelloop" ] && [ "$refactoring" != "LoopCutting" ] && [ "$refactoring" != "StripMine" ] && [ "$refactoring" != "TileLoops" ] && [ "$refactoring" != "Unroll" ]; then
	echo "That is not a recognized refactoring, please enter as shown above next time"
	exit
fi

if [ "$refactoring" == "InterchangeLoops" ]; then
	echo "Enter depth of loop to exchange"
	read param1
fi
if [ "$refactoring" == "LoopCutting" ]; then
	echo "Enter number cuts"
	read param1
fi
if [ "$refactoring" == "StripMine" ]; then
	echo "Enter number of strips"
	read param1
fi
if [ "$refactoring" == "Unroll" ]; then
	echo "Enter number of rolls"
	read param1
fi	
if [ "$refactoring" == "TileLoops" ]; then
	echo "Enter number of x tiles"
	read param1
	echo "Enter number of y tiles"
	read param2
fi

fileToRefactor=""

echo "please enter a test name or custom"
read testName

if [ $testName == "level1" ]; then
	COMPILE="gcc -o examples/testcode-epcc/oa -fopenmp examples/testcode-epcc/common.c examples/testcode-epcc/level1-CMD.c examples/testcode-epcc/main.c -lm"
	RUN="./examples/testcode-epcc/oa --datasize 1024 --reps 1" > ./Scripts/result.txt
	fileToRefactor="examples/testcode-epcc/level1-CMD.c"
	numNestLoops=77
fi
if [ "$testName" == "custom" ]; then
	echo "Please enter the compile command now"
	read compileInput
	COMPILE="$commandInput"
	echo "Now enter the run command"
	read runInput
	RUN="$runInput + " > ./Scripts/result.txt
	echo "Now enter the file to be refactored"
	read fileToRefactor
	echo "How many nests of loops are there?"
	read numNestLoops

fi

declare -a outerinner=(outer inner inner2);

>./Scripts/errorlog.txt

outputtemp=$(mktemp)
inputtemp=$(mktemp)
echo $outputtemp
echo $inputtemp

cp $fileToRefactor $inputtemp

echo "Compiling..."
$COMPILE
if [ $? -ne 0 ]; then
	echo "Compilation failed"
	exit 1
fi

echo "Running..."
$RUN | tee ./Scripts/result.txt

for i in $(seq $numNestLoops)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		if [ "$refactoring" == "InterchangeLoops" ] || [ "$refactoring" == "LoopCutting" ]  || [ "$refactoring" == "StripMine" ] || [ "$refactoring" == "Unroll" ]; then
			java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$refactoring" $inputtemp "$loopname" "$param1"  > $outputtemp 2>> ./Scripts/errorlog.txt
		elif [ "$refactoring" == "TileLoops" ]; then
			java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$refactoring" $inputtemp "$loopname"  "$param1" "$param2" > $outputtemp 2>> ./Scripts/errorlog.txt
		else
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$refactoring" $inputtemp "$loopname"  > $outputtemp 2>> ./Scripts/errorlog.txt
		fi
		if [[ -s $outputtemp ]]; then
			cp $outputtemp $inputtemp
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done

DIFF=$(diff $fileToRefactor $inputtemp)
if [ "$DIFF" == "" ]; then
	echo "No loops were able to be refactored"
else
	echo "Refactoring successful"
fi

cat $inputtemp

cp $fileToRefactor $outputtemp
cp $inputtemp $fileToRefactor

$Compile
$RUN | tee ./Scripts/result2.txt

cp $outputtemp $fileToRefactor

if cmp -s ./Scripts/result.txt ./Scripts/result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi
