#include <stdio.h>

int main() {

	printf("test");

	int i, b;

	for (i = 0; i < 10; i++) {

		int a;

#pragma acc data copyin(i)
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
