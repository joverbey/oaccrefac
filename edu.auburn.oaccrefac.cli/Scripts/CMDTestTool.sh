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
>./Scripts/outputtemp.c
cp /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/level1-CMD.c /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/inputtemp.c



g++ /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/level1-CMD.c /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/inputtemp.c -o /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/runnable
/home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/runnable > ./Scripts/result.txt

for i in $(seq 77)
do
	loopname="loop"
	loopname+=$i
	loopnameTemp=$loopname
	
	for j in "${outerinner[@]}"
	do
		loopname+="$j"
		java -cp "lib/*:bin:../edu.auburn.oaccrefac.core/bin" DistributeLoops ./Scripts/inputtemp.c "$loopname"  > ./Scripts/outputtemp.c 2>> ./Scripts/errorlog.txt	
		if [[ -s ./Scripts/outputtemp.c ]]; then
			cp ./Scripts/outputtemp.c ./Scripts/inputtemp.c
		fi
		echo $loopname
		loopname=$loopnameTemp
	done
done

g++ ./Scripts/output.c -o /home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/runnable
/home/stupideclipse/Code/oaccrefac/edu.auburn.oaccrefac.ui/testcode-epcc/runnable > result2.txt

if cmp -s result.txt result2.txt
then
	echo "The results match"
else
	echo "The results did not match"
fi

rm ./Scripts/outputtemp.c
rm ./Scripts/inputtemp.c
