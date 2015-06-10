
static int foo();

int main() {

	int i = 0;
	int i_0 = 100;
	int a[N];
	for (int i = 0; i < i_0; i++) { /*<<<<< 9,5,12,6,2,pass*/
		a[i] = 0;
		foo();
	}

}

int foo() {
	return 0;
}
