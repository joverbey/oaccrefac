int main() {

	printf("test");

	int a, b, i;

	b = a;

	a = b;

#pragma acc data /*<<<<< 11,0,12,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = b;
	}

	while (0)
		;

	return 0;

}
