
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 8,1,11,6,2,pass*/
		a[i] = 0;
		foo();
	}

}

int foo() {
	return 0;
}