#include <stdio.h>

int main() {

	printf("test");

	int i, j, b, a;

	for (i = 0; i < 10; i++)
		for (j = 0; j < 10; j++)

#pragma acc data copyin(i) /*<<<<< 12,0,13,0,pass*/
				{
#pragma acc parallel loop
			for (i = 0; i < 10; i++)
				a = i;
#pragma acc parallel loop
			for (i = 0; i < 10; i++) {
				a = i;
			}
		}

	b = 6;

	while (0)
		;

	return 0;

}
