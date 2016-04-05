static int foo();

int main() {

	int N = 100;
	int a[N];
	/* autotune */
	for (int i = 0; i < N; i++) {
		a[i] = 0;
	}

}

int foo() {
	return 0;
}
