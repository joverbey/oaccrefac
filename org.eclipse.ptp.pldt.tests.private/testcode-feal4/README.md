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
- `feal4\_initial.c`: The initial source code.
- `feal4\_restructured.c`: The source restructured for parallelizatoin.
- `feal4\_final.c`: The parallelized source code.
- `feal4\_demo.c`: The source code with a fixed seed.

To compile any of these files, run `make <FILE_NAME>` without the `.c`.
To run them, type `./feal4`. `make clean` removes the feal4 file and `make`
compiles the demo file.
