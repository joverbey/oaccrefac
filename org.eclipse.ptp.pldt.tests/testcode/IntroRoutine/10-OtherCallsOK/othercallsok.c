int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,pass*/
        int a = foo(3);
    }
}

int foo(int n) {
	return bar();
}

int bar() {
#pragma acc parallel loop worker
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}

int nonFunction() {
	int a = foo(6);
	return a;
}
