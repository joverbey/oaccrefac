int main() {

	int a, b, i;

	a = b;

#pragma acc data copyin(i) /*<<<<< 7,0,8,0,pass*/
	{
#pragma acc parallel loop
		for (i = 0; i < 10; i++)
			a = i;
	}

#pragma acc data copyin(a)
	{
		for (int j = 0; j < 10; j++) {
			j += a;
		}
	}

	b = a;

	while (0)
		;

	return 0;

}
