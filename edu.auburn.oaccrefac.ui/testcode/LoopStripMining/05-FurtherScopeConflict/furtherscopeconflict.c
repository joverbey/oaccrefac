//Here we go with a messy scope conflict...

static int foo();

int main() {

	int i_0 = 50;
	int i_1 = 100;
	int N = i_1;
	int a[N];
	for (int i = 0; i < i_0; i++) { /*<<<<< 11,1,15,1,2,pass*/
		a[i] = 0;
		foo();
	}

}

int foo() {
	return 0;
}
