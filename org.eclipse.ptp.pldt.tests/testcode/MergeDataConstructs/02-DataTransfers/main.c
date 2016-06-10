int main() {
	int a[3] = { 1, 2, 3 };
	int b[3] = { 4, 5, 6 };
	int c[3] = { -1, -1, -1 };
#pragma acc data /*<<<<< 5,0,6,0,pass*/
	{
#pragma acc data
		{
#pragma acc parallel loop
			for (int i = 0; i < 3; i++) {
				a[i] = b[0];
				b[i] = c[1];
				c[i] = a[2];
			}
			for (int i = 0; i < 3; i++) {
				printf("%d", b[i]);
			}
		}
	}
#pragma acc data
	{
		int d[3] = { -1, -2, -3 };
#pragma acc parallel loop
		for (int i = 0; i < 3; i++) {
			a[i] = b[0];
			b[i] = c[1];
			c[i] = d[2];
			d[i] = a[0];
		}
		for (int i = 0; i < 3; i++) {
			printf("%d", d[i]);
		}
	}
	for (int i = 0; i < 3; i++) {
		printf("%d", a[i]);
		printf("%d", b[i]);
		printf("%d", c[i]);
	}
	return 0;
}
