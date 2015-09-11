#!/bin/sh

if [ $# -ne 1 ]; then
	echo "Usage: ./test.sh <command>"
	echo "  e.g. ./test.sh Unroll"
	exit 1
fi

CMD=$1

for file in test????.c; do
	echo "Testing $file..."
	gcc -std=c99 "$file"
	./a.out >"$file.before.txt"
	java -cp "../bin:../../edu.auburn.oaccrefac.core/bin:../lib/*" $CMD "$file" 4 >"$file.new.c"
	gcc -std=c99 "$file.new.c"
	./a.out >"$file.after.txt"
	diff "$file.before.txt" "$file.after.txt"
	if [ $? -ne 0 ]; then
		echo "Output differs for $file"
		exit 1
	fi
	rm -f "$file.new.c" "$file.before.txt" "$file.after.txt"
done
exit 0
