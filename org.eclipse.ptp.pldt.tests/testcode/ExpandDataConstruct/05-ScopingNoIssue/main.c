int main() {

	printf("test");

	int a, b, i;

#pragma acc data /*<<<<< 7,0,8,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;
	}

	while (0)
		;

#pragma acc data copyin(a)
	{
		int x = 0;
		for (int j = 0; j < 10; j++) {
			x += j;
		}
	}

	return 0;

}
