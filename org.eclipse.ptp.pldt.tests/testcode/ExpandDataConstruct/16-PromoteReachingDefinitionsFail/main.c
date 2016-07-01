#include <stdio.h>

int main() {

	printf("test");

	int i, b, a;

	int *start = (int*) malloc(10 * sizeof(int));
	int *j;

	for (j = start; j < start + 10 * sizeof(int); j += sizeof(int)) {

#pragma acc data copyin(i) /*<<<<< 11,0,12,0,fail*/
		{
#pragma acc parallel loop
			for (i = 0; i < 10; i++)
				a = *j;
#pragma acc parallel loop
			for (i = 0; i < 10; i++) {
				a = *j;
			}
		}
	}

	b = 6;

	while (0)
		;

	return 0;

}
