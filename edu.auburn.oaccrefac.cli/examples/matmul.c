#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <omp.h>

#define l 2048
#define m l
#define n l

float a[l][n], b[m][l], c[m][n];

/*
 * Matrix multiplication (based on Allen & Kennedy Chapter 5)
 *
 * Compile with: gcc -O4 -funroll-loops -fopenmp -std=c99 matmul.c
 */
int main(int argc, char **argv)
{
	/* Initialize a and b to the identity matrix */
	memset(a, 0, sizeof(a));
	memset(b, 0, sizeof(a));
	for (int j = 0; j < l || j < m || j < n; j++) {
		if (j < n && j < l) {
			a[j][j] = 1.0;
		}
		if (j < l && j < m) {
			b[j][j] = 1.0;
		}
	}

	printf("Multiplying...\n");
	double start = omp_get_wtime();

	/* Matrix multiplication - naive (19.57 seconds) */
	/* Autotune */
	for (int j = 0; j < m; j++) {
		for (int i = 0; i < n; i++) {
			float t = 0.0;
			for (int k = 0; k < l; k++) {
				t += a[k][i] * b[j][k];
			}
			c[j][i] = t;
		}
	}

	/* Matrix multiplication - optimized (2.32 seconds without, 0.67 with OpenMP) */
	/* 0. Move float t declaration out of i-loop
	 * 1. Scalar expand t
	 * 2. Distribute i-loop
	 * 3. Interchange i- and k-loops
	 * 4. (Optional) Parallelize j-loop
	 */
	/*
	#pragma omp parallel for
	for (int j = 0; j < m; j++) {
		float t[n];
		for (int i = 0; i < n; i++) {
			t[i] = 0.0;
		}
		for (int k = 0; k < l; k++) {
			for (int i = 0; i < n; i++) {
				t[i] += a[k][i] * b[j][k];
			}
		}
		for (int i = 0; i < n; i++) {
			c[j][i] = t[i];
		}
	}
	*/

	double end = omp_get_wtime();

	/* Print partial result */
	for (int j = 0; j < n && j < 16; j++) {
		for (int i = 0; i < m && j < 16; i++) {
			printf("%1.1f ", c[i][j]);
		}
		printf("\n");
	}

	printf("%.2f seconds (timer resolution: %f seconds)\n", end - start, omp_get_wtick());
	return EXIT_SUCCESS;
}
