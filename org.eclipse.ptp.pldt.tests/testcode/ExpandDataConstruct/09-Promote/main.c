#include <stdio.h>

int main() {

	printf("test");

	int i, b, a;

	for (i = 0; i < 10; i++) {

#pragma acc data copyin(i) /*<<<<< 11,0,12,0,pass*/
		{
#pragma acc parallel loop
			for (i = 0; i < 10; i++)
				a = i;
#pragma acc parallel loop
			for (i = 0; i < 10; i++) {
				a = i;
			}
		}
	}

	b = 6;

	while (0)
		;

	return 0;

}
