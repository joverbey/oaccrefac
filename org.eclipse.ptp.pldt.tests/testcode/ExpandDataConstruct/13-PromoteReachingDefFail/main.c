#include <stdio.h>

int main() {

	printf("test");

	int a, b, j, i;

	for (j = 0; j < 10; j++) {

#pragma acc data copyin(i) /*<<<<< 11,0,12,0,pass*/
		{
#pragma acc parallel loop
			for (i = 0; i < 10; i++)
				a = i + b;
#pragma acc parallel loop
			for (i = 0; i < 10; i++) {
				a = i + j;
			}
		}
	}

	b = 6;

	while (0)
		;

	return 0;

}
