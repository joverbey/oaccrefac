int a, i;

int main() {

	for (i = 0; i < 10; i++)
		;

#pragma acc data copyin(i) /*<<<<< 8,0,9,0,pass*/
	{
#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;
	}

	for (i = 0; i < 10; i++)
		;

	return 0;

}
