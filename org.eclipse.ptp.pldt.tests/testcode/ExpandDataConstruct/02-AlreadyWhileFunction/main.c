int a, i;

int main() {

#pragma acc data copyin(i) /*<<<<< 5,0,6,0,pass*/
	{

		for (i = 0; i < 10; i++)
			;

#pragma acc parallel loop
		for (; i < 10; i++)
			a = i;

		for (i = 0; i < 10; i++)
			;

	}

	return 0;

}
