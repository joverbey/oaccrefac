#include <stdio.h>
#include <stdlib.h>

int main(void) {
	int a[10][10], i, j;

#pragma acc parallel loop private(i)
	for (i = 0; i < 10; i++) { /*<<<<< 8, 1, 14, 1, 1, pass */
#pragma acc loop private(j)
		for (j = 0; j < 10; j++) {
			a[i][j] = i*10 + j;
		}
	}

	for (i = 0; i < 10; i++) {
		for (j = 0; j < 10; j++) {
			printf("%d ", a[i][j]);
		}
		printf("\n");
	}
	return EXIT_SUCCESS;
}
