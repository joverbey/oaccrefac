int main() {

	int i, a;

#pragma acc parallel
	{
		for(i = 0; i < 10; i++)
			printf("test");
	}

	int b = 7;

#pragma acc data copyin(b, i) copyout(a) /*<<<<< 13,0,14,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;
	}

	b = 7;

	while (0)
		;

	return a ^ a;

}
