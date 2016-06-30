int main() {

	printf("test");

	int i, b, a = 10;

	for (i = 0; i < 10; i++) {

#pragma acc data copyin(i) /*<<<<< 9,0,10,0,pass*/
		{
#pragma acc parallel loop
			for (; i < 10; i++)
				a = i;
		}

		while(b == 100) {
			break;
		}

	}

	return 0;

}
