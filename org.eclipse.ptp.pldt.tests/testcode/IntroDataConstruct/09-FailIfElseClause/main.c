#include <stdio.h>

int main() {

	int i, a, b;

	b = 16; /*<<<<< 14,6,18,3,fail*/

	if (b == 16) {
#pragma acc parallel loop
		for (i = 0; i < 100; i++)
			a = i + b;
	}
	else {
#pragma acc parallel loop
		for (i = 0; i < 100; i++)
			a = i + 10;
	}
	printf("%d\n", a + 12);

	if (a == 10) {
		printf("Success!\n");
	} else {
		printf("Failure!");
	}

	return 0;

}
