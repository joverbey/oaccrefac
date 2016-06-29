int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,fail*/
        int a = foo();
    }
}

int foo() {
	return bar();
}

int bar() {
	static int n;
	for (int i = 0; i < 100; i++) {

	}
	return 1;
}
