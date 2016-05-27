#include <stdio.h>

int main() {
	int a[3] = { 1, 2, 3 };
	int b[3] = { 4, 5, 6 };
	int c[3] = { -1, -1, -1 };

		c[0] = 7; /*<<<<< 8,0,34,0,pass*/
		c[1] = 8;
		c[2] = 9;
#pragma acc data copyin(c)
		{
			b[0] = 0;
			b[1] = 2;
			b[2] = 4;
#pragma acc parallel loop
			for (int i = 0; i < 3; i++) {
				a[i] = c[2];
				c[i] = a[2];
				b[i] = b[i] + 1;
			}
			b[0] = a[1];
		}

#pragma acc parallel loop
		for (int i = 0; i < 3; i++) {
			a[i] = b[0];
			c[i] = a[i];
		}

	for(int i = 0; i < 3; i++) {
		printf("%d %d", a[i], b[i]);
	}

	return 0;

}
