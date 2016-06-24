int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) { /*<<<<< 5,1,15,3,2,pass*/
		switch (i) {
		case(1):
				a[i] = 77;
		case(2):
				a[i] = 55;
		default:
				a[i] = 0;
		}
	}

}
