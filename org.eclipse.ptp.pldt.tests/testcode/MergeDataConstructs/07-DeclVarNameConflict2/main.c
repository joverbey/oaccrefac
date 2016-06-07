int main() {
	int a[3] = { 1, 2, 3 };
	int b[3] = { 4, 5, 6 };
	int d[3] = { -1, -2, -3 };
#pragma acc data /*<<<<< 5,0,6,0,fail*/
	{
		int c[3] = {7, 8, 9};
		for (int i = 0; i < 3; i++) {
			a[i] = b[0];
			b[i] = c[1];
			c[i] = a[2];
		}
	}
#pragma acc data
	{
		int c[3] = {10, 11, 12};
		for (int i = 0; i < 3; i++) {
			a[i] = b[0];
			b[i] = c[1];
			c[i] = d[2];
			d[i] = a[0];
		}
	}
	return 0;
}
