//Here we go with a messy scope conflict...

static int foo();

int main() {

	int i_1 = 100;
	int a[N];
	for (int i_0 = 0; i_0 < i_1; i_0++) { /*<<<<< 9,5,12,6,2,pass*/
		a[i_0] = 0;
		foo();
	}

}

int foo() {
	return 0;
}
