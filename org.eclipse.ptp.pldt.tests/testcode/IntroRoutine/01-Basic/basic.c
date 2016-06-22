int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,5,4,6,pass*/
        int a = foo();
    }
}

int foo() {

	return bar();
}

int bar() {
#pragma acc parallel vector
	{
#pragma acc loop
		for (int i = 0; i < 100; i++) {

		}
	}
	return 1;
}
