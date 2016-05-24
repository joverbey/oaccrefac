#!/bin/bash

PLDT_CLASSPATH=../org.eclipse.ptp.pldt.openacc.cli/lib/*:../org.eclipse.ptp.pldt.openacc.cli/bin:../org.eclipse.ptp.pldt.openacc.core/bin

#TODO Throw error if not C file
curdir=`basename "$PWD"`
if [ "$curdir" != "org.eclipse.ptp.pldt.tests.private" ]
then
	echo "Error: This script must be run from the org.eclipse.ptp.pldt.tests.private directory."
	exit
fi

printf "Please enter the desired refactoring exactly as listed below: \nDistributeLoops\nFuseLoops\nInterchangeLoops\nIntroduceKernelsLoop\nIntroduceParallelLoop\nLoopCutting\nStripMine\nTileLoops\nUnroll\nExpandDataConstruct\n"

read refactoring
if [ "$refactoring" != "DistributeLoops" ] && [ "$refactoring" != "FuseLoops" ] && [ "$refactoring" != "InterchangeLoops" ] && [ "$refactoring" != "IntroduceKernelsLoop" ] && [ "$refactoring" != "IntroduceParallelLoop" ] && [ "$refactoring" != "LoopCutting" ] && [ "$refactoring" != "StripMine" ] && [ "$refactoring" != "TileLoops" ] && [ "$refactoring" != "Unroll" ] && [ "$refactoring" != "ExpandDataConstruct" ]; then
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

echo -e "please enter one of the following test names:\nlevel1\nfeal4\ncustom"
read testName

declare -a nameArray

if [ $testName == "level1" ]; then
	COMPILE="gcc -std=c99 -o examples/testcode-epcc/oa -fopenmp examples/testcode-epcc/common.c examples/testcode-epcc/level1-CMD.c examples/testcode-epcc/main.c -lm"
	RUN="./examples/testcode-epcc/oa --datasize 1024 --reps 1" > ./Scripts/result.txt
	fileToRefactor="examples/testcode-epcc/level1-CMD.c"
	names="$(java -cp $PLDT_CLASSPATH FindName "examples/testcode-epcc/level1-CMD.c")"
	read -a nameArray <<<$names
fi
if [ $testName == "feal4" ]; then
	COMPILE="gcc -std=c99 testcode-feal4/feal4.c -o testcode-feal4/oa"
	RUN="./testcode-feal4/oa --datasize 1024 --reps 1" > ./Scripts/result.txt
	fileToRefactor="testcode-feal4/feal4.c"
	names="$(java -cp $PLDT_CLASSPATH FindName "testcode-feal4/feal4.c")"
	read -a nameArray <<<$names
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
	names="$(java -cp $PLDT_CLASSPATH FindName $filetoRefactor)"
	read -a nameArray <<<$names
fi

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
> ./Scripts/errorlog.txt
thing=0
for i in "${nameArray[@]}"
do
	echo $i >> ./Scripts/errorlog.txt
	if [ "$refactoring" == "InterchangeLoops" ] || [ "$refactoring" == "LoopCutting" ]  || [ "$refactoring" == "StripMine" ] || [ "$refactoring" == "Unroll" ]; then
		java -cp $PLDT_CLASSPATH Main "$refactoring" "$param1" $inputtemp "-ln" "$i"  > $outputtemp 2>> ./Scripts/errorlog.txt
	elif [ "$refactoring" == "TileLoops" ]; then
		java -cp $PLDT_CLASSPATH Main "$refactoring" "$param1" "$param2" $inputtemp "-ln" "$i" > $outputtemp 2>> ./Scripts/errorlog.txt
	else
		java -cp $PLDT_CLASSPATH Main "$refactoring" $inputtemp "-ln" "$i"  > $outputtemp 2>> ./Scripts/errorlog.txt
	fi
	if [[ -s $outputtemp ]]; then
		thing=$(($thing + 1))
		cp $outputtemp $inputtemp
	fi
	echo $i
done

DIFF=$(diff $fileToRefactor $inputtemp)
if [ "$DIFF" == "" ]; then
	echo "No loops were able to be refactored"
else
	echo "Refactoring successful"
fi

#cat $inputtemp

cp $fileToRefactor $outputtemp
cp $inputtemp $fileToRefactor

echo "Compiling..."
$COMPILE
echo "Do not turn off while Running"
echo "Running..."
$RUN | tee ./Scripts/result2.txt

cp $outputtemp $fileToRefactor
if cmp -s ./Scripts/result.txt ./Scripts/result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi
echo $thing