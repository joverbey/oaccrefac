
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < 100; i++) { /*<<<<< 8,1,12,1,2,pass*/
		a[i] = 0;
	}

}

int foo() {
	return 0;
}
