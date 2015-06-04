/*
 * This test should demonstrate a simple refactoring
 * of a for-loop with array parameters to a function as the
 * body and an unrolling factor of 2. Furthermore, it
 * is a simpler test in that the bounds are already
 * known, no dependencies, and the upper bound is
 * divisible by the factor.
 */

static void foo(char* p) {
	//Empty for test purpose
}

int main() {

	char a[10];
    for (int i = 0; i < 10; i++) /*<<<<< 17,5,18,19,2,pass*/
        foo(a[i]);

}