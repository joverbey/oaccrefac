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

declare -i numNestLoops
fileToRefactor=""

echo "please enter a test name or custom"
read testName

if [ $testName == "level1" ]; then
	COMPILE="gcc -o examples/testcode-epcc/oa -fopenmp examples/testcode-epcc/common.c examples/testcode-epcc/level1-CMD.c examples/testcode-epcc/main.c -lm"
	RUN="./examples/testcode-epcc/oa --datasize 1024 --reps 1" > ./Scripts/result.txt
	fileToRefactor="examples/testcode-epcc/level1-CMD.c"
	$numNestLoops=77
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
	echo "How man nests of loops are there?"
	read numNestLoops

fi

outerinner[0]="outer"
outerinner[1]="inner"
outerinner[2]="inner2"


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
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$refactoring" $inputtemp "$loopname"  > $outputtemp 2>> ./Scripts/errorlog.txt	
		if [[ -s $outputtemp ]]; then
			cp $outputtemp $inputtemp
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done

#cat $inputtemp

cp $fileToRefactor $outputtemp
cp $inputtemp $fileToRefactor

$Compile
$RUN | tee ./Scripts/result2.txt

cp $outputtemp $fileToRefactor

if cmp -s ./Scripts/result.txt ./Scripts/result2.txt
then
	echo "\nThe results match"
else
	echo "\nThe results did not match"
fi
