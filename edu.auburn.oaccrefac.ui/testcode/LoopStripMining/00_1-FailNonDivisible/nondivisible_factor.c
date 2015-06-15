
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < 100; i+=2) { /*<<<<< 8,1,11,6,7,fail*/
		a[i] = 0;
		foo();
	}

}

int foo() {
	return 0;
}
