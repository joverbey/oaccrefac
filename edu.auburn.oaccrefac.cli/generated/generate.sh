#!/bin/sh
NUM_TESTS=50
SEED=0
DEPTH=2
STMTS=2
java -cp "../bin:../../edu.auburn.oaccrefac.core/bin:../lib/*" \
	LoopNestGenerator $NUM_TESTS $SEED $DEPTH $STMTS
