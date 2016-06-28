int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,fail*/
        int a = bar();
    }
}

int foo() {
#pragma acc parallel
	{
#pragma acc loop
		for (int i = 0; i < 100; i++) {

		}
	}
	return 1;
}

int bar() {
	return foo();
}
