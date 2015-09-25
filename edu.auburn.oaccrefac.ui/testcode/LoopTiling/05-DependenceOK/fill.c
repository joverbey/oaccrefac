#include <stdio.h>
#include <stdlib.h>

int main(void) {

	int a[10][10];

	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			a[i][j] = i*10 + j;
		}
	}

	for (int i = 1; i < 10; i++) { /*<<<<< 14,1,19,1,3,2,pass*/
		for (int j = 0; j < 10; j++) {
			a[i][j] = a[i-1][j];
		}
	}

	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			printf("%d ", a[i][j]);
		}
		printf("\n");
	}

	return EXIT_SUCCESS;
}
