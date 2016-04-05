#include <stdio.h>

static float matrix1[10][10], matrix2[10][10];

static void init();
static void print();

int main() {
	init();
	// AUTOTUNE
	for (int i = 1; i < 9; i++) {
		for (int j = 1; j < 9; j++) {
			for (int k = 1; k < 9; k++) {
				matrix1[j][0] = matrix2[k + 1][i + 1] + matrix2[0][j + 1];
				matrix2[j + 1][0] = 1.0;
			}
		}
	}
	print();
}

static void init() {
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			matrix1[i][j] = 10 * i + j;
			matrix2[i][j] = -10 * j - i;
		}
	}
}

static void print() {
	printf("matrix1 =");
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			printf(" %2.2f", matrix1[i][j]);
		}
		printf("\n         ");
	}
	printf("\n");
	printf("matrix2 =");
	for (int i = 0; i < 10; i++) {
		for (int j = 0; j < 10; j++) {
			printf(" %2.2f", matrix2[i][j]);
		}
		printf("\n         ");
	}
	printf("\n");
}
