/*
 * This test should demonstrate a simple refactoring
 * of a for-loop with am array assignment to array assignment
 * body and an unrolling factor of 2. Furthermore, it
 * is a simpler test in that the bounds are already
 * known, no dependencies, and the upper bound is
 * divisible by the factor.
 */
int main() {

	char a[10];
	char b[10];
    for (int i = 5; i < 10; i++) /*<<<<< 13,5,14,21,2,pass*/
        a[i] = b[i];
    return 0;
}
