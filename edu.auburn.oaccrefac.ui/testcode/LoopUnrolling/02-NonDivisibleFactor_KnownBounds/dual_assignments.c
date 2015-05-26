/*
 * Test should demonstrate refactoring for non-divisible
 * loop unrolling factors.
 */
int main() {

	char a[15];
	char b[15];
    for (int i = 0; i < 14; i++) /*<<<<< 9,5,10,21,3,pass*/
        a[i] = b[i];

}
