int main() {

	printf("test");

	int i, a = 10;

	for (i = 0; i < 10; i++) {

#pragma acc data copyin(i) /*<<<<< 9,0,10,0,pass*/
		{
#pragma acc parallel loop
			for (; i < 10; i++)
				a = i;
		}

		while(i == 10) {
			goto label;
		}

	}
label:
	while (0)
		;

	return 0;

}
