int main() {

	printf("test");

	int i;

	for (i = 0; i < 10; i++) {

		int a, b;

#pragma acc data copyin(i) /*<<<<< 11,0,12,0,pass*/
		{
#pragma acc parallel loop
			for (; i < 10; i++)
				a = i;
		}

		b = a;
	}

	while (0)
		;

	return 0;

}
