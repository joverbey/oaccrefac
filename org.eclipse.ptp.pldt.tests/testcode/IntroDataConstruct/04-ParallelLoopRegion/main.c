#include <stdio.h>

int main() {

	int i, a, b;

	b = 16; /*<<<<< 8,0,18,0,pass*/

#pragma acc parallel loop
    for (i = 0; i < 100; i++)
        a = i + b;

#pragma acc parallel loop
    for (i = 0; i < 100; i++)
        a = i + 10;

	printf("%d\n", a + 12);

	if(a == 10) {
		printf("Success!\n");
	}
	else {
		printf("Failure!");
	}

	return 0;

}