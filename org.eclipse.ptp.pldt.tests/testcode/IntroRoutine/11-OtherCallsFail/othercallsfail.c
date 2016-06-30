int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,fail*/
        int a = foo(3);
    }
}

int foo(int n) {
	return bar();
}

#pragma acc routine worker
int bar() {
#pragma acc parallel loop worker
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}

#pragma acc routine gang
int nonFunction() {
	int a = foo(6);
	return a;
}
