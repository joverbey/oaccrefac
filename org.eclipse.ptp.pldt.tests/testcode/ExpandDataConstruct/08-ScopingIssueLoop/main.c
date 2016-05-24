#include <stdio.h>

int main() {

	printf("test");

	int i, b;

	for (i = 0; i < 10; i++) {

		int a;

#pragma acc data copyin(i) /*<<<<< 13,0,14,0,pass*/
		{
#pragma acc parallel loop
			for (; i < 10; i++)
				a = i;
		}

#pragma acc parallel loop
		for(i = 0; i < 10; i++) {
			a = i;
		}

	}

	b = 6;

	while (0)
		;

	return 0;

}
