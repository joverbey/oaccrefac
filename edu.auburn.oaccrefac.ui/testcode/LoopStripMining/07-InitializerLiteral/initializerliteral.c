
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 1; i < N; i+=3) { /*<<<<< 8,1,12,1,6,pass*/
		a[i] = 0;
		foo();
	}

}

int foo() {
	return 0;
}
