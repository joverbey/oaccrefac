int main() {

	int i, b;

	b = 16; /*<<<<< 6,0,19,0,fail*/
	int a;
#pragma acc parallel
	{
#pragma acc loop
    for (i = 0; i < 100; i++)
        a = i + b;

#pragma acc loop
    for (i = 0; i < 100; i++)
        a = i + 10;
	}

	printf("%d\n", a + 12);

	if(a == 10) {
		printf("Success!\n");
	}
	else {
		printf("Failure!");
	}

	return 0;

}
