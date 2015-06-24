//LEGEND:
// Start line, Start char, End line, End char, Strip Depth, Strip Factor, Propagate Interchange, pass/fail
#include <stdio.h>
#include <stdlib.h>

int main(void) {
	int a[10][10];

	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			a[i][j] = i*10 + j;
		}
	}

	for (int i = 1; i < 10; i++) { /*<<<<< 15,1,19,3,1,2,-1,pass*/
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
