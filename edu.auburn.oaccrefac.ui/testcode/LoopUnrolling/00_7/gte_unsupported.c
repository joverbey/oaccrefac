/*
 * This test should demonstrate a fail state
 * in which the loop does not follow a supported
 * form suitable for loop unrolling.
 */
int main() {

	char a[40];
	int foo = 1;
    for (int i = 20; i >= 10; i--) { /*<<<<< 10,5,12,6,2,fail*/
        a[i] = '\0';
    }

}
