int a, i;

int main() {

	for (i = 0; i < 10; i++)
		;

#pragma acc data copyin(a)
	{
		for (int j = 0; j < 10; j++) {
			j += a;
		}
	}

	switch (a) {
	default:
		break;
	}

	while (0)
		;

#pragma acc data copyin(i) /*<<<<< 23,0,24,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;
	}

	for (i = 0; i < 10; i++)
		;

	return 0;

}
