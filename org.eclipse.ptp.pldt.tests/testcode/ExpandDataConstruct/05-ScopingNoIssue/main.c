int main() {

	printf("test");

	int a, b, i;

#pragma acc data /*<<<<< 7,0,8,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;
	}

	b = a;

	while (0)
		;

	return 0;

}
