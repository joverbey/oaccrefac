/*
 * This test should demonstrate a fail state
 * in which the loop does not follow a supported
 * form suitable for loop unrolling.
 */
int main() {

	char a[10];
	int foo = 1;
    for (a[0] = 0; a[0] < 10; (a[0])++) { /*<<<<< 10,5,13,6,2,fail*/
        a[a[0]] = '\0';
    }
    return 0;
}
