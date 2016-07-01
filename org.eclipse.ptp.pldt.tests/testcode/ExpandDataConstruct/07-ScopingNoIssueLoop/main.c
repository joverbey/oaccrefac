int main() {

	printf("test");

	int i;

	for (i = 0; i < 10; i++) {

		int a = 10;
		int b;

#pragma acc data copyin(i) /*<<<<< 12,0,13,0,pass*/
		{
#pragma acc parallel loop
			for (; i < 10; i++)
				a = i;
		}

		b = 7;
	}

	while (0)
		;

	return 0;

}
