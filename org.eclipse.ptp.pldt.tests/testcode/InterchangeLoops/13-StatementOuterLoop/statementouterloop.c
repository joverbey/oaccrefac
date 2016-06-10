int main() {

	int N = 100;
	int a[N];
	for (long i = 0; i < N; i++) { /*<<<<< 5,1,11,3,1,fail*/
		for (int j = 0; j < N; j++) {
			a[i] = 0; //comment
			break;
		}
		a[i] = 7;
	}
}
