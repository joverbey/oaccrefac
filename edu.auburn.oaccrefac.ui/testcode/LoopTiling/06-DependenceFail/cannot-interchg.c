#include <stdio.h>
#include <stdlib.h>

int main(void) {
	int a[10][10];

	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			a[i][j] = i*10 + j;
		}
	}

	for (int i = 0; i < 10; i++) { /*<<<<< 13,1,18,1,1,2,-1,fail */
		for (int j = 0; j < 10; j++) {
			a[i][j] = a[i][j] * 100 + a[i-1][j+1];
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
