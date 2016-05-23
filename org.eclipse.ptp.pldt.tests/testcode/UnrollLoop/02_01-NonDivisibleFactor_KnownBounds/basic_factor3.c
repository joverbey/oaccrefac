/*
 * Test should demonstrate refactoring for non-divisible
 * loop unrolling factors.
 */
int main() {

	char a[15];
    for (int i = 0; i < 14; i++) /*<<<<< 8,5,9,21,3,pass*/
        a[i] = '\0';
    return 0;
}
