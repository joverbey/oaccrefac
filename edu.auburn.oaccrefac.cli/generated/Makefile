all: generate unroll

SOURCES=$(wildcard test????.c)
UNROLLED=$(SOURCES:.c=.unrolled.c)

generate: test0000.c

test0000.c:
	rm -f test????.c
	java -cp "../bin:../../edu.auburn.oaccrefac.core/bin:../lib/*" \
		LoopNestGenerator 100 0 3 2


unroll: $(UNROLLED)

%.unrolled.c: %.c
	@echo ============================== $^ ==============================
	gcc -std=c99 -o $(addsuffix .before, $(basename $^)) $^
	./$(addsuffix .before, $(basename $^)) >$(addsuffix .before.txt, $(basename $^))
	java -cp "../bin:../../edu.auburn.oaccrefac.core/bin:../lib/*" Unroll $^ 4 >$@
	gcc -std=c99 -o $(addsuffix .after, $(basename $^)) $@
	./$(addsuffix .after, $(basename $^)) >$(addsuffix .after.txt, $(basename $^))
	diff $(addsuffix .before.txt, $(basename $^)) \
	     $(addsuffix .after.txt, $(basename $^))
	rm -f $(addsuffix .before.txt, $(basename $^)) \
	      $(addsuffix .before, $(basename $^)) \
	      $(addsuffix .after.txt, $(basename $^)) \
	      $(addsuffix .after, $(basename $^))

clean:
	rm -f $(addsuffix .before.txt, $(basename $(SOURCES))) \
	      $(addsuffix .before, $(basename $(SOURCES))) \
	      $(addsuffix .after.txt, $(basename $(SOURCES))) \
	      $(addsuffix .after, $(basename $(SOURCES))) \
	      $(addsuffix .unrolled.c, $(basename $(SOURCES)))
