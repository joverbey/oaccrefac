int main() {
    for (int i = 0; i < 100; i++){ /*<<<<< 2,1,4,6,pass*/
        int a = foo();
    }
}

int foo() {
	return bar(1);
}

int bar(int n) {
	if (n < 10)
		return bar(n+1);
	else
		return 1;
}
