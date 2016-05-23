
static int foo();

int main() {

	int i_0 = 100;
	int N = 100;
	int a[N];
	for (int i = 0; i < i_0; i++) { /*<<<<< 9,1,13,1,2,pass*/
		a[i] = 0;
	}

}

int foo() {
	return 0;
}
