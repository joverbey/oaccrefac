int main() {

	int N = 100;
	int a[N];
	for (int i = 0; i < N; i++) {
		for (int j = 0; j < N; j++) { /*<<<<< 5,1,10,1,1,fail*/
			a[i] = 0;
			i++;
		}
	}
}
