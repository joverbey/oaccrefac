int main() {

	int i, a, b;

	b = 16; /*<<<<< 6,0,20,0,fail*/

#pragma acc parallel
	{
#pragma acc loop
    for (i = 0; i < 100; i++) {
        a = i + b;
        goto loop;
    }

#pragma acc loop
loop:
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
