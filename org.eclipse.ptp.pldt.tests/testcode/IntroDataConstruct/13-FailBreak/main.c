int main() {

	int i, a, b;

	b = 16; /*<<<<< 8,0,23,0,fail*/

	for (int i = 0; i < 10; i++) {
#pragma acc parallel
		{
#pragma acc loop
			for (i = 0; i < 100; i++) {
				a = i + b;
			}

#pragma acc loop
			for (i = 0; i < 100; i++)
				a = i + 10;
		}
		if(a > 100) {
			break;
		}
	}

	printf("%d\n", a + 12);

	if (a == 10) {
		printf("Success!\n");
	} else {
		printf("Failure!");
	}

	return 0;

}
