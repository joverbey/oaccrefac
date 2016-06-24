# FEAL4

FEAL4 is a differential cryptanalysis program. Here it is used to apply
openacc directives and measure their effect. This code was chosen because
it is more complicated than toy code and requires restructuring before
being parallelized. The parallelization process also involved testing the
oaccrefac tool on real code to see its effects.

The tool relies on some luck to fully crack an encrypted plaintext, so don't
expect all subkeys to be cracked very often. Most of the time, at least one
will be cracked.

When testing on the authors computer with sample runs, the initial file completed
in 240 seconds, the restructured file completed in 178 seconds, and the final file
completed in 2 seconds.

# Installation

The project includes several source files:
- `feal4_initial.c`: The initial source code.
- `feal4_restructured.c`: The source restructured for parallelizatoin.
- `feal4_final.c`: The parallelized source code.
- `feal4_demo.c`: The source code with a fixed seed.

To compile any of these files, run `make <FILE_NAME>` without the `.c`.
To run them, type `./feal4`. `make clean` removes the feal4 file and `make`
compiles the demo file.


# Refactoring `feal4_initial.c` with our Tool

BE CAREFUL OF CURRENT BUGS IN INTRO DATA
- copyout sets should mostly be copies for FEAL4; look at new data construct and altered parallel constructs inside as well
- current bug: parameters are not considered. in crackLastRound(), the parameter is created; should be copied in


in `crackLastRound()`:  
	
- manually declare a new boolean initialized to `false`  
- manually replace the break in the `score == numplain` if statement with an assignment of `true` to the new variable  
- with the tool, strip mine the `fakeK` loop with a strip factor of 65535, 0-based loops, and ignoring overflow  
- manually add a condition to the new outer loop that will cause it to stop iteration if the variable is `true`  
- with the tool, parallelize the new inner loop  
- with the tool, make all writes in the above if statement atomic  
- with the tool, introduce a data construct around the new outer for loop  

in `main()`:  
	
- follow a similar sequence of actions using the `guessK0` loop
- change the seed to 1461523442  


