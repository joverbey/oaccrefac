/*
 * Test should demonstrate refactoring for non-divisible
 * loop unrolling factors.
 */

static void foo(char* p) {
	//Empty for test purpose
}

int main() {

	char a[15], b[15];
    for (int i = 0; i < 14; i++) /*<<<<< 13,5,14,19,3,pass*/
        b[i] = a[i];
    return 0;
}
