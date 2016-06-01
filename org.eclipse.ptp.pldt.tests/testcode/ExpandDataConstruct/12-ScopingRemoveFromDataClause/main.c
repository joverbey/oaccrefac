int main() {

	int i;

#pragma acc parallel
	{
		for(i = 0; i < 10; i++)
			printf("test");
	}

	int a, b = 7;

#pragma acc data copyin(b, i) copyout(a) /*<<<<< 13,0,14,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = b;
	}

	b = a;

	while (0)
		;

	return 0;

}
