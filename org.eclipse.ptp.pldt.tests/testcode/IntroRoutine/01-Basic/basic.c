int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,5,4,6,pass*/
        int a = foo();
    }
}

int foo() {
#pragma acc parallel
	{
#pragma acc loop gang
		for (int i = 0; i < 100; i++) {

		}
	}
	return bar();
}

int bar() {
	return 1;
}
