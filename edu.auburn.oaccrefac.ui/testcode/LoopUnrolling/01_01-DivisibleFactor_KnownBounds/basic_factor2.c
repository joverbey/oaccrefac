/*
 * This test should demonstrate a simple refactoring
 * of a for-loop with an full-array-assignment as the
 * body and an unrolling factor of 2. Furthermore, it
 * is a simpler test in that the bounds are already
 * known, no dependencies, and the upper bound is
 * divisible by the factor.
 */
int main() {

	char a[10];
    for (int i = 0; i < 10; i++) /*<<<<< 12,5,13,21,2,pass*/
        a[i] = '\0';
    return 0;
}
