int main() {

	int i, b;

	b = 16; /*<<<<< 6,0,19,0,pass*/
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

	return 0;

}
