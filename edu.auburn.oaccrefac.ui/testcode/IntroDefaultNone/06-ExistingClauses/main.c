int main() {

	int a;
#pragma acc parallel copyin(a) copyout(a)
	{
		for (int i = 0; i < 100; i++)/*<<<<< 4,0,5,0,pass*/
			;
	}

}
