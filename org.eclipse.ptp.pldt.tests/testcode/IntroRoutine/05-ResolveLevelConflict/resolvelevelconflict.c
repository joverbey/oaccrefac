int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,pass*/
        int a = foo();
    }
}

int foo() {
	return bar() + bar2();
}

int bar() {
#pragma acc loop vector
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}

int bar2() {
#pragma acc loop worker
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}
