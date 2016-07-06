
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < a[i]; i++) { /*<<<<< 8,1,11,3,2,fail*/
		a[i] = 0;
	}

}

int foo() {
	return 0;
}
