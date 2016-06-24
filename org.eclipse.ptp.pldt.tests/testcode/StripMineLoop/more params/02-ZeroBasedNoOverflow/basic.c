
static int foo();

int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 8,1,11,3,2,true,true,j,k,pass*/
		a[i] = 0;
	}

}

int foo() {
	return 0;
}
