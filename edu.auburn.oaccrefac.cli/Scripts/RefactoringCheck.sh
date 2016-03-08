#!/bin/bash
curdir=`basename "$PWD"`
if [ "$curdir" != "edu.auburn.oaccrefac.cli" ]
then
	echo "Error: This script must be run from the edu.auburn.oaccrefac.cli directory."
	exit
fi

if [ $# -eq 0 ]
then
	echo "Please enter file to be checked"
	read fileCheck
	echo "Please enter loop name to be checked"
	read loopCheck
else
	fileCheck=$1
	loopCheck=$2
fi




declare -a refacList=(DistributeLoops FuseLoops InterchangeLoops IntroduceKernelsLoop IntroduceParallelLoop LoopCutting StripMine TileLoops Unroll);
declare -a refacPossible=("false" "false" "false" "false" "false" "false" "false" "false" "false" "false");
tempholder=$(mktemp)

listCount=0
for k in "${refacList[@]}"
do
	if [ $k == "InterchangeLoops" ]; then
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$k" $fileCheck "-ln" "$loopCheck" 1 > "$tempholder" 2> /dev/null
	elif [ $k == "LoopCutting" ] || [ $k == "StripMine" ] || [ $k == "Unroll" ]; then
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$k" $fileCheck "-ln" "$loopCheck" 2 > "$tempholder" 2> /dev/null
	elif [ $k == "TileLoops" ]; then
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$k" $fileCheck "-ln" "$loopCheck" 4 3 > "$tempholder" 2> /dev/null
	else
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" "$k" $fileCheck "-ln" "$loopCheck" > "$tempholder" 2> /dev/null
	fi
	if [[ -s $tempholder ]]; then
		refacPossible[$listCount]="true"
	fi
	> "$tempholder"
	loopname=$loopnameTemp
	listCount=$((listCount+1))
done

count=0

for h in "${refacPossible[@]}"
do
	if [ $h == "true" ]
	then
		echo ${refacList[$count]}
	fi
	count=$[count + 1]

done
