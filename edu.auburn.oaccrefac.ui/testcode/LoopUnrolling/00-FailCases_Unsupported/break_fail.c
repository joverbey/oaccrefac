/*
 * This test should demonstrate a fail state
 * in which loop unrolling is unsuitable for
 * loops that break
 */
int main() {

	char a[10];
	int foo = 1;
    for (int i = 0; i < 10; i++) { /*<<<<< 10,5,13,6,2,fail*/
        if (foo) //to add depth to test case...
        	break;
    }

}
