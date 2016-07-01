int n = 7;

int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 4,1,6,6,pass*/
        int a = foo();
    }
}

int foo() {
	return bar();
}

int bar() {
	for (int i = 0; i < n; i++) {

	}
	return 1;
}
